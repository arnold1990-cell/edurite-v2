package com.edurite.roadmap.controller;

import com.edurite.roadmap.service.StudyPlanService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/student/career-roadmaps/study-plans", "/api/v1/student/career-roadmaps/study-plans"})
public class StudyPlanController {
    private final StudyPlanService service;
    public StudyPlanController(StudyPlanService service) { this.service=service; }
    @GetMapping public List<StudyPlanService.Plan> history(Principal principal) { return service.history(principal); }
    @PostMapping public StudyPlanService.Plan generate(Principal principal, @Valid @RequestBody StudyPlanService.Generate request) { return service.generate(principal, request); }
    @PutMapping("/{id}/progress") public StudyPlanService.Plan progress(Principal principal, @PathVariable UUID id, @Valid @RequestBody StudyPlanService.Progress request) { return service.progress(principal,id,request); }
}
