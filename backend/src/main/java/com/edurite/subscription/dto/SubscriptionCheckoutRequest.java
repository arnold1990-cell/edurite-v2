package com.edurite.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionCheckoutRequest(
        @NotBlank(message = "planCode is required")
        String planCode,
        @NotBlank(message = "provider is required")
        String provider
) {
}

