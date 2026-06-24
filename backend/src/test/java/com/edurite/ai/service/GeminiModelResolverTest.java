package com.edurite.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiModelResolverTest {

    @Test
    void resolveModelNameNormalizesCommonAccidentalPrefixes() {
        assertThat(GeminiModelResolver.resolveModelName("gemini-2.0-flash"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("  /models/gemini-2.0-flash  "))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("/gemini-2.0-flash"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("models/gemini-2.0-flash"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("models/models/gemini-2.0-flash"))
                .isEqualTo("gemini-2.0-flash");
    }

    @Test
    void resolveModelNameNormalizesEndpointStyleValuesFromConfiguration() {
        assertThat(GeminiModelResolver.resolveModelName("models/gemini-2.0-flash:generateContent"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("v1/models/gemini-2.0-flash:generateContent"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("v1beta/models/gemini-2.0-flash:generateContent"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=test"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("gemini-2.5-flash"))
                .isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("models/gemini-2.5-flash"))
                .isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("v1/models/gemini-2.5-flash"))
                .isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("v1beta/models/gemini-2.5-flash"))
                .isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent"))
                .isEqualTo("gemini-2.5-flash");
    }

    @Test
    void resolveModelNameFallsBackToDefaultForNullOrBlank() {
        assertThat(GeminiModelResolver.resolveModelName(null)).isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("")).isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("   ")).isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("models/   ")).isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("/models/   ")).isEqualTo("gemini-2.5-flash");
    }

    @Test
    void resolveModelNameFallsBackToDefaultForInvalidNames() {
        assertThat(GeminiModelResolver.resolveModelName("models/text-bison-001"))
                .isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("invalid model name"))
                .isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.fallsBackToDefault("invalid model name")).isTrue();
        assertThat(GeminiModelResolver.fallsBackToDefault("gemini-2.5-flash")).isFalse();
    }

    @Test
    void buildGenerateContentPathUsesApiVersionFromModelOrBaseUrl() {
        assertThat(GeminiModelResolver.buildGenerateContentPath("gemini-2.0-flash", null))
                .isEqualTo("/v1beta/models/gemini-2.0-flash:generateContent");
        assertThat(GeminiModelResolver.buildGenerateContentPath("v1/models/gemini-2.0-flash", null))
                .isEqualTo("/v1/models/gemini-2.0-flash:generateContent");
        assertThat(GeminiModelResolver.buildGenerateContentPath("models/gemini-2.0-flash", "https://generativelanguage.googleapis.com/v1"))
                .isEqualTo("/v1/models/gemini-2.0-flash:generateContent");
    }

    @Test
    void normalizeBaseUrlRemovesVersionAndModelSegments() {
        assertThat(GeminiModelResolver.normalizeBaseUrl("https://generativelanguage.googleapis.com/v1beta/"))
                .isEqualTo("https://generativelanguage.googleapis.com");
        assertThat(GeminiModelResolver.normalizeBaseUrl("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent"))
                .isEqualTo("https://generativelanguage.googleapis.com");
        assertThat(GeminiModelResolver.normalizeBaseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash"))
                .isEqualTo("https://generativelanguage.googleapis.com");
    }

    @Test
    void buildGenerateContentPathContainsExactlyOneModelsSegment() {
        String path = GeminiModelResolver.buildGenerateContentPath("models/models/gemini-2.0-flash", null);

        assertThat(path).contains("/v1beta/models/");
        assertThat(path).doesNotContain("/models/models/");
    }
}

