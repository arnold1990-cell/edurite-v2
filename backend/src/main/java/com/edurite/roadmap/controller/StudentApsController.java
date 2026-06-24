package com.edurite.roadmap.controller;

import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsCalculationRequest;
import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsCalculationResponse;
import com.edurite.roadmap.service.CareerRoadmapService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/aps", "/api/student/aps"})
public class StudentApsController {

    private final CareerRoadmapService careerRoadmapService;

    public StudentApsController(CareerRoadmapService careerRoadmapService) {
        this.careerRoadmapService = careerRoadmapService;
    }

    @PostMapping("/calculate")
    public ApsCalculationResponse calculate(@RequestBody ApsCalculationRequest request) {
        return careerRoadmapService.calculateAps(request);
    }

    @GetMapping("/profile")
    public ApsCalculationResponse profile(Principal principal) {
        return careerRoadmapService.apsProfile(principal);
    }
}
