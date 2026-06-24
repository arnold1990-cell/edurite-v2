package com.edurite.subscription.payment.provider;

import java.util.Map;

public record PaymentCheckoutResult(
        String provider,
        String status,
        String externalOrderId,
        String externalSessionId,
        String externalPaymentId,
        String externalSubscriptionId,
        String checkoutUrl,
        String failureReason,
        Map<String, Object> rawResponse
) {
    public static PaymentCheckoutResult pending(
            String provider,
            String externalOrderId,
            String externalSessionId,
            String checkoutUrl,
            Map<String, Object> rawResponse
    ) {
        return new PaymentCheckoutResult(
                provider,
                "PENDING",
                externalOrderId,
                externalSessionId,
                null,
                null,
                checkoutUrl,
                null,
                rawResponse
        );
    }

    public static PaymentCheckoutResult completed(
            String provider,
            String externalPaymentId,
            String externalSubscriptionId,
            Map<String, Object> rawResponse
    ) {
        return new PaymentCheckoutResult(
                provider,
                "COMPLETED",
                null,
                null,
                externalPaymentId,
                externalSubscriptionId,
                null,
                null,
                rawResponse
        );
    }

    public static PaymentCheckoutResult failed(String provider, String failureReason, Map<String, Object> rawResponse) {
        return new PaymentCheckoutResult(
                provider,
                "FAILED",
                null,
                null,
                null,
                null,
                null,
                failureReason,
                rawResponse
        );
    }
}

