package com.edurite.universityapplication.controller;

import com.edurite.universityapplication.dto.UniversityApplicationDtos.UniversityApplicationRequest;
import com.edurite.universityapplication.dto.UniversityApplicationDtos.UniversityApplicationResponse;
import com.edurite.universityapplication.service.UniversityApplicationService;
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
@RequestMapping({"/api/v1/student/university-applications", "/api/student/university-applications"})
public class UniversityApplicationController {

    private final UniversityApplicationService service;

    public UniversityApplicationController(UniversityApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<UniversityApplicationResponse> list(Principal principal) {
        return service.list(principal);
    }

    @PostMapping
    public UniversityApplicationResponse create(Principal principal, @Valid @RequestBody UniversityApplicationRequest request) {
        return service.create(principal, request);
    }

    @PutMapping("/{id}")
    public UniversityApplicationResponse update(Principal principal, @PathVariable UUID id, @Valid @RequestBody UniversityApplicationRequest request) {
        return service.update(principal, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(Principal principal, @PathVariable UUID id) {
        service.delete(principal, id);
        return ResponseEntity.ok(Map.of("message", "University application deleted"));
    }
}

