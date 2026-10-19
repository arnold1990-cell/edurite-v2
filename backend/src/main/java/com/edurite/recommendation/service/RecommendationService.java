package com.edurite.recommendation.service;

import com.edurite.recommendation.dto.*;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.career.repository.CareerRepository;
import com.edurite.course.repository.CourseRepository;
import java.security.Principal;
import java.util.*;
import java.util.stream.*;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
    private final StudentService students;
    private final StudentPlanAccessService access;
    private final PsychometricService psychometric;
    private final CareerRepository careers;
    private final CourseRepository courses;
    private final com.edurite.student.repository.StudentPreferenceRepository preferences;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;
    public RecommendationService(StudentService students, StudentPlanAccessService access, PsychometricService psychometric,
            CareerRepository careers, CourseRepository courses, com.edurite.student.repository.StudentPreferenceRepository preferences,
            com.fasterxml.jackson.databind.ObjectMapper mapper) {
        this.students = students; this.access = access; this.psychometric = psychometric; this.careers = careers; this.courses = courses;
        this.preferences = preferences; this.mapper = mapper;
    }
    public RecommendationResultDto generateForStudent(Principal principal) {
        var profile = students.getProfile(principal);
        var entity = students.getProfileEntity(principal);
        var plan = access.resolveByUserId(entity.getUserId());
        List<String> inputs = new ArrayList<>(), missing = new ArrayList<>(), limitations = new ArrayList<>();
        limitations.add("Career exploration only: not a diagnosis, admission decision or guaranteed career outcome. Catalogue ranking uses text correlations, not matching percentages.");
        List<String> assessmentStrengths = List.of();
        try {
            if (psychometric.hasSubmissionForStudentProfileId(entity.getId())) {
                assessmentStrengths = psychometric.findStrengthAreasByStudentProfileId(entity.getId());
                inputs.add("Saved psychometric responses" + (assessmentStrengths.isEmpty() ? " (no high-rated dimensions)" : ": " + String.join(", ", assessmentStrengths)));
            } else missing.add("Psychometric assessment (optional)");
        } catch (org.springframework.dao.DataAccessException | com.edurite.common.exception.ResourceConflictException ex) {
            missing.add("Psychometric results are temporarily unavailable; recommendations use the other saved inputs.");
        }
        final List<String> strengths = assessmentStrengths;
        List<String> choices = new ArrayList<>();
        if (!safe(profile.careerGoals()).isBlank()) choices.add(profile.careerGoals());
        preferences.findByStudentId(entity.getId()).ifPresent(p -> {
            if (!safe(p.getPreferredIndustries()).isBlank()) choices.addAll(Arrays.asList(p.getPreferredIndustries().split(",")));
            if (!safe(p.getPreferredLocations()).isBlank()) limitations.add("Preferred locations: " + p.getPreferredLocations() + ". Career catalogue matching does not verify local availability.");
        });
        try {
            var saved = mapper.readTree(entity.getPreferencesJson());
            for (var choice : saved.path("profileAssociations")) if (!choice.path("title").asText().isBlank()) choices.add(choice.path("title").asText());
        } catch (Exception ex) { limitations.add("Saved course and career links could not be read; update or retry your profile."); }
        if (choices.isEmpty()) missing.add("Career goals or saved preferences"); else inputs.add("Career goals and preferences: " + String.join(", ", choices));
        if (profile.interests().isEmpty()) missing.add("Interests"); else inputs.add("Interests: " + String.join(", ", profile.interests()));
        if (profile.skills().isEmpty()) missing.add("Skills"); else inputs.add("Skills: " + String.join(", ", profile.skills()));
        var academic = profile.subjectAchievements().stream().filter(s -> s.markPercentage() != null || s.achievementLevel() != null).toList();
        if (academic.isEmpty()) missing.add("Academic subjects and marks");
        else inputs.add("Saved academic results: " + academic.stream().map(s -> s.subjectName() + (s.markPercentage() != null ? " " + s.markPercentage() + "%" : " level " + s.achievementLevel())).collect(Collectors.joining(", ")));
        var ranked = careers.findAll().stream().map(career -> {
            String text = String.join(" ", safe(career.getTitle()), safe(career.getDescription()), safe(career.getIndustry())).toLowerCase(Locale.ROOT);
            List<String> interestMatches = matches(profile.interests(), text);
            List<String> skillMatches = matches(profile.skills(), text);
            var matchingAcademic = academic.stream()
                .filter(s -> s.subjectName() != null && !matches(List.of(s.subjectName()), text).isEmpty())
                .toList();
            List<String> subjectMatches = matchingAcademic.stream()
                .map(s -> s.subjectName() + (s.markPercentage() == null ? " level " + s.achievementLevel() : " " + s.markPercentage() + "%")).toList();
            int academicWeight = matchingAcademic.stream().mapToInt(s -> Math.max(1, Math.min(5,
                    s.markPercentage() != null ? s.markPercentage() / 20 : s.achievementLevel()))).sum();
            List<String> traitMatches = matches(strengths, text);
            List<String> preferenceMatches = matches(choices, text);
            int score = Math.min(100, Math.min(60, interestMatches.size()*50) + Math.min(20, skillMatches.size()*10)
                + Math.min(15, academicWeight) + Math.min(5, traitMatches.size()*5) + preferenceMatches.size()*10);
            List<String> reasons = new ArrayList<>();
            if (!interestMatches.isEmpty()) reasons.add("interests in " + String.join(", ", interestMatches));
            if (!skillMatches.isEmpty()) reasons.add("skills in " + String.join(", ", skillMatches));
            if (!subjectMatches.isEmpty()) reasons.add("related recorded subjects: " + String.join(", ", subjectMatches) + " (marks are shown for context, not an eligibility check)");
            if (!traitMatches.isEmpty()) reasons.add("self-reported assessment dimensions: " + String.join(", ", traitMatches));
            if (!preferenceMatches.isEmpty()) reasons.add("career goals or preferences: " + String.join(", ", preferenceMatches));
            String rationale = reasons.isEmpty() ? "Explore this alternative; no strong profile correlation yet." : "Recommended because of your " + String.join("; ", reasons) + ".";
            if (plan.premium()) rationale += " Profile correlation is guidance, not an admission or employment guarantee.";
            return new RecommendationItemDto(career.getId().toString(), career.getTitle(), score, rationale);
        }).filter(item -> item.score() > 0).sorted(Comparator.comparingInt(RecommendationItemDto::score).reversed().thenComparing(RecommendationItemDto::title)).toList();
        int limit = plan.careerSuggestionLimit() == null ? Integer.MAX_VALUE : plan.careerSuggestionLimit();
        var visible = ranked.stream().limit(limit).toList();
        List<String> topTitles = visible.stream().map(RecommendationItemDto::title).toList();
        var study = courses.findAll().stream().map(course -> {
            var related = matches(topTitles, safe(course.getName()).toLowerCase(Locale.ROOT));
            return new RecommendationItemDto("course-" + course.getId(), course.getName(), related.size()*25,
                "Study pathway related to your career matches: " + String.join(", ", related) + ". Check programme entry requirements.");
        }).filter(c -> c.score() > 0).sorted(Comparator.comparingInt(RecommendationItemDto::score).reversed()).limit(limit).toList();
        List<String> tips = new ArrayList<>();
        if (profile.interests().isEmpty()) tips.add("Add interests to improve your top matches.");
        if (profile.skills().isEmpty()) tips.add("Add your skills to improve matching.");
        if (profile.subjectAchievements().isEmpty()) tips.add("Add subjects and results to check academic fit.");
        if (profile.transcriptFileUrl() == null) tips.add("Upload a transcript to complete your academic documents.");
        return new RecommendationResultDto(visible, List.of(), study, tips, "profile-catalogue-v6", plan.planCode(), plan.premium(), plan.careerSuggestionLimit(), ranked.size() > visible.size(), plan.upgradeMessage(), inputs, missing, limitations);
    }
    static List<String> matches(List<String> values, String text) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).filter(value -> tokens(value).stream().anyMatch(text::contains)).toList();
    }
    private static List<String> tokens(String text) {
        return Arrays.stream(safe(text).toLowerCase(Locale.ROOT).split("[^a-z]+"))
            .filter(t -> t.length() > 3 && !Set.of("studies", "level", "language", "career", "science", "general").contains(t)).toList();
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
