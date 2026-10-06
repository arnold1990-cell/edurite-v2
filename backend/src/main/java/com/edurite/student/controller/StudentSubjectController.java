package com.edurite.student.controller;

import com.edurite.student.service.StudentSubjectCatalogue;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/student/subjects", "/api/student/subjects"})
public class StudentSubjectController {
    private final StudentSubjectCatalogue catalogue;
    public StudentSubjectController(StudentSubjectCatalogue catalogue) { this.catalogue = catalogue; }
    @GetMapping
    public List<StudentSubjectCatalogue.Subject> subjects() { return catalogue.subjects(); }
}
