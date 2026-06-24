package com.edurite.ai.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversitySourcesAnalysisRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsMoreThanTenUrls() {
        var urls = IntStream.range(0, 11).mapToObj(i -> "https://www.unisa.ac.za/" + i).toList();
        var request = new UniversitySourcesAnalysisRequest(urls, null, null, null, 10);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsInvalidUrlFormat() {
        var request = new UniversitySourcesAnalysisRequest(List.of("ftp://unisa.ac.za/file"), null, null, null, 10);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void defaultsMaxRecommendationsToTen() {
        var request = new UniversitySourcesAnalysisRequest(null, null, null, null, null);

        assertThat(request.safeMaxRecommendations()).isEqualTo(10);
        assertThat(request.usesDefaultSources()).isTrue();
    }
}

