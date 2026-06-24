package com.edurite.school.controller;

import com.edurite.school.dto.SchoolDtos.LinkStudentRequest;
import com.edurite.school.dto.SchoolDtos.SchoolProfileRequest;
import com.edurite.school.dto.SchoolDtos.SchoolProfileResponse;
import com.edurite.school.dto.SchoolDtos.SchoolSummaryResponse;
import com.edurite.school.service.SchoolService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/schools", "/api/admin/schools"})
public class SchoolPortalController {

    private final SchoolService schoolService;

    public SchoolPortalController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping
    public List<SchoolProfileResponse> list() {
        return schoolService.listSchools();
    }

    @PostMapping
    public SchoolProfileResponse create(@Valid @RequestBody SchoolProfileRequest request) {
        return schoolService.create(request);
    }

    @PutMapping("/{schoolId}")
    public SchoolProfileResponse update(@PathVariable UUID schoolId, @Valid @RequestBody SchoolProfileRequest request) {
        return schoolService.update(schoolId, request);
    }

    @PostMapping("/{schoolId}/students")
    public SchoolSummaryResponse linkStudent(@PathVariable UUID schoolId, @Valid @RequestBody LinkStudentRequest request) {
        return schoolService.linkStudent(schoolId, request);
    }

    @GetMapping("/{schoolId}/summary")
    public SchoolSummaryResponse summary(@PathVariable UUID schoolId) {
        return schoolService.summary(schoolId);
    }
}

