package com.edurite.institution.universityinfo;

import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityRefreshResponse;
import com.edurite.institution.universityinfo.service.UniversityInfoService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/universities", "/api/admin/universities"})
public class AdminUniversityInfoController {

    private final UniversityInfoService universityInfoService;

    public AdminUniversityInfoController(UniversityInfoService universityInfoService) {
        this.universityInfoService = universityInfoService;
    }

    @PostMapping("/{slug}/refresh")
    public UniversityRefreshResponse refresh(@PathVariable String slug) {
        return universityInfoService.refreshUniversityData(slug);
    }
}
