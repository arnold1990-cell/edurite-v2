package com.edurite.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionPaymentVerifyRequest(
        @NotBlank(message = "paymentReference is required")
        String paymentReference
) {
}

