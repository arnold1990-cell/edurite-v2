package com.edurite.discovery.controller;

import com.edurite.discovery.dto.PublicDiscoveryInsightDto;
import com.edurite.discovery.service.PublicDiscoveryInsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/public/discovery", "/api/public/discovery"})
public class PublicDiscoveryController {

    private final PublicDiscoveryInsightService publicDiscoveryInsightService;

    public PublicDiscoveryController(PublicDiscoveryInsightService publicDiscoveryInsightService) {
        this.publicDiscoveryInsightService = publicDiscoveryInsightService;
    }

    @GetMapping("/careers/insight")
    public PublicDiscoveryInsightDto careersInsight(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String industry,
            @RequestParam(defaultValue = "") String qualificationLevel,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "5") int top
    ) {
        return publicDiscoveryInsightService.careersInsight(q, industry, qualificationLevel, location, top);
    }

    @GetMapping("/courses/insight")
    public PublicDiscoveryInsightDto coursesInsight(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String level,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "5") int top
    ) {
        return publicDiscoveryInsightService.coursesInsight(q, level, location, top);
    }

    @GetMapping("/bursaries/insight")
    public PublicDiscoveryInsightDto bursariesInsight(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String qualification,
            @RequestParam(defaultValue = "") String region,
            @RequestParam(defaultValue = "") String eligibility,
            @RequestParam(defaultValue = "5") int top
    ) {
        return publicDiscoveryInsightService.bursariesInsight(q, qualification, region, eligibility, top);
    }
}

