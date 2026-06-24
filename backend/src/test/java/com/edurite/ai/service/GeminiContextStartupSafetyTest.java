package com.edurite.ai.service;

import com.edurite.ai.config.GeminiProperties;
import com.edurite.ai.exception.AiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiContextStartupSafetyTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class
            ))
            .withUserConfiguration(TestConfig.class)
            .withBean(ObjectMapper.class)
            .withBean(AiProviderOrchestratorService.class,
                    () -> new AiProviderOrchestratorService(Map.of(), new MockEnvironment()))
            .withBean(GeminiService.class);

    @Test
    void geminiBeanStartsWhenConfigurationIsValid() {
        contextRunner.withPropertyValues(
                "gemini.api-key=test-gemini-key",
                "gemini.model=gemini-2.5-flash",
                "gemini.base-url=https://generativelanguage.googleapis.com"
        ).run(context -> {
            assertThat(context).hasSingleBean(GeminiService.class);
            assertThat(context).hasNotFailed();
        });
    }

    @Test
    void geminiBeanStartsWithoutApiKeySoNonAiContextsCanLoad() {
        contextRunner.withPropertyValues(
                "gemini.api-key=   ",
                "gemini.model=gemini-2.5-flash",
                "gemini.base-url=https://generativelanguage.googleapis.com"
        ).run(context -> {
            assertThat(context).hasSingleBean(GeminiService.class);
            assertThat(context).hasNotFailed();
            GeminiService geminiService = context.getBean(GeminiService.class);
            assertThatThrownBy(() -> geminiService.generateContent("test prompt"))
                    .isInstanceOf(AiServiceException.class)
                    .hasMessageContaining("Gemini provider configuration is incomplete");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GeminiProperties.class)
    static class TestConfig {
    }
}

