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
    public ProfileGuidanceContext(ObjectMapper mapper, PsychometricService psychometric) { this.mapper = mapper; this.psychometric = psychometric; }
    public String describe(StudentProfile profile) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("grade", profile.getSelectedGrade());
        context.put("interests", profile.getInterests());
        context.put("skills", profile.getSkills());
        context.put("goals", profile.getCareerGoals());
        List<Map<String, Object>> subjects = new ArrayList<>();
        try {
            for (var row : mapper.readTree(profile.getSubjectAchievementsJson())) {
                Map<String, Object> result = new LinkedHashMap<>();
                for (String key : List.of("subjectName", "achievementLevel", "markPercentage", "source", "verified")) {
                    if (row.hasNonNull(key)) result.put(key, row.get(key));
                }
                subjects.add(result);
            }
        } catch (Exception ignored) { /* No inferred academic results. */ }
        context.put("subjects", subjects);
        context.put("assessmentStrengths", psychometric.findStrengthAreasByStudentProfileId(profile.getId()));
        context.put("developmentAreas", psychometric.findGrowthAreasByStudentProfileId(profile.getId()));
        try { return mapper.writeValueAsString(context); }
        catch (Exception ex) { throw new IllegalStateException("Unable to prepare profile context", ex); }
    }
}
