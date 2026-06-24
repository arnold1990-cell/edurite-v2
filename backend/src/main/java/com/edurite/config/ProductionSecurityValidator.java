package com.edurite.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecurityValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);
    private static final String DEFAULT_JWT_SECRET = "change-me";
    private static final int MIN_JWT_SECRET_LENGTH = 32;

    private final String jwtSecret;
    private final String datasourcePassword;
    private final String geminiApiKey;
    private final String aiPrimaryProvider;
    private final String aiFallbackProvider;
    private final boolean googleOauthEnabled;
    private final String googleClientId;
    private final String googleClientSecret;

    public ProductionSecurityValidator(
            @Value("${security.jwt.secret}") String jwtSecret,
            @Value("${spring.datasource.password:}") String datasourcePassword,
            @Value("${gemini.api-key:}") String geminiApiKey,
            @Value("${ai.primary-provider:gemini}") String aiPrimaryProvider,
            @Value("${ai.fallback-provider:openai}") String aiFallbackProvider,
            @Value("${edurite.auth.google.enabled:false}") boolean googleOauthEnabled,
            @Value("${spring.security.oauth2.client.registration.google.client-id:}") String googleClientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret:}") String googleClientSecret
    ) {
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
        this.datasourcePassword = datasourcePassword == null ? "" : datasourcePassword.trim();
        this.geminiApiKey = geminiApiKey == null ? "" : geminiApiKey.trim();
        this.aiPrimaryProvider = normalizeProvider(aiPrimaryProvider);
        this.aiFallbackProvider = normalizeProvider(aiFallbackProvider);
        this.googleOauthEnabled = googleOauthEnabled;
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
        this.googleClientSecret = googleClientSecret == null ? "" : googleClientSecret.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (jwtSecret.isBlank() || DEFAULT_JWT_SECRET.equals(jwtSecret) || jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            throw new IllegalStateException("Production startup blocked: JWT_SECRET must be set to a strong value with at least 32 characters.");
        }

        if (datasourcePassword.isBlank()) {
            throw new IllegalStateException("Production startup blocked: SPRING_DATASOURCE_PASSWORD must be configured.");
        }

        if (usesGeminiProvider() && geminiApiKey.isBlank()) {
            throw new IllegalStateException("Production startup blocked: GEMINI_API_KEY must be configured when Gemini is an active AI provider.");
        }

        if (googleOauthEnabled && (googleClientId.isBlank() || googleClientSecret.isBlank())) {
            throw new IllegalStateException("Production startup blocked: Google OAuth is enabled but GOOGLE_CLIENT_ID or GOOGLE_CLIENT_SECRET is missing.");
        }

        log.info("Production security validation passed.");
    }

    private boolean usesGeminiProvider() {
        return "gemini".equals(aiPrimaryProvider) || "gemini".equals(aiFallbackProvider);
    }

    private String normalizeProvider(String provider) {
        if (provider == null) {
            return "";
        }
        String normalized = provider.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        return normalized.endsWith("service")
                ? normalized.substring(0, normalized.length() - "service".length())
                : normalized;
    }
}
