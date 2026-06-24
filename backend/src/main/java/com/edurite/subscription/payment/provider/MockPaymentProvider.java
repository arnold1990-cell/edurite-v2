package com.edurite.subscription.payment.provider;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String providerCode() {
        return "mock";
    }

    @Override
    public PaymentCheckoutResult createCheckout(PaymentCheckoutContext context) {
        String externalPaymentId = "MOCK-PAY-" + UUID.randomUUID();
        return PaymentCheckoutResult.completed(
                providerCode(),
                externalPaymentId,
                null,
                Map.of(
                        "paymentReference", context.paymentReference(),
                        "externalPaymentId", externalPaymentId,
                        "mode", "mock"
                )
        );
    }

    @Override
    public PaymentConfirmationResult confirmPayment(PaymentConfirmationContext context) {
        String paymentId = context.providerPaymentId() == null || context.providerPaymentId().isBlank()
                ? "MOCK-CONFIRM-" + UUID.randomUUID()
                : context.providerPaymentId();
        return PaymentConfirmationResult.completed(
                providerCode(),
                context.providerOrderId(),
                context.providerSessionId(),
                paymentId,
                null,
                Map.of(
                        "paymentReference", context.paymentReference(),
                        "providerOrderId", context.providerOrderId() == null ? "" : context.providerOrderId()
                )
        );
    }

    @Override
    public PaymentWebhookResult handleWebhook(Map<String, String> headers, String rawPayload) {
        String normalizedStatus = headers.getOrDefault("x-mock-status", "COMPLETED")
                .trim()
                .toUpperCase(Locale.ROOT);
        String status = switch (normalizedStatus) {
            case "SUCCESS", "COMPLETED" -> "COMPLETED";
            case "CANCELLED", "CANCELED" -> "CANCELLED";
            case "FAILED" -> "FAILED";
            default -> "PENDING";
        };
        return new PaymentWebhookResult(
                providerCode(),
                headers.get("x-mock-event-id"),
                headers.getOrDefault("x-mock-event-type", "MOCK_EVENT"),
                headers.get("x-mock-reference"),
                status,
                true,
                Map.of("rawPayload", rawPayload == null ? "" : rawPayload)
        );
    }
}

