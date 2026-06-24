package com.edurite.ai.service;

import com.edurite.ai.exception.AiServiceException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiProviderOrchestratorServiceTest {

    @Test
    void primaryProviderSuccessReturnsPrimaryContent() {
        AiProviderOrchestratorService orchestrator = new AiProviderOrchestratorService(
                Map.of(
                        "geminiService", prompt -> "primary-content",
                        "openAiService", prompt -> "fallback-content"
                ),
                env("gemini", "openai")
        );

        String content = orchestrator.generateContent("hello");

        assertThat(content).isEqualTo("primary-content");
    }

    @Test
    void primaryFailureFallbackSuccessReturnsFallbackContent() {
        AiProviderOrchestratorService orchestrator = new AiProviderOrchestratorService(
                Map.of(
                        "geminiService", prompt -> {
                            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "AI_PROVIDER_UNAVAILABLE",
                                    "Primary provider failed",
                                    "AI guidance is temporarily unavailable. Please try again later.");
                        },
                        "openAiService", prompt -> "fallback-content"
                ),
                env("gemini", "openai")
        );

        String content = orchestrator.generateContent("hello");

        assertThat(content).isEqualTo("fallback-content");
    }

    @Test
    void allProvidersFailRaisesFriendlyException() {
        AiProviderOrchestratorService orchestrator = new AiProviderOrchestratorService(
                Map.of(
                        "geminiService", prompt -> {
                            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "AI_PROVIDER_UNAVAILABLE",
                                    "Primary provider failed",
                                    "AI guidance is temporarily unavailable. Please try again later.");
                        },
                        "openAiService", prompt -> {
                            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "AI_PROVIDER_UNAVAILABLE",
                                    "Fallback provider failed",
                                    "AI guidance is temporarily unavailable. Please try again later.");
                        }
                ),
                env("gemini", "openai")
        );

        assertThatThrownBy(() -> orchestrator.generateContent("hello"))
                .isInstanceOf(AiServiceException.class)
                .satisfies(ex -> {
                    AiServiceException aiEx = (AiServiceException) ex;
                    assertThat(aiEx.getErrorCode()).isEqualTo("AI_PROVIDER_UNAVAILABLE");
                    assertThat(aiEx.getUserMessage()).isEqualTo("AI guidance is temporarily unavailable. Please try again later.");
                });
    }

    @Test
    void invalidPrimaryResponseBodyFallsBackToSecondaryProvider() {
        AiProviderOrchestratorService orchestrator = new AiProviderOrchestratorService(
                Map.of(
                        "geminiService", prompt -> "   ",
                        "openAiService", prompt -> "fallback-content"
                ),
                env("gemini", "openai")
        );

        String content = orchestrator.generateContent("hello");

        assertThat(content).isEqualTo("fallback-content");
    }

    @Test
    void timeoutOnPrimaryProviderFallsBackToSecondaryProvider() {
        AiProviderOrchestratorService orchestrator = new AiProviderOrchestratorService(
                Map.of(
                        "geminiService", prompt -> {
                            throw new AiServiceException(HttpStatus.GATEWAY_TIMEOUT,
                                    "AI_PROVIDER_TIMEOUT",
                                    "Primary provider timed out",
                                    "AI guidance is temporarily unavailable. Please try again later.");
                        },
                        "openAiService", prompt -> "fallback-content"
                ),
                env("gemini", "openai")
        );

        String content = orchestrator.generateContent("hello");

        assertThat(content).isEqualTo("fallback-content");
    }

    private MockEnvironment env(String primary, String fallback) {
        return new MockEnvironment()
                .withProperty("ai.primary-provider", primary)
                .withProperty("ai.fallback-provider", fallback);
    }
}


