package com.edurite.ai.university;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversityUrlNormalizerTest {

    private final UniversityUrlNormalizer normalizer = new UniversityUrlNormalizer();

    @Test
    void normalizesHostFragmentTrailingSlashAndTrackingParams() {
        String normalized = normalizer.normalize("HTTPS://WWW.Example.AC.ZA/programmes/?utm_source=x&id=2#top");

        assertThat(normalized).isEqualTo("https://www.example.ac.za/programmes?id=2");
    }

    @Test
    void keepsDeterministicQueryOrderAfterDroppingTrackingParameters() {
        String normalized = normalizer.normalize("https://www.example.ac.za/courses?b=2&utm_medium=social&a=1");

        assertThat(normalized).isEqualTo("https://www.example.ac.za/courses?a=1&b=2");
    }
}

