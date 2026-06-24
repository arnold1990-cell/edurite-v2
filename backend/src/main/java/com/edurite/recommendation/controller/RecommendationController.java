package com.edurite.recommendation.controller;

import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.service.RecommendationService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class named RecommendationController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
@RestController
@RequestMapping({"/api/v1/recommendations", "/api/recommendations"})
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * this method handles the "myRecommendations" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    @GetMapping("/me")
    public RecommendationResultDto myRecommendations(Principal principal) {
        return recommendationService.generateForStudent(principal);
    }
}

