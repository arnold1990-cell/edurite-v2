package com.edurite.recommendation.dto;

/**
 * Catalogue suggestion. Score is an internal ordering weight, not a probability,
 * match percentage, admission decision or assessment score.
 */
public record RecommendationItemDto(String id, String title, int score, String rationale) {
}

