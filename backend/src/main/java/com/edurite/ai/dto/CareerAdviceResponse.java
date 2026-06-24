package com.edurite.ai.dto;

import java.util.List;

public record CareerAdviceResponse(
        List<RecommendedCareer> recommendedCareers,
        String planCode,
        Boolean premiumUnlocked,
        Integer careerSuggestionLimit,
        Boolean careerSuggestionsLimited,
        String upgradeMessage
) {

    public CareerAdviceResponse(List<RecommendedCareer> recommendedCareers) {
        this(recommendedCareers, null, null, null, null, null);
    }

    public record RecommendedCareer(
            String name,
            Integer matchScore,
            String reason,
            List<String> improvements
    ) {
    }
}

