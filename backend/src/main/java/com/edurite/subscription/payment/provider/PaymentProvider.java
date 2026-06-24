package com.edurite.subscription.payment.provider;

import java.util.Map;

public interface PaymentProvider {
    String providerCode();

    PaymentCheckoutResult createCheckout(PaymentCheckoutContext context);

    PaymentConfirmationResult confirmPayment(PaymentConfirmationContext context);

    default PaymentWebhookResult handleWebhook(Map<String, String> headers, String rawPayload) {
        return new PaymentWebhookResult(
                providerCode(),
                null,
                "UNHANDLED",
                null,
                "PENDING",
                false,
                Map.of("rawPayload", rawPayload == null ? "" : rawPayload)
        );
    }
}

