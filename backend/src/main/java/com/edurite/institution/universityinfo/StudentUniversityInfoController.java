package com.edurite.institution.universityinfo;

import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityAdmissionRequirementsViewResponse;
import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityProgrammesViewResponse;
import com.edurite.institution.universityinfo.service.UniversityInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/universities", "/api/student/universities"})
public class StudentUniversityInfoController {

    private final UniversityInfoService universityInfoService;

    public StudentUniversityInfoController(UniversityInfoService universityInfoService) {
        this.universityInfoService = universityInfoService;
    }

    @GetMapping("/{slug}/programmes")
    public UniversityProgrammesViewResponse programmes(@PathVariable String slug) {
        return universityInfoService.getProgrammes(slug);
    }

    @GetMapping("/{slug}/admission-requirements")
    public UniversityAdmissionRequirementsViewResponse admissionRequirements(@PathVariable String slug) {
        return universityInfoService.getAdmissionRequirements(slug);
    }
}
