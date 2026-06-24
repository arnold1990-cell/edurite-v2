package com.edurite.subscription.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edurite.payment")
public record PaymentGatewayProperties(
        String provider,
        String callbackUrl,
        String apiKey,
        String secret,
        String baseUrl
) {
}

