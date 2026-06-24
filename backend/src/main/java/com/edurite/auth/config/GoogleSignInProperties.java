package com.edurite.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edurite.auth.google")
public record GoogleSignInProperties(
        boolean enabled,
        String clientId,
        String tokenInfoUrl
) {
}

