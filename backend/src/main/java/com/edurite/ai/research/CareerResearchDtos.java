package com.edurite.ai.research;

import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.student.dto.StudentSubjectAchievementDto;

import java.util.*;

public final class CareerResearchDtos {
    private CareerResearchDtos() {}

    public record AcademicAnalysis(
            String grade,
            Double percentageAverage,
            List<StudentSubjectAchievementDto> subjects,
            List<StudentSubjectAchievementDto> relativeStrengths,
            List<StudentSubjectAchievementDto> improvementPriorities,
            List<String> curricula,
            List<String> interests,
            List<String> skills,
            String goals,
            List<String> limitations,
            List<AcademicProgression> progression) {
        public AcademicAnalysis(
                String grade,
                Double average,
                List<StudentSubjectAchievementDto> subjects,
                List<StudentSubjectAchievementDto> strengths,
                List<StudentSubjectAchievementDto> priorities,
                List<String> curricula,
                List<String> interests,
                List<String> skills,
                String goals,
                List<String> limitations) {
            this(
                    grade,
                    average,
                    subjects,
                    strengths,
                    priorities,
                    curricula,
                    interests,
                    skills,
                    goals,
                    limitations,
                    List.of());
        }
    }

    public record AcademicProgression(
            String subject,
            String curriculum,
            String previousYear,
            String currentYear,
            int previousMark,
            int currentMark,
            int percentagePointChange) {}

    public record SubjectCheck(
            String subject,
            String studentResult,
            String requirement,
            String status,
            boolean reviewed) {}

    public record Eligibility(
            String status,
            Integer studentAps,
            Integer requiredAps,
            String apsRule,
            List<SubjectCheck> subjects,
            List<String> gaps,
            List<String> uncheckedConditions) {}

    public record ProgrammeFinding(
            String id,
            String institution,
            String country,
            String name,
            String qualification,
            String duration,
            String campus,
            String studyMode,
            String programmeUrl,
            String applicationUrl,
            String requirements,
            String sourceUrl,
            String verifiedAt,
            String verificationStatus,
            Eligibility eligibility,
            List<String> careerIds) {}

    public record CareerFinding(
            String id,
            String title,
            String category,
            String description,
            int rankingWeight,
            String suitability,
            String explanation,
            List<String> relevantSubjects,
            List<String> strengths,
            List<String> improvementAreas,
            List<String> qualifications,
            List<String> programmeIds) {}

    public record Source(String url, String institution, String verifiedAt, String status) {}

    public record Narrative(
            String assessment,
            String researchSummary,
            List<String> nextSteps,
            List<String> sourceUrls) {}

    public record Report(
            String schemaVersion,
            String generatedAt,
            AcademicAnalysis academic,
            List<CareerFinding> careers,
            List<ProgrammeFinding> programmes,
            List<ProgrammeFinding> alternativePathways,
            List<BursaryResultDto> funding,
            Narrative interpretation,
            List<Source> sources,
            List<String> inputsUsed,
            List<String> limitations,
            String rankingMethod) {}

    public record JobStatus(
            String jobId,
            String status,
            String stage,
            String updatedAt,
            String error,
            Report report) {}
}
