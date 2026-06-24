package com.edurite.school.controller;

import com.edurite.school.dto.SchoolLinkDtos;
import com.edurite.school.service.MySchoolService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/my-school", "/api/student/my-school"})
public class StudentMySchoolController {

    private final MySchoolService mySchoolService;

    public StudentMySchoolController(MySchoolService mySchoolService) {
        this.mySchoolService = mySchoolService;
    }

    @GetMapping("/status")
    public SchoolLinkDtos.StudentSchoolStatusDto status(Principal principal) {
        return mySchoolService.studentStatus(principal);
    }

    @PostMapping("/request")
    public SchoolLinkDtos.StudentSchoolStatusDto requestJoin(Principal principal, @Valid @RequestBody SchoolLinkDtos.StudentSchoolRequest request) {
        return mySchoolService.requestJoin(principal, request.schoolId());
    }
}
