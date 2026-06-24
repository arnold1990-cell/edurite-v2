package com.edurite.subscription.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edurite.payment.paypal")
public record PayPalPaymentProperties(
        String clientId,
        String clientSecret,
        String mode,
        String baseUrl,
        String currency
) {
}

