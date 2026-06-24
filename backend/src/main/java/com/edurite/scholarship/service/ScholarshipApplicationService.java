package com.edurite.scholarship.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.scholarship.dto.ScholarshipApplicationDtos.MotivationLetterResponse;
import com.edurite.scholarship.dto.ScholarshipApplicationDtos.ScholarshipApplicationRequest;
import com.edurite.scholarship.dto.ScholarshipApplicationDtos.ScholarshipApplicationResponse;
import com.edurite.scholarship.entity.ScholarshipApplication;
import com.edurite.scholarship.repository.ScholarshipApplicationRepository;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentContextService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScholarshipApplicationService {

    private static final Set<String> STATUSES = Set.of("NOT_STARTED", "IN_PROGRESS", "SUBMITTED", "APPROVED", "REJECTED");

    private final StudentContextService studentContextService;
    private final ScholarshipApplicationRepository repository;
    private final AiProviderOrchestratorService aiProviderOrchestratorService;

    public ScholarshipApplicationService(
            StudentContextService studentContextService,
            ScholarshipApplicationRepository repository,
            AiProviderOrchestratorService aiProviderOrchestratorService
    ) {
        this.studentContextService = studentContextService;
        this.repository = repository;
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
    }

    @Transactional(readOnly = true)
    public List<ScholarshipApplicationResponse> list(Principal principal) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        return repository.findByStudentIdOrderByApplicationDeadlineAscCreatedAtDesc(profile.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScholarshipApplicationResponse> upcoming(Principal principal) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        LocalDate today = LocalDate.now();
        return repository.findByStudentIdAndApplicationDeadlineBetweenOrderByApplicationDeadlineAsc(profile.getId(), today, today.plusDays(45)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ScholarshipApplicationResponse create(Principal principal, ScholarshipApplicationRequest request) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        ScholarshipApplication application = new ScholarshipApplication();
        application.setStudentId(profile.getId());
        apply(application, request);
        return toResponse(repository.save(application));
    }

    @Transactional
    public ScholarshipApplicationResponse update(Principal principal, UUID id, ScholarshipApplicationRequest request) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        ScholarshipApplication application = repository.findByIdAndStudentId(id, profile.getId())
                .orElseThrow(() -> new ResourceConflictException("Scholarship application not found"));
        apply(application, request);
        return toResponse(repository.save(application));
    }

    @Transactional
    public void delete(Principal principal, UUID id) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        ScholarshipApplication application = repository.findByIdAndStudentId(id, profile.getId())
                .orElseThrow(() -> new ResourceConflictException("Scholarship application not found"));
        repository.delete(application);
    }

    @Transactional
    public MotivationLetterResponse motivationLetter(Principal principal, UUID id) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        ScholarshipApplication application = repository.findByIdAndStudentId(id, profile.getId())
                .orElseThrow(() -> new ResourceConflictException("Scholarship application not found"));
        String fallback = """
                Dear Selection Committee,

                I am applying for %s because it aligns with my academic goals and future career plans. My current studies, interests, and commitment to growth have prepared me to make good use of this opportunity. Support from this scholarship would help me focus on my studies, continue building relevant skills, and contribute positively to my community.

                Thank you for considering my application.
                """.formatted(application.getScholarshipTitle());
        String prompt = """
                Draft a concise, sincere motivation letter for a South African student scholarship application.
                Student qualification: %s
                Interests: %s
                Scholarship: %s
                Provider: %s
                Required documents: %s
                Keep it under 220 words.
                """.formatted(
                valueOr(profile.getQualificationLevel(), "not provided"),
                valueOr(profile.getInterests(), "not provided"),
                application.getScholarshipTitle(),
                valueOr(application.getProvider(), "not provided"),
                valueOr(application.getRequiredDocuments(), "not provided")
        );
        String draft;
        try {
            draft = aiProviderOrchestratorService.generateContent(prompt);
        } catch (RuntimeException ex) {
            draft = fallback;
        }
        application.setMotivationLetterDraft(draft.trim());
        repository.save(application);
        return new MotivationLetterResponse(application.getMotivationLetterDraft());
    }

    private void apply(ScholarshipApplication application, ScholarshipApplicationRequest request) {
        application.setBursaryId(request.bursaryId());
        application.setScholarshipTitle(clean(request.scholarshipTitle()));
        application.setProvider(clean(request.provider()));
        application.setApplicationDeadline(request.applicationDeadline());
        application.setStatus(normalizeStatus(request.status()));
        application.setChecklist(clean(request.checklist()));
        application.setRequiredDocuments(clean(request.requiredDocuments()));
        application.setReminderNotes(clean(request.reminderNotes()));
        application.setMotivationLetterDraft(clean(request.motivationLetterDraft()));
        application.setSaved(request.saved() == null || request.saved());
        application.setNotes(clean(request.notes()));
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "NOT_STARTED";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new ResourceConflictException("Unsupported scholarship status: " + value);
        }
        return normalized;
    }

    private ScholarshipApplicationResponse toResponse(ScholarshipApplication application) {
        LocalDate deadline = application.getApplicationDeadline();
        boolean deadlineSoon = deadline != null
                && !deadline.isBefore(LocalDate.now())
                && !deadline.isAfter(LocalDate.now().plusDays(14));
        return new ScholarshipApplicationResponse(
                application.getId(),
                application.getBursaryId(),
                valueOr(application.getScholarshipTitle(), ""),
                valueOr(application.getProvider(), ""),
                application.getApplicationDeadline(),
                application.getStatus(),
                valueOr(application.getChecklist(), ""),
                valueOr(application.getRequiredDocuments(), ""),
                valueOr(application.getReminderNotes(), ""),
                valueOr(application.getMotivationLetterDraft(), ""),
                application.isSaved(),
                valueOr(application.getNotes(), ""),
                deadlineSoon,
                application.getUpdatedAt()
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

