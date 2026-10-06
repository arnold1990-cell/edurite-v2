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
    public RecommendationService(StudentService students, StudentPlanAccessService access, PsychometricService psychometric,
            CareerRepository careers, CourseRepository courses) {
        this.students = students; this.access = access; this.psychometric = psychometric; this.careers = careers; this.courses = courses;
    }
    public RecommendationResultDto generateForStudent(Principal principal) {
        var profile = students.getProfile(principal);
        var entity = students.getProfileEntity(principal);
        var plan = access.resolveByUserId(entity.getUserId());
        List<String> strengths = psychometric.findStrengthAreasByStudentProfileId(entity.getId());
        var ranked = careers.findAll().stream().map(career -> {
            String text = String.join(" ", safe(career.getTitle()), safe(career.getDescription()), safe(career.getIndustry())).toLowerCase(Locale.ROOT);
            List<String> interestMatches = matches(profile.interests(), text);
            List<String> skillMatches = matches(profile.skills(), text);
            List<String> subjectMatches = profile.subjectAchievements().stream()
                .filter(s -> s.achievementLevel() != null && s.achievementLevel() >= 4 && !matches(List.of(s.subjectName()), text).isEmpty())
                .map(s -> s.subjectName() + (s.markPercentage() == null ? " level " + s.achievementLevel() : " " + s.markPercentage() + "%")).toList();
            List<String> traitMatches = matches(strengths, text);
            int score = Math.min(100, Math.min(60, interestMatches.size()*50) + Math.min(20, skillMatches.size()*10)
                + Math.min(15, subjectMatches.size()*5) + Math.min(5, traitMatches.size()*5));
            List<String> reasons = new ArrayList<>();
            if (!interestMatches.isEmpty()) reasons.add("interests in " + String.join(", ", interestMatches));
            if (!skillMatches.isEmpty()) reasons.add("skills in " + String.join(", ", skillMatches));
            if (!subjectMatches.isEmpty()) reasons.add("academic strengths in " + String.join(", ", subjectMatches));
            if (!traitMatches.isEmpty()) reasons.add("assessment strengths in " + String.join(", ", traitMatches));
            String rationale = reasons.isEmpty() ? "Explore this alternative; no strong profile correlation yet." : "Recommended because of your " + String.join("; ", reasons) + ".";
            if (plan.premium()) rationale += " Profile correlation is guidance, not an admission or employment guarantee.";
            return new RecommendationItemDto(career.getId().toString(), career.getTitle(), score, rationale);
        }).sorted(Comparator.comparingInt(RecommendationItemDto::score).reversed().thenComparing(RecommendationItemDto::title)).toList();
        int limit = plan.careerSuggestionLimit() == null ? 12 : plan.careerSuggestionLimit();
        var visible = ranked.stream().limit(limit).toList();
        List<String> topTitles = ranked.stream().filter(c -> c.score() > 0).limit(3).map(RecommendationItemDto::title).toList();
        var study = courses.findAll().stream().map(course -> {
            var related = matches(topTitles, safe(course.getName()).toLowerCase(Locale.ROOT));
            return new RecommendationItemDto("course-" + course.getId(), course.getName(), related.size()*25,
                "Study pathway related to your career matches: " + String.join(", ", related) + ". Check programme entry requirements.");
        }).filter(c -> c.score() > 0).sorted(Comparator.comparingInt(RecommendationItemDto::score).reversed()).limit(6).toList();
        List<String> tips = new ArrayList<>();
        if (profile.interests().isEmpty()) tips.add("Add interests to improve your top matches.");
        if (profile.skills().isEmpty()) tips.add("Add your skills to improve matching.");
        if (profile.subjectAchievements().isEmpty()) tips.add("Add subjects and results to check academic fit.");
        if (profile.transcriptFileUrl() == null) tips.add("Upload a transcript to complete your academic documents.");
        return new RecommendationResultDto(visible, List.of(), study, tips, "profile-catalogue-v5", plan.planCode(), plan.premium(), plan.careerSuggestionLimit(), ranked.size() > visible.size(), plan.upgradeMessage());
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
