package com.edurite.psychometric.controller;

import com.edurite.psychometric.dto.PsychometricAssessmentDto;
import com.edurite.psychometric.dto.PsychometricAttemptRequest;
import com.edurite.psychometric.dto.PsychometricAttemptResultDto;
import com.edurite.psychometric.dto.PsychometricQuestionDto;
import com.edurite.psychometric.dto.PsychometricSubmissionRequest;
import com.edurite.psychometric.dto.PsychometricSubmissionResponse;
import com.edurite.psychometric.service.PsychometricService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1", "/api"})
public class PsychometricController {

    private final PsychometricService psychometricService;

    public PsychometricController(PsychometricService psychometricService) {
        this.psychometricService = psychometricService;
    }

    @GetMapping("/student/psychometric/assessments")
    public List<PsychometricAssessmentDto> studentAssessments() {
        return psychometricService.listAssessments();
    }

    @GetMapping("/student/psychometric/assessments/{assessmentId}/questions")
    public List<PsychometricQuestionDto> studentAssessmentQuestions(@PathVariable UUID assessmentId) {
        return psychometricService.assessmentQuestions(assessmentId);
    }

    @PostMapping("/student/psychometric/assessments/{assessmentId}/attempts")
    public PsychometricAttemptResultDto submitAssessmentAttempt(
            Principal principal,
            @PathVariable UUID assessmentId,
            @Valid @RequestBody PsychometricAttemptRequest request
    ) {
        return psychometricService.submitAssessmentAttempt(principal, assessmentId, request);
    }

    @GetMapping("/student/psychometric/assessments/{assessmentId}/attempts")
    public List<PsychometricAttemptResultDto> attemptHistory(
            Principal principal,
            @PathVariable UUID assessmentId
    ) {
        return psychometricService.assessmentAttemptHistory(principal, assessmentId);
    }

    @PostMapping("/student/psychometric/submit")
    public PsychometricSubmissionResponse submitForStudent(
            Principal principal,
            @Valid @RequestBody PsychometricSubmissionRequest request
    ) {
        return psychometricService.submitForStudent(principal, request);
    }

    @GetMapping("/student/psychometric/latest")
    public PsychometricSubmissionResponse latestForStudent(Principal principal) {
        return psychometricService.latestForStudent(principal);
    }

    @PostMapping("/public/psychometric/submit")
    public PsychometricSubmissionResponse submitPublic(
            @RequestParam(required = false) String sessionId,
            @Valid @RequestBody PsychometricSubmissionRequest request
    ) {
        return psychometricService.submitPublic(sessionId, request);
    }
}

