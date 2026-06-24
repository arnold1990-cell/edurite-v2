package com.edurite.subscription.dto;

import java.util.Map;

public record SubscriptionCheckoutResponse(
        String paymentReference,
        String provider,
        String paymentStatus,
        String subscriptionStatus,
        String checkoutUrl,
        String message,
        Map<String, Object> checkoutPayload
) {
}

