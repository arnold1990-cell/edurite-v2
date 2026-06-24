package com.edurite.recommendation.dto;

/**
 * this method handles the "RecommendationItemDto" step of the feature.
 * It exists to keep this class focused and reusable.
 */
public record RecommendationItemDto(String id, String title, int score, String rationale) {
}

