package com.edurite.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionPaymentCancelRequest(
        @NotBlank(message = "paymentReference is required")
        String paymentReference,
        String provider,
        String reason
) {
}

