package com.edurite.recommendation.dto;

import java.util.List;

public record RecommendationResultDto(
        List<RecommendationItemDto> suggestedCareers,
        List<RecommendationItemDto> suggestedBursaries,
        List<RecommendationItemDto> suggestedCoursesOrImprovements,
        List<String> profileImprovementTips,
        String modelVersion,
        String planCode,
        boolean premiumUnlocked,
        Integer careerSuggestionLimit,
        boolean careerSuggestionsLimited,
        String upgradeMessage
) {
}

