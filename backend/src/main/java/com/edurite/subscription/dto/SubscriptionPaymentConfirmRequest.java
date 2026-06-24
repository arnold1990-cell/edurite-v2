package com.edurite.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionPaymentConfirmRequest(
        @NotBlank(message = "paymentReference is required")
        String paymentReference,
        String provider,
        String sessionId,
        String orderId,
        String token,
        String payerId
) {
}

