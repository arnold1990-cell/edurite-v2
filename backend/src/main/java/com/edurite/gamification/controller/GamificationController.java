package com.edurite.gamification.controller;

import com.edurite.gamification.dto.GamificationSummaryDto;
import com.edurite.gamification.dto.RewardClaimRequest;
import com.edurite.gamification.entity.RewardClaim;
import com.edurite.gamification.service.GamificationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/gamification", "/api/student/gamification"})
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/summary")
    public GamificationSummaryDto summary(Principal principal) {
        return gamificationService.getSummary(principal);
    }

    @PostMapping("/tasks/complete")
    public Map<String, String> markTaskComplete(Principal principal, @RequestParam(required = false) String referenceId) {
        gamificationService.awardTaskCompletion(principal, referenceId);
        return Map.of("message", "Task completion recorded. Points awarded.");
    }

    @PostMapping("/claims")
    public RewardClaim claimReward(Principal principal, @Valid @RequestBody RewardClaimRequest request) {
        return gamificationService.claimReward(principal, request);
    }
}

