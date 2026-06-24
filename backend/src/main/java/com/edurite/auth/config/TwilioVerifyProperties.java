package com.edurite.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edurite.auth.otp.twilio")
public record TwilioVerifyProperties(
        String accountSid,
        String authToken,
        String verifyServiceSid,
        String baseUrl
) {
}

