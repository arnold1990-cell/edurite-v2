package com.edurite.student.controller;

import com.edurite.opportunity.dto.OpportunityType;
import com.edurite.opportunity.dto.UnifiedOpportunityResponse;
import com.edurite.opportunity.service.StudentOpportunityService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/opportunities", "/api/student/opportunities"})
public class StudentOpportunityController {

    private final StudentOpportunityService studentOpportunityService;

    public StudentOpportunityController(StudentOpportunityService studentOpportunityService) {
        this.studentOpportunityService = studentOpportunityService;
    }

    @GetMapping
    public List<UnifiedOpportunityResponse> list(
            Principal principal,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String field,
            @RequestParam(defaultValue = "") String industry,
            @RequestParam(defaultValue = "") String qualification,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String demand,
            @RequestParam(defaultValue = "ALL") String opportunityType
    ) {
        return studentOpportunityService.search(principal, q, field, industry, qualification, location, demand, opportunityType);
    }

    @PostMapping("/{type}/{opportunityId}/save")
    public ResponseEntity<Map<String, String>> save(
            Principal principal,
            @PathVariable OpportunityType type,
            @PathVariable String opportunityId,
            @RequestBody(required = false) Map<String, String> request
    ) {
        studentOpportunityService.saveOpportunity(principal, type, opportunityId, request == null ? null : request.get("title"));
        return ResponseEntity.ok(Map.of("message", "Opportunity saved"));
    }

    @DeleteMapping("/{type}/{opportunityId}/save")
    public ResponseEntity<Map<String, String>> unsave(
            Principal principal,
            @PathVariable OpportunityType type,
            @PathVariable String opportunityId
    ) {
        studentOpportunityService.unsaveOpportunity(principal, type, opportunityId);
        return ResponseEntity.ok(Map.of("message", "Opportunity removed"));
    }
}

