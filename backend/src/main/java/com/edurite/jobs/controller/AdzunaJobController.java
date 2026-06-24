package com.edurite.jobs.controller;

import com.edurite.jobs.dto.JobOpportunityDto;
import com.edurite.jobs.service.AdzunaJobService;
import com.edurite.jobs.service.AdzunaJobService.JobSearchException;
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
@RequestMapping({"/api/v1/jobs", "/api/jobs"})
public class AdzunaJobController {

    private static final Logger log = LoggerFactory.getLogger(AdzunaJobController.class);
    private static final String FRIENDLY_ERROR = "Job opportunities are temporarily unavailable. Please try again.";

    private final AdzunaJobService adzunaJobService;

    public AdzunaJobController(AdzunaJobService adzunaJobService) {
        this.adzunaJobService = adzunaJobService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(name = "query", defaultValue = "software developer") String query,
            @RequestParam(name = "location", defaultValue = "Cape Town") String location,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "experience", required = false) String experience
    ) {
        try {
            List<JobOpportunityDto> results = adzunaJobService.searchJobs(query, location, category, experience);
            return ResponseEntity.ok(results);
        } catch (JobSearchException ex) {
            log.error("Adzuna jobs search failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(HttpStatus.BAD_GATEWAY));
        } catch (RuntimeException ex) {
            log.error("Adzuna jobs search crashed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    private Map<String, Object> errorBody(HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", FRIENDLY_ERROR);
        return body;
    }
}


