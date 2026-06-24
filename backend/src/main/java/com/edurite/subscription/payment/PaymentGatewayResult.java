package com.edurite.subscription.payment;

public record PaymentGatewayResult(String status, String reference, String provider, String failureReason) {
    public static PaymentGatewayResult success(String reference, String provider) {
        return new PaymentGatewayResult("SUCCESS", reference, provider, null);
    }

    public static PaymentGatewayResult pending(String reference, String provider) {
        return new PaymentGatewayResult("PENDING", reference, provider, null);
    }

    public static PaymentGatewayResult cancelled(String reference, String provider, String reason) {
        return new PaymentGatewayResult("CANCELLED", reference, provider, reason);
    }

    public static PaymentGatewayResult failed(String reference, String provider, String failureReason) {
        return new PaymentGatewayResult("FAILED", reference, provider, failureReason);
    }
}

