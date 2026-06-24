package com.edurite.school.controller;

import com.edurite.school.dto.SchoolRegistrationDtos;
import com.edurite.school.service.SchoolRegistrationService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/school-registration", "/api/school-registration"})
public class SchoolRegistrationController {

    private final SchoolRegistrationService schoolRegistrationService;

    public SchoolRegistrationController(SchoolRegistrationService schoolRegistrationService) {
        this.schoolRegistrationService = schoolRegistrationService;
    }

    @GetMapping("/me")
    public SchoolRegistrationDtos.SchoolRegistrationStatusResponse me(Principal principal) {
        return schoolRegistrationService.myStatus(principal);
    }
}
