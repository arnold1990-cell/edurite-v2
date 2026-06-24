package com.edurite.cv.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.cv.dto.StudentCvDtos.StudentCvResponse;
import com.edurite.cv.dto.StudentCvDtos.StudentCvSuggestionResponse;
import com.edurite.cv.dto.StudentCvDtos.StudentCvUpsertRequest;
import com.edurite.cv.entity.StudentCv;
import com.edurite.cv.repository.StudentCvRepository;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentContextService;
import java.security.Principal;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentCvService {

    private final StudentContextService studentContextService;
    private final StudentCvRepository studentCvRepository;
    private final AiProviderOrchestratorService aiProviderOrchestratorService;

    public StudentCvService(
            StudentContextService studentContextService,
            StudentCvRepository studentCvRepository,
            AiProviderOrchestratorService aiProviderOrchestratorService
    ) {
        this.studentContextService = studentContextService;
        this.studentCvRepository = studentCvRepository;
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
    }

    @Transactional(readOnly = true)
    public StudentCvResponse get(Principal principal) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        return studentCvRepository.findByStudentId(profile.getId())
                .map(this::toResponse)
                .orElseGet(() -> emptyResponse(profile));
    }

    @Transactional
    public StudentCvResponse upsert(Principal principal, StudentCvUpsertRequest request) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        StudentCv cv = studentCvRepository.findByStudentId(profile.getId()).orElseGet(() -> {
            StudentCv next = new StudentCv();
            next.setStudentId(profile.getId());
            return next;
        });
        cv.setPersonalSummary(clean(request.personalSummary()));
        cv.setEducation(clean(request.education()));
        cv.setSkills(clean(request.skills()));
        cv.setExperience(clean(request.experience()));
        cv.setProjects(clean(request.projects()));
        cv.setCertifications(clean(request.certifications()));
        cv.setReferences(clean(request.references()));
        cv.setCareerObjective(clean(request.careerObjective()));
        return toResponse(studentCvRepository.save(cv));
    }

    @Transactional(readOnly = true)
    public StudentCvSuggestionResponse suggestions(Principal principal) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        StudentCv cv = studentCvRepository.findByStudentId(profile.getId()).orElse(null);
        String fallbackSummary = "Write a concise 3-4 line summary that mentions your target role, strongest skills, education level, and one practical achievement.";
        String fallbackSkills = "Add measurable skills, tools, and examples. Prioritise communication, problem solving, digital literacy, and role-specific technical skills.";
        String fallbackCoverLetter = "Dear Hiring Team,\n\nI am applying because my studies, skills, and career goals align with this opportunity. My background includes "
                + valueOr(profile.getQualificationLevel(), "relevant academic preparation")
                + " and growing experience in " + valueOr(cv == null ? profile.getInterests() : cv.getSkills(), "the required skills")
                + ". I would welcome the opportunity to contribute, learn, and build practical experience.\n\nKind regards";
        String fallbackTips = "Improve readiness by completing your profile, adding project evidence, tailoring your CV for each role, and practising interview answers.";

        String prompt = """
                Create concise CV coaching for an EduRite student.
                Qualification: %s
                Interests: %s
                Career objective: %s
                Skills: %s
                Experience: %s
                Return four short labelled sections: SUMMARY, SKILLS, COVER_LETTER, TIPS.
                """.formatted(
                valueOr(profile.getQualificationLevel(), "not provided"),
                valueOr(profile.getInterests(), "not provided"),
                valueOr(cv == null ? null : cv.getCareerObjective(), "not provided"),
                valueOr(cv == null ? profile.getSkills() : cv.getSkills(), "not provided"),
                valueOr(cv == null ? profile.getExperience() : cv.getExperience(), "not provided")
        );

        try {
            String generated = aiProviderOrchestratorService.generateContent(prompt);
            return new StudentCvSuggestionResponse(
                    section(generated, "SUMMARY", fallbackSummary),
                    section(generated, "SKILLS", fallbackSkills),
                    section(generated, "COVER_LETTER", fallbackCoverLetter),
                    section(generated, "TIPS", fallbackTips)
            );
        } catch (RuntimeException ex) {
            return new StudentCvSuggestionResponse(fallbackSummary, fallbackSkills, fallbackCoverLetter, fallbackTips);
        }
    }

    private StudentCvResponse emptyResponse(StudentProfile profile) {
        return new StudentCvResponse(
                null,
                "",
                valueOr(profile.getQualificationLevel(), ""),
                valueOr(profile.getSkills(), ""),
                valueOr(profile.getExperience(), ""),
                "",
                "",
                "",
                valueOr(profile.getCareerGoals(), ""),
                0,
                null
        );
    }

    private StudentCvResponse toResponse(StudentCv cv) {
        return new StudentCvResponse(
                cv.getId(),
                valueOr(cv.getPersonalSummary(), ""),
                valueOr(cv.getEducation(), ""),
                valueOr(cv.getSkills(), ""),
                valueOr(cv.getExperience(), ""),
                valueOr(cv.getProjects(), ""),
                valueOr(cv.getCertifications(), ""),
                valueOr(cv.getReferences(), ""),
                valueOr(cv.getCareerObjective(), ""),
                readiness(cv),
                cv.getUpdatedAt()
        );
    }

    private int readiness(StudentCv cv) {
        long completed = Stream.of(
                        cv.getPersonalSummary(),
                        cv.getEducation(),
                        cv.getSkills(),
                        cv.getExperience(),
                        cv.getProjects(),
                        cv.getCareerObjective()
                )
                .filter(value -> value != null && !value.isBlank())
                .count();
        return (int) Math.round((completed / 6.0) * 100);
    }

    private String section(String content, String label, String fallback) {
        String marker = label + ":";
        int start = content.toUpperCase().indexOf(marker);
        if (start < 0) {
            return content.isBlank() ? fallback : content.trim();
        }
        int valueStart = start + marker.length();
        int next = content.length();
        for (String candidate : new String[]{"SUMMARY:", "SKILLS:", "COVER_LETTER:", "TIPS:"}) {
            int index = content.toUpperCase().indexOf(candidate, valueStart);
            if (index >= 0) {
                next = Math.min(next, index);
            }
        }
        String value = content.substring(valueStart, next).trim();
        return value.isBlank() ? fallback : value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

