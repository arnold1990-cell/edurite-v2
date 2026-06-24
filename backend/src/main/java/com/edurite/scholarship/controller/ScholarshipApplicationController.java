package com.edurite.scholarship.controller;

import com.edurite.scholarship.dto.ScholarshipApplicationDtos.MotivationLetterResponse;
import com.edurite.scholarship.dto.ScholarshipApplicationDtos.ScholarshipApplicationRequest;
import com.edurite.scholarship.dto.ScholarshipApplicationDtos.ScholarshipApplicationResponse;
import com.edurite.scholarship.service.ScholarshipApplicationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/scholarship-applications", "/api/student/scholarship-applications"})
public class ScholarshipApplicationController {

    private final ScholarshipApplicationService service;

    public ScholarshipApplicationController(ScholarshipApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ScholarshipApplicationResponse> list(Principal principal) {
        return service.list(principal);
    }

    @GetMapping("/upcoming")
    public List<ScholarshipApplicationResponse> upcoming(Principal principal) {
        return service.upcoming(principal);
    }

    @PostMapping
    public ScholarshipApplicationResponse create(Principal principal, @Valid @RequestBody ScholarshipApplicationRequest request) {
        return service.create(principal, request);
    }

    @PutMapping("/{id}")
    public ScholarshipApplicationResponse update(Principal principal, @PathVariable UUID id, @Valid @RequestBody ScholarshipApplicationRequest request) {
        return service.update(principal, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(Principal principal, @PathVariable UUID id) {
        service.delete(principal, id);
        return ResponseEntity.ok(Map.of("message", "Scholarship application deleted"));
    }

    @PostMapping("/{id}/motivation-letter")
    public MotivationLetterResponse motivationLetter(Principal principal, @PathVariable UUID id) {
        return service.motivationLetter(principal, id);
    }
}

