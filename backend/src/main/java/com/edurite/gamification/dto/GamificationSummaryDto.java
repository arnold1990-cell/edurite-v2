package com.edurite.gamification.dto;

import java.util.List;

public record GamificationSummaryDto(
        long totalPoints,
        long reservedPoints,
        long availablePoints,
        String currentTermCode,
        List<RecentPointEventDto> recentEvents,
        List<RewardClaimDto> recentClaims,
        List<RewardRuleDto> activeRules
) {
    public record RecentPointEventDto(String eventType, int points, String awardedAt, String referenceId) {}

    public record RewardClaimDto(String rewardName, String status, int claimedPoints, String claimedAt) {}

    public record RewardRuleDto(String code, String name, String description, String eventType, int pointsPerEvent, Integer maxPerTerm) {}
}

