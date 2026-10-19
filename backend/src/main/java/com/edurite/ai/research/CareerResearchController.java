package com.edurite.ai.research;

import static com.edurite.ai.research.CareerResearchDtos.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping({"/api/ai/career-research", "/api/v1/ai/career-research"})
public class CareerResearchController {
    private final CareerResearchJobService jobs;

    public CareerResearchController(CareerResearchJobService jobs) {
        this.jobs = jobs;
    }

    public record ResearchRequest(String requestKey) {}

    @PostMapping
    public ResponseEntity<JobStatus> start(
            Principal principal, @RequestBody(required = false) ResearchRequest request) {
        return ResponseEntity.accepted()
                .body(jobs.start(principal, request == null ? null : request.requestKey()));
    }

    @GetMapping
    public JobStatus status(Principal principal) {
        return jobs.status(principal);
    }
}
