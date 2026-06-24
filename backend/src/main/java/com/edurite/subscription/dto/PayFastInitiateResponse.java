package com.edurite.subscription.dto;

import java.util.Map;

public record PayFastInitiateResponse(
        String paymentReference,
        String provider,
        String paymentStatus,
        String subscriptionStatus,
        String paymentUrl,
        Map<String, String> formFields,
        String message
) {
}

