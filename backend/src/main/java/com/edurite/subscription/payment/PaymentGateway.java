package com.edurite.subscription.payment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface PaymentGateway {
    String providerCode();

    PaymentGatewayResult charge(UUID userId, BigDecimal amount, String currency, String description);

    default PaymentGatewayResult handleCallback(Map<String, String> callbackPayload) {
        String reference = callbackPayload.getOrDefault("reference", callbackPayload.getOrDefault("paymentReference", ""));
        return PaymentGatewayResult.failed(reference, providerCode(), "Callback handling is not implemented for this provider.");
    }
}

