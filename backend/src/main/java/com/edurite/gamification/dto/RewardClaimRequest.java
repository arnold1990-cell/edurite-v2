package com.edurite.gamification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RewardClaimRequest(
        @NotBlank(message = "rewardName is required")
        @Size(max = 180, message = "rewardName must be at most 180 characters")
        String rewardName,
        @Size(max = 500, message = "rewardDescription must be at most 500 characters")
        String rewardDescription
) {
}

