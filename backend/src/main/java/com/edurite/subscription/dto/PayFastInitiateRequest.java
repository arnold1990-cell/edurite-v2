package com.edurite.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record PayFastInitiateRequest(
        @NotBlank(message = "planCode is required")
        String planCode
) {
}

