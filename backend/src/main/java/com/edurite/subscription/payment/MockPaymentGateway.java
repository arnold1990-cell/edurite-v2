package com.edurite.subscription.payment;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public String providerCode() {
        return "mock";
    }

    @Override
    public PaymentGatewayResult charge(UUID userId, BigDecimal amount, String currency, String description) {
        return PaymentGatewayResult.success("MOCK-" + userId + "-" + System.currentTimeMillis(), providerCode());
    }

    @Override
    public PaymentGatewayResult handleCallback(Map<String, String> callbackPayload) {
        String reference = callbackPayload.getOrDefault("reference", callbackPayload.getOrDefault("paymentReference", ""));
        String normalizedStatus = callbackPayload.getOrDefault("status", "SUCCESS").trim().toUpperCase(Locale.ROOT);
        return switch (normalizedStatus) {
            case "SUCCESS", "COMPLETED" -> PaymentGatewayResult.success(reference, providerCode());
            case "PENDING" -> PaymentGatewayResult.pending(reference, providerCode());
            case "CANCELLED", "CANCELED" -> PaymentGatewayResult.cancelled(reference, providerCode(), callbackPayload.get("reason"));
            default -> PaymentGatewayResult.failed(reference, providerCode(), callbackPayload.getOrDefault("reason", "Payment failed"));
        };
    }
}

