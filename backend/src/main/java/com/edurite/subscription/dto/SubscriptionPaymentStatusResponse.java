package com.edurite.subscription.dto;

public record SubscriptionPaymentStatusResponse(
        String paymentReference,
        String provider,
        String paymentStatus,
        String subscriptionStatus,
        String message
) {
}

