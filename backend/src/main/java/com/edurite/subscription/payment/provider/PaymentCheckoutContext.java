package com.edurite.subscription.payment.provider;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCheckoutContext(
        UUID userId,
        String paymentReference,
        String planCode,
        String planName,
        String billingInterval,
        BigDecimal amount,
        String currency,
        String description,
        String customerEmail,
        String customerFirstName,
        String customerLastName,
        String successUrl,
        String cancelUrl,
        String notifyUrl
) {
}

