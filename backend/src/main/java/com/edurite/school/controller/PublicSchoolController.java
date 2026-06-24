package com.edurite.school.controller;

import com.edurite.school.dto.SchoolLinkDtos;
import com.edurite.school.service.MySchoolService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/public/schools", "/api/public/schools"})
public class PublicSchoolController {

    private final MySchoolService mySchoolService;

    public PublicSchoolController(MySchoolService mySchoolService) {
        this.mySchoolService = mySchoolService;
    }

    @GetMapping
    public List<SchoolLinkDtos.PublicSchoolDto> schools() {
        return mySchoolService.listActiveSchools();
    }
}
