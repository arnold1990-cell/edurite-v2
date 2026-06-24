package com.edurite.recommendation.service;

import com.edurite.recommendation.dto.RecommendationItemDto;
import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.service.StudentPlanAccessService;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * This class named RecommendationService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final StudentService studentService;
    private final StudentPlanAccessService studentPlanAccessService;
    private final PsychometricService psychometricService;

    public RecommendationService(
            StudentService studentService,
            StudentPlanAccessService studentPlanAccessService,
            PsychometricService psychometricService
    ) {
        this.studentService = studentService;
        this.studentPlanAccessService = studentPlanAccessService;
        this.psychometricService = psychometricService;
    }

    /**
     * this method handles the "generateForStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public RecommendationResultDto generateForStudent(Principal principal) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        StudentPlanAccessService.StudentPlanAccess planAccess = studentPlanAccessService.resolveByUserId(profile.getUserId());
        boolean premium = planAccess.premium();

        List<String> skills = split(profile.getSkills());
        List<String> interests = split(profile.getInterests());
        String qualification = normalize(profile.getQualificationLevel());
        String experience = normalize(profile.getExperience());
        String goals = normalize(profile.getCareerGoals());

        List<RecommendationItemDto> suggestedCareers = new ArrayList<>();
        List<RecommendationItemDto> suggestedBursaries = new ArrayList<>();
        List<RecommendationItemDto> improvements = new ArrayList<>();
        List<String> profileTips = new ArrayList<>();

        if (containsAny(interests, "technology", "software", "computers")
                || containsAny(skills, "java", "programming", "coding")) {
            suggestedCareers.add(new RecommendationItemDto("career-software-engineer", "Software Engineer", 91,
                    "Your interest in technology and software skills aligns well with engineering pathways."));
            suggestedCareers.add(new RecommendationItemDto("career-data-engineer", "Data Engineer", 88,
                    "Your technical strengths suggest strong potential in data systems and analytics roles."));
            suggestedBursaries.add(new RecommendationItemDto("bursary-stem-excellence", "STEM Excellence Bursary", 87,
                    "This bursary supports high-potential STEM students and matches your profile signals."));
        }

        if (containsAny(interests, "business", "finance") || containsAny(List.of(goals), "entrepreneur", "management")) {
            suggestedCareers.add(new RecommendationItemDto("career-business-analyst", "Business Analyst", 84,
                    "Your profile suggests strong potential for data-informed business roles."));
            suggestedCareers.add(new RecommendationItemDto("career-product-analyst", "Product Analyst", 80,
                    "You show signals suited to data-driven product and strategy pathways."));
        }

        if (containsAny(interests, "health", "medical", "science")) {
            suggestedCareers.add(new RecommendationItemDto("career-health-data-analyst", "Health Data Analyst", 80,
                    "You show science-focused interests that map to modern healthcare analytics careers."));
        }

        if (containsAny(interests, "design", "creative", "ux")) {
            suggestedCareers.add(new RecommendationItemDto("career-ux-designer", "UX Designer", 77,
                    "Your creative interests align with user-centred digital product roles."));
        }

        if (containsAny(skills, "leadership", "collaboration", "communication")) {
            suggestedCareers.add(new RecommendationItemDto("career-project-coordinator", "Project Coordinator", 76,
                    "Your collaboration and communication strengths suit project delivery roles."));
        }

        if (qualification.isBlank()) {
            profileTips.add("Add your qualification level so eligibility matching can improve.");
        }
        if (skills.isEmpty()) {
            profileTips.add("List at least 3 key skills to improve career and course matching.");
        }
        if (interests.isEmpty()) {
            profileTips.add("Add your interests so recommendations can be tailored.");
        }
        if (profile.getCvFileUrl() == null) {
            profileTips.add("Upload your CV to unlock stronger matching confidence.");
        }

        improvements.add(new RecommendationItemDto("improvement-communication", "Strengthen communication portfolio", 76,
                "Add project presentations or teamwork examples to improve employability."));

        if (premium) {
            improvements.add(new RecommendationItemDto("course-data-structures", "Advanced Data Structures", 85,
                    "Recommended to close analytical problem-solving skill gaps."));
        } else {
            improvements.add(new RecommendationItemDto("upgrade-premium", "Upgrade to Premium for deeper guidance", 70,
                    "Premium unlocks additional course-level recommendations and insights."));
        }

        if (suggestedCareers.isEmpty()) {
            suggestedCareers.add(new RecommendationItemDto("career-generalist", "Digital Operations Specialist", 72,
                    "A versatile path while your profile becomes more complete."));
            suggestedCareers.add(new RecommendationItemDto("career-support-analyst", "Technology Support Analyst", 70,
                    "A practical starter role while you continue building specialist strengths."));
        }
        if (suggestedBursaries.isEmpty()) {
            suggestedBursaries.add(new RecommendationItemDto("bursary-career-growth", "Career Growth Support Bursary", 74,
                    "A broad bursary option suited to developing profiles."));
        }

        if (!profile.isProfileCompleted()) {
            profileTips.addFirst("Complete your student profile and upload required documents for better recommendations.");
        }
        if (experience.isBlank()) {
            profileTips.add("Add volunteer work or projects under experience.");
        }
        List<String> growthAreas;
        try {
            growthAreas = psychometricService.findGrowthAreasByStudentProfileId(profile.getId());
        } catch (RuntimeException ex) {
            log.warn("Unable to load psychometric growth areas for recommendations: studentProfileId={}", profile.getId(), ex);
            growthAreas = List.of();
        }
        if (!growthAreas.isEmpty()) {
            profileTips.add("Psychometric growth areas: " + String.join(", ", growthAreas) + ". Use the Learning Centre recommendations to improve.");
        }

        List<RecommendationItemDto> deduplicatedCareers = deduplicateById(suggestedCareers);
        Integer careerLimit = planAccess.careerSuggestionLimit();
        List<RecommendationItemDto> visibleCareers = careerLimit == null
                ? deduplicatedCareers
                : deduplicatedCareers.stream().limit(careerLimit).toList();
        boolean limited = visibleCareers.size() < deduplicatedCareers.size();
        if (limited && planAccess.upgradeMessage() != null) {
            profileTips.add(planAccess.upgradeMessage());
        }

        return new RecommendationResultDto(
                visibleCareers,
                suggestedBursaries,
                improvements,
                profileTips,
                "rule-engine-v4",
                planAccess.planCode(),
                premium,
                careerLimit,
                limited,
                planAccess.upgradeMessage()
        );
    }

    private List<String> split(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        return Stream.of(input.split(",")).map(this::normalize).filter(s -> !s.isBlank()).toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean containsAny(List<String> values, String... keywords) {
        for (String value : values) {
            for (String keyword : keywords) {
                if (value.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<RecommendationItemDto> deduplicateById(List<RecommendationItemDto> input) {
        LinkedHashMap<String, RecommendationItemDto> byId = new LinkedHashMap<>();
        for (RecommendationItemDto item : input) {
            byId.putIfAbsent(item.id(), item);
        }
        return new ArrayList<>(byId.values());
    }
}

