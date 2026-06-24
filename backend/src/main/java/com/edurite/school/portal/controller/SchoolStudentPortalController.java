package com.edurite.school.portal.controller;

import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.service.SchoolAccessService;
import com.edurite.school.service.StudentSchoolService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/school-student", "/api/school-student"})
public class SchoolStudentPortalController {

    private final SchoolAccessService schoolAccessService;
    private final StudentSchoolService studentSchoolService;

    public SchoolStudentPortalController(SchoolAccessService schoolAccessService, StudentSchoolService studentSchoolService) {
        this.schoolAccessService = schoolAccessService;
        this.studentSchoolService = studentSchoolService;
    }

    @GetMapping("/dashboard")
    public SchoolPortalDtos.DashboardResponse dashboard(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT));
        return studentSchoolService.dashboard(context.schoolId(), context.userId());
    }

    @GetMapping("/notes")
    public Object notes(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT));
        return studentSchoolService.notes(context.schoolId(), context.userId());
    }

    @GetMapping("/subjects")
    public List<SchoolPortalDtos.LearnerSubjectView> subjects(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT));
        return studentSchoolService.subjects(context.schoolId(), context.userId());
    }

    @GetMapping("/tasks")
    public Object tasks(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT));
        return studentSchoolService.tasks(context.schoolId(), context.userId());
    }

    @GetMapping("/assessments")
    public List<SchoolPortalDtos.StudentTaskView> assessments(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT));
        return studentSchoolService.assessments(context.schoolId(), context.userId());
    }

    @PostMapping("/submissions")
    public Object submit(Principal principal, @Valid @RequestBody SchoolPortalDtos.TaskSubmissionRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT));
        return studentSchoolService.submit(context.schoolId(), context.userId(), request);
    }

    @GetMapping("/submissions")
    public SchoolPortalDtos.CollectionResponse<SchoolPortalDtos.SubmissionView> submissions(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT));
        return new SchoolPortalDtos.CollectionResponse<>(studentSchoolService.submissions(context.schoolId(), context.userId()));
    }

    @GetMapping("/progress")
    public SchoolPortalDtos.ProgressSummaryResponse progress(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT));
        return studentSchoolService.progress(context.schoolId(), context.userId());
    }

    @GetMapping("/marks")
    public SchoolPortalDtos.CollectionResponse<SchoolPortalDtos.SubmissionView> marks(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT));
        return new SchoolPortalDtos.CollectionResponse<>(studentSchoolService.marks(context.schoolId(), context.userId()));
    }
}



