package com.edurite.application.controller;

import com.edurite.application.entity.ApplicationRecord;
import com.edurite.application.service.ApplicationService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1", "/api"})
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/bursaries/{id}/applications")
    public ApplicationRecord apply(@PathVariable UUID id, Principal principal) {
        return applicationService.submit(id, principal);
    }

    @GetMapping("/applications/me")
    public List<ApplicationRecord> myApplications(Principal principal) {
        return applicationService.listMine(principal);
    }
}

