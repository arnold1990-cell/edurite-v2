package com.edurite.subscription.payment.provider;

import java.util.Map;

public record PaymentWebhookResult(
        String provider,
        String eventId,
        String eventType,
        String paymentReference,
        String status,
        boolean verified,
        Map<String, Object> payload
) {
}

