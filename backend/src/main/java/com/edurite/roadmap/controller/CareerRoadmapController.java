package com.edurite.roadmap.controller;

import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapGenerateRequest;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapGenerateResponse;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapResponse;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapSaveRequest;
import com.edurite.roadmap.dto.CareerRoadmapDtos.SavedCareerRoadmapResponse;
import com.edurite.roadmap.dto.CareerRoadmapDtos.UniversityRequirementResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import com.edurite.roadmap.service.CareerRoadmapService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/career-roadmaps", "/api/student/career-roadmaps"})
public class CareerRoadmapController {

    private final CareerRoadmapService careerRoadmapService;

    public CareerRoadmapController(CareerRoadmapService careerRoadmapService) {
        this.careerRoadmapService = careerRoadmapService;
    }

    @GetMapping
    public List<CareerRoadmapResponse> list() {
        return careerRoadmapService.list();
    }

    @GetMapping("/{slug}")
    public CareerRoadmapResponse detail(@PathVariable String slug) {
        return careerRoadmapService.detail(slug);
    }

    @PostMapping("/generate")
    public CareerRoadmapGenerateResponse generate(Principal principal, @Valid @RequestBody CareerRoadmapGenerateRequest request) {
        return careerRoadmapService.generate(principal, request);
    }

    @GetMapping("/saved")
    public List<SavedCareerRoadmapResponse> saved(Principal principal) {
        return careerRoadmapService.saved(principal);
    }

    @PostMapping("/save")
    public SavedCareerRoadmapResponse save(Principal principal, @Valid @RequestBody CareerRoadmapSaveRequest request) {
        return careerRoadmapService.save(principal, request);
    }

    @GetMapping("/requirements")
    public List<UniversityRequirementResponse> requirements(@RequestParam("career") String career) {
        return careerRoadmapService.requirements(career);
    }
}

