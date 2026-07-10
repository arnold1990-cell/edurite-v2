package com.edurite.learning.controller;

import com.edurite.learning.dto.LearningResourceDto;
import com.edurite.learning.service.LearningCentreService;
import com.edurite.learning.service.LearningCentreService.LearningIntegrationException;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
        "/api/v1/student/learning-centre",
        "/api/student/learning-centre",
        "/api/v1/learning-centre",
        "/api/learning-centre"
})
public class LearningCentreController {

    private static final Logger log = LoggerFactory.getLogger(LearningCentreController.class);
    private static final String FRIENDLY_ERROR_MESSAGE = "Learning resources are temporarily unavailable. Please try again.";

    private final LearningCentreService learningCentreService;

    public LearningCentreController(LearningCentreService learningCentreService) {
        this.learningCentreService = learningCentreService;
    }

    @GetMapping("/catalogue")
    public List<LearningResourceDto> catalogue() {
        return learningCentreService.listCatalogue();
    }

    @GetMapping("/courses")
    public List<LearningResourceDto> courses(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "provider", required = false) String provider,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "freeOnly", defaultValue = "true") boolean freeOnly
    ) {
        return learningCentreService.listCourses(search, category, provider, level, subject, freeOnly);
    }

    @PostMapping("/courses/refresh")
    public ResponseEntity<?> refreshCourses(Principal principal) {
        return guarded(() -> {
            var summary = learningCentreService.refreshCourses(principal);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Courses refreshed successfully.");
            body.put("total", summary.total());
            body.put("created", summary.created());
            body.put("updated", summary.updated());
            body.put("refreshedAt", summary.refreshedAt());
            return body;
        });
    }

    @GetMapping("/recommended")
    public List<LearningResourceDto> recommended(
            Principal principal,
            @RequestParam(name = "outcome", required = false) List<String> outcomes
    ) {
        return learningCentreService.recommendedForStudent(principal, outcomes);
    }

    @GetMapping("/books")
    public ResponseEntity<?> books(@RequestParam(name = "query", required = false) String query) {
        return guarded(() -> learningCentreService.searchOpenLibrary(query));
    }

    @GetMapping("/google-books")
    public ResponseEntity<?> googleBooks(@RequestParam(name = "query", required = false) String query) {
        return guarded(() -> learningCentreService.searchGoogleBooks(query));
    }

    @GetMapping("/quizzes")
    public ResponseEntity<?> quizzes(@RequestParam(name = "amount", defaultValue = "10") int amount) {
        return guarded(() -> learningCentreService.fetchQuizzes(amount));
    }

    @GetMapping("/videos")
    public ResponseEntity<?> videos(@RequestParam(name = "query", required = false) String query) {
        return guarded(() -> learningCentreService.searchYouTubeVideos(query));
    }

    private ResponseEntity<?> guarded(SupplierCall supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (LearningIntegrationException ex) {
            log.error("Learning centre provider call failed: {}", ex.getMessage(), ex);
            HttpStatus status = ex.getMessage().contains("administrators") ? HttpStatus.FORBIDDEN : HttpStatus.BAD_GATEWAY;
            return ResponseEntity.status(status).body(errorBody(status, ex.getMessage().contains("administrators") ? ex.getMessage() : FRIENDLY_ERROR_MESSAGE));
        } catch (RuntimeException ex) {
            log.error("Learning centre request failed unexpectedly: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(HttpStatus.BAD_GATEWAY, FRIENDLY_ERROR_MESSAGE));
        }
    }

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }

    @FunctionalInterface
    private interface SupplierCall {
        Object get();
    }
}
