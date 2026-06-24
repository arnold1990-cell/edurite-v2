package com.edurite.ai.service;

import com.edurite.ai.exception.AiServiceException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiProviderOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AiProviderOrchestratorService.class);
    private static final String FRIENDLY_FAILURE_MESSAGE = "AI guidance is temporarily unavailable. Please try again later.";

    private final Map<String, AiProviderService> providerBeans;
    private final Environment environment;

    public AiProviderOrchestratorService(Map<String, AiProviderService> providerBeans, Environment environment) {
        this.providerBeans = providerBeans == null ? Map.of() : new LinkedHashMap<>(providerBeans);
        this.environment = environment;
    }

    public String generateContent(String prompt) {
        String primaryProvider = environment.getProperty("ai.primary-provider", "gemini");
        String fallbackProvider = environment.getProperty("ai.fallback-provider", "openai");

        List<String> attemptedProviders = new ArrayList<>();

        ProviderAttempt primary = attempt(primaryProvider, "primary", prompt);
        if (primary.success()) {
            return primary.content();
        }
        attemptedProviders.add(primary.providerLabel());

        if (!sameProvider(primaryProvider, fallbackProvider)) {
            ProviderAttempt fallback = attempt(fallbackProvider, "fallback", prompt);
            if (fallback.success()) {
                return fallback.content();
            }
            attemptedProviders.add(fallback.providerLabel());
        }

        log.error("All AI providers failed. attemptedProviders={}", attemptedProviders);
        throw new AiServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI_PROVIDER_UNAVAILABLE",
                "All configured AI providers failed. attemptedProviders=" + attemptedProviders,
                FRIENDLY_FAILURE_MESSAGE
        );
    }

    private ProviderAttempt attempt(String configuredProvider, String role, String prompt) {
        ResolvedProvider provider = resolveProvider(configuredProvider);
        if (provider == null) {
            log.warn("Configured {} AI provider is unavailable: configuredProvider={}", role, configuredProvider);
            return ProviderAttempt.failure(configuredProvider);
        }

        try {
            String content = provider.service().generateContent(prompt);
            if (content == null || content.isBlank()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "AI_PROVIDER_RESPONSE_INVALID",
                        "Provider returned empty content: " + provider.providerName(),
                        FRIENDLY_FAILURE_MESSAGE);
            }
            log.info("AI provider handled request: provider={}, role={}", provider.providerName(), role);
            return ProviderAttempt.success(provider.providerName(), content.trim());
        } catch (AiServiceException ex) {
            log.warn("AI provider request failed: provider={}, role={}, errorCode={}, status={}, message={}",
                    provider.providerName(), role, ex.getErrorCode(), ex.getStatus(), sanitize(ex.getMessage()));
            return ProviderAttempt.failure(provider.providerName());
        } catch (RuntimeException ex) {
            log.warn("AI provider runtime failure: provider={}, role={}, message={}",
                    provider.providerName(), role, sanitize(ex.getMessage()), ex);
            return ProviderAttempt.failure(provider.providerName());
        }
    }

    private ResolvedProvider resolveProvider(String configuredProvider) {
        if (providerBeans.isEmpty()) {
            return null;
        }
        String normalizedRequested = normalizeProviderKey(configuredProvider);
        if (normalizedRequested.isBlank()) {
            return null;
        }

        for (Map.Entry<String, AiProviderService> entry : providerBeans.entrySet()) {
            String beanName = entry.getKey();
            AiProviderService service = entry.getValue();
            if (sameProvider(normalizedRequested, beanName)
                    || sameProvider(normalizedRequested, service.getClass().getSimpleName())) {
                return new ResolvedProvider(beanName, service);
            }
        }
        return null;
    }

    private boolean sameProvider(String left, String right) {
        return normalizeProviderKey(left).equals(normalizeProviderKey(right));
    }

    private String normalizeProviderKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.endsWith("service")) {
            normalized = normalized.substring(0, normalized.length() - "service".length());
        }
        return normalized;
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.replace('\n', ' ').replace('\r', ' ').trim();
        return trimmed.length() <= 240 ? trimmed : trimmed.substring(0, 240) + "...";
    }

    private record ProviderAttempt(
            boolean success,
            String providerLabel,
            String content
    ) {
        private static ProviderAttempt success(String providerLabel, String content) {
            return new ProviderAttempt(true, providerLabel, content);
        }

        private static ProviderAttempt failure(String providerLabel) {
            return new ProviderAttempt(false, providerLabel, "");
        }
    }

    private record ResolvedProvider(
            String providerName,
            AiProviderService service
    ) {
    }
}


