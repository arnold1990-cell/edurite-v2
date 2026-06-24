package com.edurite.subscription.payment.provider;

import java.util.Map;

public record PaymentConfirmationContext(
        String paymentReference,
        String providerOrderId,
        String providerSessionId,
        String providerPaymentId,
        String token,
        String payerId,
        Map<String, String> metadata
) {
}

