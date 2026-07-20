package com.edurite.roadmap.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class CareerRoadmapDtos {
    private CareerRoadmapDtos() {
    }

    public record CareerRoadmapResponse(
            UUID id,
            String slug,
            String title,
            String overview,
            String requiredSubjects,
            String recommendedSkills,
            String studyPath,
            String entryLevelJobs,
            String longTermGrowth,
            String learningResources
    ) {}

    public record CareerRoadmapGenerateRequest(
            @NotBlank String careerName,
            String grade,
            String province,
            List<ApsSubjectInput> subjects
    ) {}

    public record ApsCalculationRequest(
            String grade,
            String province,
            List<ApsSubjectInput> subjects
    ) {}

    public record ApsSubjectInput(
            @NotBlank String subjectName,
            Integer markPercentage,
            Integer level,
            Integer apsPoints
    ) {}

    public record ApsSubjectResult(
            String subjectName,
            Integer markPercentage,
            Integer level,
            Integer apsPoints,
            boolean included,
            String exclusionReason
    ) {}

    public record ApsCalculationResponse(
            String source,
            String status,
            String grade,
            String province,
            List<ApsSubjectResult> subjects,
            Integer totalAps,
            List<String> missingRequirements,
            String calculationRule,
            boolean lifeOrientationIncluded,
            String resultSetLabel,
            String resultSetId
    ) {}

    public record CareerRoadmapGenerateResponse(
            String careerName,
            CareerOverview overview,
            List<SubjectRequirement> requiredSubjects,
            List<SubjectRequirement> recommendedSubjects,
            List<PathwayStep> universityPathway,
            List<PathwayStep> professionalPathway,
            List<RoadmapTimelineStage> roadmapTimeline,
            List<UniversityRequirementResponse> universityRequirements,
            ApsReadiness apsReadiness,
            GapAnalysis gapAnalysis,
            List<String> alternativePathways,
            List<StudyPlanStep> studyPlan
    ) {}

    public record CareerOverview(
            String description,
            List<String> dailyResponsibilities,
            List<String> skillsNeeded,
            String careerDemand,
            String salaryRange,
            String professionalBody
    ) {}

    public record SubjectRequirement(
            String subject,
            Integer minimumLevel,
            String minimumPass,
            String suggestedMark,
            boolean required,
            String notes
    ) {}

    public record PathwayStep(
            String title,
            String description
    ) {}

    public record RoadmapTimelineStage(
            Integer stage,
            String title,
            String description
    ) {}

    public record UniversityRequirementResponse(
            UUID id,
            String institutionName,
            String institutionType,
            String province,
            String qualificationName,
            String faculty,
            Integer apsRequired,
            String mathematicsRequirement,
            String mathematicalLiteracyRequirement,
            String englishRequirement,
            String accountingRequirement,
            String physicalSciencesRequirement,
            String lifeSciencesRequirement,
            String duration,
            String nqfLevel,
            String applicationUrl,
            String notes,
            String source,
            boolean verified,
            String verificationBadge,
            String requirementStatus,
            Integer apsGap
    ) {}

    public record ApsReadiness(
            Integer learnerAps,
            Integer requiredAps,
            Integer apsGap,
            Integer readinessScore,
            String status,
            Integer bestFitUniversities
    ) {}

    public record GapAnalysis(
            Integer currentAps,
            Integer requiredAps,
            Integer apsGap,
            List<String> missingSubjects,
            List<String> subjectsNeedingImprovement,
            List<String> bestFitUniversities,
            String riskLevel,
            List<String> improvementSuggestions
    ) {}

    public record StudyPlanStep(
            String title,
            String focus,
            List<String> actions
    ) {}

    public record CareerRoadmapSaveRequest(
            @NotBlank String careerName,
            CareerRoadmapGenerateResponse roadmap,
            Integer learnerAps,
            Integer requiredAps,
            Integer apsGap,
            Integer readinessScore
    ) {}

    public record SavedCareerRoadmapResponse(
            UUID id,
            String careerName,
            CareerRoadmapGenerateResponse roadmap,
            Integer learnerAps,
            Integer requiredAps,
            Integer apsGap,
            Integer readinessScore,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}
}
