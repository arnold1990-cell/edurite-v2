package com.edurite.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edurite.auth.otp")
public record AuthOtpProperties(boolean enabled) {
}

