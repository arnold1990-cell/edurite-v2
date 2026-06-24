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
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody());
        } catch (RuntimeException ex) {
            log.error("Learning centre request failed unexpectedly: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody());
        }
    }

    private Map<String, Object> errorBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_GATEWAY.value());
        body.put("error", HttpStatus.BAD_GATEWAY.getReasonPhrase());
        body.put("message", FRIENDLY_ERROR_MESSAGE);
        return body;
    }

    @FunctionalInterface
    private interface SupplierCall {
        List<LearningResourceDto> get();
    }
}

