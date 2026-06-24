package com.edurite.subscription.payment.provider;

import java.util.Map;

public record PaymentConfirmationResult(
        String provider,
        String status,
        String externalOrderId,
        String externalSessionId,
        String externalPaymentId,
        String externalSubscriptionId,
        String failureReason,
        Map<String, Object> rawResponse
) {
    public static PaymentConfirmationResult completed(
            String provider,
            String externalOrderId,
            String externalSessionId,
            String externalPaymentId,
            String externalSubscriptionId,
            Map<String, Object> rawResponse
    ) {
        return new PaymentConfirmationResult(
                provider,
                "COMPLETED",
                externalOrderId,
                externalSessionId,
                externalPaymentId,
                externalSubscriptionId,
                null,
                rawResponse
        );
    }

    public static PaymentConfirmationResult pending(
            String provider,
            String externalOrderId,
            String externalSessionId,
            Map<String, Object> rawResponse
    ) {
        return new PaymentConfirmationResult(
                provider,
                "PENDING",
                externalOrderId,
                externalSessionId,
                null,
                null,
                null,
                rawResponse
        );
    }

    public static PaymentConfirmationResult cancelled(String provider, String reason, Map<String, Object> rawResponse) {
        return new PaymentConfirmationResult(
                provider,
                "CANCELLED",
                null,
                null,
                null,
                null,
                reason,
                rawResponse
        );
    }

    public static PaymentConfirmationResult failed(String provider, String reason, Map<String, Object> rawResponse) {
        return new PaymentConfirmationResult(
                provider,
                "FAILED",
                null,
                null,
                null,
                null,
                reason,
                rawResponse
        );
    }
}

