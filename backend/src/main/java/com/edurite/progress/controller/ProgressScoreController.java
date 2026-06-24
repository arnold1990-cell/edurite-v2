package com.edurite.progress.controller;

import com.edurite.progress.dto.ProgressScoreDtos.ProgressScoreResponse;
import com.edurite.progress.service.ProgressScoreService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/progress-score", "/api/student/progress-score"})
public class ProgressScoreController {

    private final ProgressScoreService progressScoreService;

    public ProgressScoreController(ProgressScoreService progressScoreService) {
        this.progressScoreService = progressScoreService;
    }

    @GetMapping
    public ProgressScoreResponse score(Principal principal) {
        return progressScoreService.score(principal);
    }
}

