package com.edurite.ai.dto;

import java.util.List;

public record UniversitySourcesAnalysisResponse(
        Boolean aiLive,
        Boolean fallbackUsed,
        String status,
        String mode,
        String groundingStatus,
        Integer evidenceCoverage,
        String warningMessage,
        List<String> requestedSources,
        List<String> sourceUrls,
        List<String> successfullyAnalysedUrls,
        List<String> failedUrls,
        Integer totalSourcesUsed,
        String summary,
        List<String> inferredGuidance,
        List<RecommendedCareer> recommendedCareers,
        List<RecommendedProgramme> recommendedProgrammes,
        List<RecommendedBursary> bursarySuggestions,
        List<String> recommendedUniversities,
        List<String> minimumRequirements,
        List<String> keyRequirements,
        List<String> skillGaps,
        List<String> recommendedNextSteps,
        List<String> warnings,
        Integer suitabilityScore,
        String rawModelUsed,
        String suitabilityScoreReason,
        List<String> suitabilitySignalsUsed,
        List<String> suitabilityScoreLimitations,
        List<SourceDiagnostic> sourceDiagnostics,
        SourceCoverage sourceCoverage,
        String planCode,
        Boolean premiumUnlocked,
        Integer careerSuggestionLimit,
        Boolean careerSuggestionsLimited,
        String upgradeMessage,
        Boolean available,
        String message
) {

    public UniversitySourcesAnalysisResponse(
            Boolean aiLive,
            Boolean fallbackUsed,
            String status,
            String mode,
            String groundingStatus,
            Integer evidenceCoverage,
            String warningMessage,
            List<String> requestedSources,
            List<String> sourceUrls,
            List<String> successfullyAnalysedUrls,
            List<String> failedUrls,
            Integer totalSourcesUsed,
            String summary,
            List<String> inferredGuidance,
            List<RecommendedCareer> recommendedCareers,
            List<RecommendedProgramme> recommendedProgrammes,
            List<RecommendedBursary> bursarySuggestions,
            List<String> recommendedUniversities,
            List<String> minimumRequirements,
            List<String> keyRequirements,
            List<String> skillGaps,
            List<String> recommendedNextSteps,
            List<String> warnings,
            Integer suitabilityScore,
            String rawModelUsed
    ) {
        this(aiLive, fallbackUsed, status, mode, groundingStatus, evidenceCoverage, warningMessage, requestedSources, sourceUrls, successfullyAnalysedUrls, failedUrls, totalSourcesUsed, summary, inferredGuidance, recommendedCareers, recommendedProgrammes, bursarySuggestions, recommendedUniversities, minimumRequirements, keyRequirements, skillGaps, recommendedNextSteps, warnings, suitabilityScore, rawModelUsed, null, List.of(), List.of(), List.of(), null, null, null, null, null, null, null, null);
    }

    public UniversitySourcesAnalysisResponse(
            Boolean aiLive,
            Boolean fallbackUsed,
            String status,
            String mode,
            String groundingStatus,
            Integer evidenceCoverage,
            String warningMessage,
            List<String> requestedSources,
            List<String> sourceUrls,
            List<String> successfullyAnalysedUrls,
            List<String> failedUrls,
            Integer totalSourcesUsed,
            String summary,
            List<String> inferredGuidance,
            List<RecommendedCareer> recommendedCareers,
            List<RecommendedProgramme> recommendedProgrammes,
            List<RecommendedBursary> bursarySuggestions,
            List<String> recommendedUniversities,
            List<String> minimumRequirements,
            List<String> keyRequirements,
            List<String> skillGaps,
            List<String> recommendedNextSteps,
            List<String> warnings,
            Integer suitabilityScore,
            String rawModelUsed,
            String suitabilityScoreReason,
            List<String> suitabilitySignalsUsed,
            List<String> suitabilityScoreLimitations,
            List<SourceDiagnostic> sourceDiagnostics,
            SourceCoverage sourceCoverage
    ) {
        this(aiLive, fallbackUsed, status, mode, groundingStatus, evidenceCoverage, warningMessage, requestedSources, sourceUrls, successfullyAnalysedUrls, failedUrls, totalSourcesUsed, summary, inferredGuidance, recommendedCareers, recommendedProgrammes, bursarySuggestions, recommendedUniversities, minimumRequirements, keyRequirements, skillGaps, recommendedNextSteps, warnings, suitabilityScore, rawModelUsed, suitabilityScoreReason, suitabilitySignalsUsed, suitabilityScoreLimitations, sourceDiagnostics, sourceCoverage, null, null, null, null, null, null, null);
    }

    public record RecommendedCareer(
            String name,
            String reason,
            List<String> requirements,
            List<String> relatedProgrammes,
            String recommendationReason,
            String confidenceLevel,
            List<String> verifiedFacts,
            List<String> inferredInsights,
            List<String> missingData,
            String sourceStatus,
            String rankingCategory,
            List<String> nextBestActions
    ) {
        public RecommendedCareer(String name, String reason, List<String> requirements, List<String> relatedProgrammes) {
            this(name, reason, requirements, relatedProgrammes, null, null, List.of(), List.of(), List.of(), null, null, List.of());
        }
    }

    public record RecommendedProgramme(
            String name,
            String university,
            List<String> admissionRequirements,
            String notes,
            String recommendationReason,
            String confidenceLevel,
            List<String> verifiedFacts,
            List<String> inferredInsights,
            List<String> missingData,
            String sourceStatus,
            String rankingCategory,
            List<String> nextBestActions
    ) {
        public RecommendedProgramme(String name, String university, List<String> admissionRequirements, String notes) {
            this(name, university, admissionRequirements, notes, null, null, List.of(), List.of(), List.of(), null, null, List.of());
        }
    }

    public record RecommendedBursary(
            String name,
            String provider,
            List<String> eligibility,
            String notes,
            List<String> sourceUrls,
            boolean officialSource
    ) {
    }

    public record SourceDiagnostic(
            String sourceUrl,
            String fetchStatus,
            String failureReason,
            String university,
            boolean usableProgrammeData
    ) {
    }

    public record SourceCoverage(
            Integer requestedSourcesCount,
            Integer successfulSourcesCount,
            Integer failedSourcesCount,
            Integer partialSourcesCount,
            List<String> universitiesWithUsableProgrammeData
    ) {
    }
}

