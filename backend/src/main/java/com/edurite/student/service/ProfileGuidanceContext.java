package com.edurite.student.service;

import com.edurite.student.entity.StudentProfile;
import com.edurite.psychometric.service.PsychometricService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;

/** Shared, minimal profile evidence: never names, contact details or document locations. */
@Service
public class ProfileGuidanceContext {
    private final ObjectMapper mapper;
    private final PsychometricService psychometric;
    private final com.edurite.student.repository.StudentPreferenceRepository preferences;
    public ProfileGuidanceContext(ObjectMapper mapper, PsychometricService psychometric, com.edurite.student.repository.StudentPreferenceRepository preferences) { this.mapper = mapper; this.psychometric = psychometric; this.preferences = preferences; }
    public String describe(StudentProfile profile) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("grade", profile.getSelectedGrade());
        context.put("interests", profile.getInterests());
        context.put("skills", profile.getSkills());
        context.put("goals", profile.getCareerGoals());
        preferences.findByStudentId(profile.getId()).ifPresent(p -> {
            context.put("preferredIndustries", p.getPreferredIndustries());
            context.put("preferredLocations", p.getPreferredLocations());
        });
        List<Map<String, Object>> subjects = new ArrayList<>();
        try {
            for (var row : mapper.readTree(profile.getSubjectAchievementsJson())) {
                Map<String, Object> result = new LinkedHashMap<>();
                for (String key : List.of("subjectName", "achievementLevel", "markPercentage", "source", "verified", "curriculum", "gradingScale", "academicYear", "examination")) {
                    if (row.hasNonNull(key)) result.put(key, row.get(key));
                }
                subjects.add(result);
            }
        } catch (Exception ignored) { /* No inferred academic results. */ }
        context.put("subjects", subjects);
        try {
            var preferences = mapper.readTree(profile.getPreferencesJson());
            List<Map<String, String>> linkedChoices = new ArrayList<>();
            for (var item : preferences.path("profileAssociations")) linkedChoices.add(Map.of("type", item.path("type").asText(), "title", item.path("title").asText()));
            context.put("linkedChoices", linkedChoices);
        } catch (Exception ignored) { /* No inferred course or career associations. */ }
        try {
            context.put("assessmentAvailable", psychometric.hasSubmissionForStudentProfileId(profile.getId()));
            context.put("assessmentStrengths", psychometric.findStrengthAreasByStudentProfileId(profile.getId()));
            context.put("developmentAreas", psychometric.findGrowthAreasByStudentProfileId(profile.getId()));
        } catch (org.springframework.dao.DataAccessException | com.edurite.common.exception.ResourceConflictException ex) {
            context.put("assessmentAvailable", false);
            context.put("assessmentStatus", "Temporarily unavailable; use other saved inputs only.");
        }
        try { return mapper.writeValueAsString(context); }
        catch (Exception ex) { throw new IllegalStateException("Unable to prepare profile context", ex); }
    }
}
