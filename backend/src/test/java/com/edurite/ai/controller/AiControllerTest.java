package com.edurite.ai.controller;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.StudentAiGuidanceService;
import com.edurite.ai.service.UniversitySourcesGuidanceService;
import com.edurite.ai.university.UniversitySourceCoverageService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiControllerTest {

    private static final String SAFE_MESSAGE = "Our AI could not process your request at this time. Please try again.";

    @Test
    void careerAdviceForStudentReturnsSafeMessageWhenAiProviderFails() {
        GeminiService geminiService = mock(GeminiService.class);
        UniversitySourcesGuidanceService universitySourcesGuidanceService = mock(UniversitySourcesGuidanceService.class);
        UniversitySourceCoverageService sourceCoverageService = mock(UniversitySourceCoverageService.class);
        StudentAiGuidanceService studentAiGuidanceService = mock(StudentAiGuidanceService.class);
        AiController controller = new AiController(geminiService, universitySourcesGuidanceService, sourceCoverageService, studentAiGuidanceService);

        Principal principal = () -> "student@example.com";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/ai/career-advice/me");
        when(studentAiGuidanceService.careerAdviceForStudent(principal))
                .thenThrow(new AiServiceException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_AUTH", "Gemini failed with 401", "Gemini auth failed"));

        ResponseEntity<?> response = controller.careerAdviceForStudent(principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo(SAFE_MESSAGE);
        assertThat(body.get("code")).isEqualTo("AI_PROVIDER_AUTH");
    }

    @Test
    void analyseUniversitySourcesReturnsSafeMessageOnUnexpectedRuntimeFailure() {
        GeminiService geminiService = mock(GeminiService.class);
        UniversitySourcesGuidanceService universitySourcesGuidanceService = mock(UniversitySourcesGuidanceService.class);
        UniversitySourceCoverageService sourceCoverageService = mock(UniversitySourceCoverageService.class);
        StudentAiGuidanceService studentAiGuidanceService = mock(StudentAiGuidanceService.class);
        AiController controller = new AiController(geminiService, universitySourcesGuidanceService, sourceCoverageService, studentAiGuidanceService);

        Principal principal = () -> "student@example.com";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/ai/analyse-university-sources");
        when(universitySourcesGuidanceService.analyse(any(), any())).thenThrow(new RuntimeException("timeout from provider"));

        UniversitySourcesAnalysisRequest payload = new UniversitySourcesAnalysisRequest(
                null, null, null, null, null
        );
        ResponseEntity<?> response = controller.analyseUniversitySources(payload, principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo(SAFE_MESSAGE);
    }
}

