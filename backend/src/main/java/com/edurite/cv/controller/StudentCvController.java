package com.edurite.cv.controller;

import com.edurite.cv.dto.StudentCvDtos.StudentCvResponse;
import com.edurite.cv.dto.StudentCvDtos.StudentCvSuggestionResponse;
import com.edurite.cv.dto.StudentCvDtos.StudentCvUpsertRequest;
import com.edurite.cv.service.StudentCvService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/cv", "/api/student/cv"})
public class StudentCvController {

    private final StudentCvService studentCvService;

    public StudentCvController(StudentCvService studentCvService) {
        this.studentCvService = studentCvService;
    }

    @GetMapping
    public StudentCvResponse get(Principal principal) {
        return studentCvService.get(principal);
    }

    @PutMapping
    public StudentCvResponse upsert(Principal principal, @Valid @RequestBody StudentCvUpsertRequest request) {
        return studentCvService.upsert(principal, request);
    }

    @GetMapping("/ai-suggestions")
    public StudentCvSuggestionResponse suggestions(Principal principal) {
        return studentCvService.suggestions(principal);
    }
}

