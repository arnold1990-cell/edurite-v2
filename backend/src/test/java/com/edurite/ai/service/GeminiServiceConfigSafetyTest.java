package com.edurite.ai.service;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiServiceConfigSafetyTest {

    @Test
    void missingApiKeyFailsAtRequestTimeNotConstructionTime() {
        GeminiService service = GeminiServiceTestFactory.service("", "models/gemini-2.0-flash", "https://generativelanguage.googleapis.com");

        assertThatThrownBy(() -> service.getCareerAdvice(new CareerAdviceRequest("hs", "tech", "java", "harare")))
                .isInstanceOf(AiServiceException.class)
                .satisfies(ex -> {
                    AiServiceException aiEx = (AiServiceException) ex;
                    assertThat(aiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(aiEx.getErrorCode()).isEqualTo("AI_PROVIDER_CONFIGURATION_INVALID");
                });
    }

    @Test
    void missingApiKeyReturnsWarningOnlyForUniversitySourceAnalysis() {
        GeminiService service = GeminiServiceTestFactory.service("", "gemini-2.5-flash", "https://generativelanguage.googleapis.com");

        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/page"), "Software", "Developer", "Undergraduate", 10),
                profile,
                List.of("https://www.unisa.ac.za/page"),
                List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/page", "t", UniversityPageType.QUALIFICATION_LIST,
                        "content", Set.of("computer science"), true, null, null)),
                "content"
        );

        assertThat(response.summary()).contains("Live AI guidance is currently unavailable");
        assertThat(response.recommendedCareers()).isEmpty();
        assertThat(response.warnings()).isNotEmpty();
        assertThat(response.status()).isEqualTo("ERROR");
        assertThat(response.mode()).isEqualTo("UNAVAILABLE");
        assertThat(response.groundingStatus()).isEqualTo("FULLY_GROUNDED");
        assertThat(response.bursarySuggestions()).isEmpty();
    }

    @Test
    void resolvesApiKeyFromBoundProperties() {
        GeminiService service = GeminiServiceTestFactory.service("dot-notation-key");

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveApiKey");

        assertThat(resolved).isEqualTo("dot-notation-key");
    }

    @Test
    void resolvesModelFromBoundProperties() {
        GeminiService service = GeminiServiceTestFactory.service("test-key", "models/gemini-2.0-flash", "https://generativelanguage.googleapis.com");

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveModel");

        assertThat(resolved).isEqualTo("gemini-2.0-flash");
    }

    @Test
    void blankModelFallsBackToDefaultModel() {
        GeminiService service = GeminiServiceTestFactory.service("test-key", "  ", "https://generativelanguage.googleapis.com");

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveModel");

        assertThat(resolved).isEqualTo("gemini-2.5-flash");
    }

    @Test
    void blankBaseUrlFallsBackToDefaultBaseUrl() {
        GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.5-flash", "   ");

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveBaseUrl");

        assertThat(resolved).isEqualTo("https://generativelanguage.googleapis.com");
    }

    @Test
    void resolveBaseUrlStripsVersionAndTrailingSlash() {
        GeminiService service = GeminiServiceTestFactory.service(
                "test-key",
                "gemini-2.5-flash",
                "https://generativelanguage.googleapis.com/v1beta/"
        );

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveBaseUrl");

        assertThat(resolved).isEqualTo("https://generativelanguage.googleapis.com");
    }

    @Test
    void healthCheckUsesSameApiVersionAsGenerateContentPath() {
        GeminiService service = GeminiServiceTestFactory.service(
                "",
                "v1/models/gemini-2.0-flash",
                "https://generativelanguage.googleapis.com/"
        );

        GeminiService.GeminiHealthCheck health = service.checkHealth();

        assertThat(health.endpoint()).contains("/v1/models/gemini-2.0-flash");
    }

    @Test
    void missingSourceFieldsAreNormalizedToNotFound() {
        GeminiService service = GeminiServiceTestFactory.service();

        String payload = "{\"recommendedCareers\":[{\"name\":\"Software Developer\",\"reason\":\"Strong fit\",\"requirements\":[],\"relatedProgrammes\":[]}],\"recommendedProgrammes\":[{\"name\":\"BSc Computer Science\",\"university\":\"\",\"admissionRequirements\":[],\"notes\":\"\"}],\"recommendedUniversities\":[\"UNISA\"],\"minimumRequirements\":[],\"keyRequirements\":[],\"skillGaps\":[],\"recommendedNextSteps\":[],\"warnings\":[],\"inferredGuidance\":[\"Profile-aligned next step\"],\"bursarySuggestions\":[],\"summary\":\"Summary\",\"suitabilityScore\":75}";

        UniversitySourcesAnalysisResponse response = (UniversitySourcesAnalysisResponse) ReflectionTestUtils.invokeMethod(
                service,
                "parseUniversityAdvice",
                payload,
                List.of("https://www.unisa.ac.za/programmes"),
                List.of("https://www.unisa.ac.za/programmes"),
                List.of()
        );

        assertThat(response.recommendedProgrammes().get(0).admissionRequirements()).containsExactly("Not found in fetched sources");
        assertThat(response.recommendedProgrammes().get(0).notes()).isEqualTo("Not found in fetched sources");
        assertThat(response.recommendedProgrammes().get(0).university()).isEqualTo("Not found in fetched sources");
    }
}

