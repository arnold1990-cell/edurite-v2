package com.edurite.ai.controller;

import com.edurite.ai.dto.AiDashboardSummaryResponse;
import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.StudentAiGuidanceService;
import com.edurite.ai.service.UniversitySourcesGuidanceService;
import com.edurite.ai.university.UniversitySourceCoverage;
import com.edurite.ai.university.UniversitySourceCoverageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/ai", "/api/ai"})
@RequiredArgsConstructor
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private static final String AI_ERROR_MESSAGE = "Our AI could not process your request at this time. Please try again.";

    private final GeminiService geminiService;
    private final UniversitySourcesGuidanceService universitySourcesGuidanceService;
    private final UniversitySourceCoverageService sourceCoverageService;
    private final StudentAiGuidanceService studentAiGuidanceService;

    @PostMapping("/career-advice")
    public ResponseEntity<?> careerAdvice(@Valid @RequestBody CareerAdviceRequest request, HttpServletRequest httpRequest) {
        log.info("AI guidance endpoint hit: path={}, qualificationLevel={}, location={}, interestsLength={}, skillsLength={}",
                httpRequest.getRequestURI(),
                safeValue(request.qualificationLevel()),
                safeValue(request.location()),
                safeLength(request.interests()),
                safeLength(request.skills()));
        try {
            CareerAdviceResponse response = geminiService.getCareerAdvice(request);
            log.info("AI guidance request completed successfully: recommendations={}",
                    response.recommendedCareers() == null ? 0 : response.recommendedCareers().size());
            return ResponseEntity.ok(response);
        } catch (AiServiceException ex) {
            return errorResponse(httpRequest, ex);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "ai");
        body.put("status", "ok");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }


    @GetMapping("/career-advice/me")
    public ResponseEntity<?> careerAdviceForStudent(Principal principal, HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(studentAiGuidanceService.careerAdviceForStudent(principal));
        } catch (AiServiceException ex) {
            return errorResponse(httpRequest, ex);
        }
    }

    @GetMapping("/bursary-guidance/me")
    public ResponseEntity<List<com.edurite.bursary.dto.BursaryResultDto>> bursaryGuidanceForStudent(Principal principal) {
        return ResponseEntity.ok(studentAiGuidanceService.bursaryGuidanceForStudent(principal));
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<?> dashboardSummary(Principal principal, HttpServletRequest httpRequest) {
        try {
            AiDashboardSummaryResponse response = studentAiGuidanceService.dashboardSummary(principal);
            return ResponseEntity.ok(response);
        } catch (AiServiceException ex) {
            return errorResponse(httpRequest, ex);
        }
    }

    @PostMapping("/analyse-university-sources")
    public ResponseEntity<?> analyseUniversitySources(@Valid @RequestBody UniversitySourcesAnalysisRequest request,
                                                      Principal principal,
                                                      HttpServletRequest httpRequest) {
        log.info("University analysis request received: path={}, usesDefaultSources={}, requestedUrls={}, targetProgramPresent={}, careerInterestPresent={}, qualificationLevel={}",
                httpRequest.getRequestURI(),
                request.usesDefaultSources(),
                request.urls() == null ? 0 : request.urls().size(),
                request.targetProgram() != null && !request.targetProgram().isBlank(),
                request.careerInterest() != null && !request.careerInterest().isBlank(),
                safeValue(request.qualificationLevel()));
        try {
            UniversitySourcesAnalysisResponse response = universitySourcesGuidanceService.analyse(principal, request);
            return ResponseEntity.ok(response);
        } catch (AiServiceException ex) {
            return errorResponse(httpRequest, ex);
        } catch (RuntimeException ex) {
            log.error("University analysis request crashed before a controlled response was returned: path={}, message={}",
                    httpRequest.getRequestURI(), ex.getMessage(), ex);
            return unexpectedErrorResponse(httpRequest);
        }
    }

    @GetMapping("/default-university-sources")
    public ResponseEntity<List<String>> defaultUniversitySources() {
        return ResponseEntity.ok(universitySourcesGuidanceService.getDefaultSources());
    }

    @GetMapping("/source-coverage")
    public ResponseEntity<UniversitySourceCoverage> sourceCoverage() {
        return ResponseEntity.ok(sourceCoverageService.getCoverage());
    }

    @GetMapping("/gemini-health")
    public ResponseEntity<GeminiService.GeminiHealthCheck> geminiHealth() {
        return ResponseEntity.ok(geminiService.checkHealth());
    }

    private ResponseEntity<Map<String, Object>> unexpectedErrorResponse(HttpServletRequest httpRequest) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        body.put("message", AI_ERROR_MESSAGE);
        body.put("path", httpRequest.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpServletRequest httpRequest, AiServiceException ex) {
        log.error("AI recommendation failed: path={}, status={}, errorCode={}, detail={}",
                httpRequest.getRequestURI(),
                ex.getStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", ex.getStatus().value());
        body.put("error", ex.getStatus().getReasonPhrase());
        body.put("message", AI_ERROR_MESSAGE);
        body.put("code", ex.getErrorCode());
        body.put("path", httpRequest.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}

