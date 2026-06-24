package com.edurite.progress.service;

import com.edurite.cv.repository.StudentCvRepository;
import com.edurite.progress.dto.ProgressScoreDtos.ProgressScoreCard;
import com.edurite.progress.dto.ProgressScoreDtos.ProgressScoreResponse;
import com.edurite.scholarship.repository.ScholarshipApplicationRepository;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.SavedBursaryRepository;
import com.edurite.student.repository.SavedCareerRepository;
import com.edurite.student.service.StudentContextService;
import com.edurite.student.service.StudentProfileCompletionService;
import com.edurite.tutor.repository.TutorSessionRepository;
import com.edurite.universityapplication.repository.UniversityApplicationRepository;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressScoreService {

    private final StudentContextService studentContextService;
    private final StudentProfileCompletionService profileCompletionService;
    private final StudentCvRepository studentCvRepository;
    private final TutorSessionRepository tutorSessionRepository;
    private final ScholarshipApplicationRepository scholarshipApplicationRepository;
    private final UniversityApplicationRepository universityApplicationRepository;
    private final SavedBursaryRepository savedBursaryRepository;
    private final SavedCareerRepository savedCareerRepository;

    public ProgressScoreService(
            StudentContextService studentContextService,
            StudentProfileCompletionService profileCompletionService,
            StudentCvRepository studentCvRepository,
            TutorSessionRepository tutorSessionRepository,
            ScholarshipApplicationRepository scholarshipApplicationRepository,
            UniversityApplicationRepository universityApplicationRepository,
            SavedBursaryRepository savedBursaryRepository,
            SavedCareerRepository savedCareerRepository
    ) {
        this.studentContextService = studentContextService;
        this.profileCompletionService = profileCompletionService;
        this.studentCvRepository = studentCvRepository;
        this.tutorSessionRepository = tutorSessionRepository;
        this.scholarshipApplicationRepository = scholarshipApplicationRepository;
        this.universityApplicationRepository = universityApplicationRepository;
        this.savedBursaryRepository = savedBursaryRepository;
        this.savedCareerRepository = savedCareerRepository;
    }

    @Transactional(readOnly = true)
    public ProgressScoreResponse score(Principal principal) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        int profileScore = clamp(profileCompletionService.calculateCompleteness(profile));
        int cvScore = studentCvRepository.findByStudentId(profile.getId())
                .map(cv -> {
                    int score = 0;
                    if (notBlank(cv.getPersonalSummary())) score += 20;
                    if (notBlank(cv.getEducation())) score += 15;
                    if (notBlank(cv.getSkills())) score += 20;
                    if (notBlank(cv.getExperience())) score += 15;
                    if (notBlank(cv.getProjects())) score += 15;
                    if (notBlank(cv.getCareerObjective())) score += 15;
                    return clamp(score);
                })
                .orElse(0);
        int learningScore = tutorSessionRepository.countByStudentId(profile.getId()) > 0 ? 70 : 0;
        long scholarshipCount = scholarshipApplicationRepository.countByStudentId(profile.getId());
        long universityCount = universityApplicationRepository.countByStudentId(profile.getId());
        long savedBursaryCount = savedBursaryRepository.countByStudentId(profile.getId());
        long activeApplicationCount = scholarshipApplicationRepository.countByStudentIdAndStatusIn(profile.getId(), List.of("IN_PROGRESS", "SUBMITTED", "APPROVED"))
                + universityApplicationRepository.countByStudentIdAndApplicationStatusIn(profile.getId(), List.of("READY", "SUBMITTED", "ACCEPTED", "WAITLISTED"));
        int applicationScore = clamp((int) Math.min(100, savedBursaryCount * 20 + scholarshipCount * 25 + universityCount * 25 + activeApplicationCount * 30));
        long savedCareerCount = savedCareerRepository.countByStudentId(profile.getId());
        int careerScore = clamp((int) Math.min(100, savedCareerCount * 60));

        List<ProgressScoreCard> cards = List.of(
                card("profile", "Profile completion", profileScore, "Complete your profile"),
                card("cv", "CV readiness", cvScore, "Add CV skills and a career objective"),
                card("learning", "Learning activity", learningScore, "Start an AI tutor session"),
                card("applications", "Bursary/application progress", applicationScore, "Save a bursary or track an application"),
                card("career", "Career exploration", careerScore, "Save a career recommendation")
        );
        int overall = (int) Math.round(cards.stream().mapToInt(ProgressScoreCard::percentage).average().orElse(0));
        List<String> recommendations = new ArrayList<>();
        cards.stream()
                .filter(card -> card.percentage() < 70)
                .map(ProgressScoreCard::recommendation)
                .forEach(recommendations::add);
        if (recommendations.isEmpty()) {
            recommendations.add("Keep your applications, CV, and learning activity current.");
        }
        return new ProgressScoreResponse(overall, color(overall), cards, recommendations);
    }

    private ProgressScoreCard card(String key, String label, int percentage, String recommendation) {
        return new ProgressScoreCard(key, label, percentage, color(percentage), recommendation);
    }

    private String color(int percentage) {
        if (percentage >= 70) return "green";
        if (percentage >= 35) return "orange";
        return "red";
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

