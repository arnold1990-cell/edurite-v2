package com.edurite.school.portal.controller;

import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.service.SchoolAccessService;
import com.edurite.school.service.TeacherService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/teacher", "/api/teacher"})
public class TeacherPortalController {

    private final SchoolAccessService schoolAccessService;
    private final TeacherService teacherService;

    public TeacherPortalController(SchoolAccessService schoolAccessService, TeacherService teacherService) {
        this.schoolAccessService = schoolAccessService;
        this.teacherService = teacherService;
    }

    @GetMapping("/dashboard")
    public SchoolPortalDtos.DashboardResponse dashboard(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.dashboard(context.schoolId(), context.userId());
    }

    @PostMapping("/notes")
    public Object createNote(Principal principal, @Valid @RequestBody SchoolPortalDtos.LearningNoteRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.createNote(context.schoolId(), context.userId(), request);
    }

    @PostMapping("/tasks")
    public Object createTask(Principal principal, @Valid @RequestBody SchoolPortalDtos.SchoolTaskRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.createTask(context.schoolId(), context.userId(), request);
    }

    @GetMapping("/atp-topics")
    public List<SchoolPortalDtos.AtpTopicView> atpTopics(
            Principal principal,
            @RequestParam(value = "phase", required = false) String phase,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "subjectId", required = false) UUID subjectId,
            @RequestParam(value = "term", required = false) String term
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.atpTopics(context.schoolId(), phase, grade, subjectId, term);
    }

    @GetMapping("/tasks")
    public Object tasks(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.tasks(context.schoolId(), context.userId());
    }

    @GetMapping("/tasks/{taskId}/submissions")
    public SchoolPortalDtos.CollectionResponse<SchoolPortalDtos.SubmissionView> submissions(Principal principal, @PathVariable UUID taskId) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return new SchoolPortalDtos.CollectionResponse<>(teacherService.taskSubmissions(context.schoolId(), context.userId(), taskId));
    }

    @PostMapping("/submissions/{submissionId}/mark")
    public Object mark(Principal principal, @PathVariable UUID submissionId, @Valid @RequestBody SchoolPortalDtos.MarkSubmissionRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.markSubmission(context.schoolId(), context.userId(), submissionId, request);
    }

    @GetMapping("/progress")
    public SchoolPortalDtos.ProgressSummaryResponse progress(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.progress(context.schoolId(), context.userId());
    }

    @GetMapping("/classes")
    public List<SchoolPortalDtos.TeacherClassView> classes(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.classes(context.schoolId(), context.userId());
    }

    @GetMapping("/subjects")
    public List<SchoolPortalDtos.TeacherSubjectView> subjects(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.subjects(context.schoolId(), context.userId());
    }

    @GetMapping("/assignments")
    public Object assignments(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.tasks(context.schoolId(), context.userId());
    }

    @GetMapping("/assessments")
    public Object assessments(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.assessments(context.schoolId(), context.userId());
    }

    @GetMapping("/submissions")
    public SchoolPortalDtos.CollectionResponse<SchoolPortalDtos.SubmissionView> submissionsAll(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        List<SchoolPortalDtos.SubmissionView> all = teacherService.tasks(context.schoolId(), context.userId()).stream()
                .flatMap(task -> teacherService.taskSubmissions(context.schoolId(), context.userId(), task.getId()).stream())
                .toList();
        return new SchoolPortalDtos.CollectionResponse<>(all);
    }

    @GetMapping("/analytics")
    public SchoolPortalDtos.TeacherAnalyticsResponse analytics(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.analytics(context.schoolId(), context.userId());
    }

    @GetMapping("/calendar")
    public SchoolPortalDtos.CollectionResponse<SchoolPortalDtos.TeacherCalendarItem> calendar(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return new SchoolPortalDtos.CollectionResponse<>(teacherService.calendar(context.schoolId(), context.userId()));
    }

    @GetMapping("/activity")
    public SchoolPortalDtos.CollectionResponse<SchoolPortalDtos.TeacherActivityItem> activity(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return new SchoolPortalDtos.CollectionResponse<>(teacherService.activity(context.schoolId(), context.userId()));
    }

    @GetMapping("/search")
    public SchoolPortalDtos.CollectionResponse<String> search(Principal principal, @RequestParam("q") String query) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        String keyword = query == null ? "" : query.trim().toLowerCase();
        List<String> matches = teacherService.tasks(context.schoolId(), context.userId()).stream()
                .map(com.edurite.school.portal.entity.SchoolTask::getTitle)
                .filter(title -> keyword.isBlank() || title.toLowerCase().contains(keyword))
                .limit(10)
                .toList();
        return new SchoolPortalDtos.CollectionResponse<>(matches);
    }
    @GetMapping("/portal/dashboard")
    public SchoolPortalDtos.DashboardSnapshot portalDashboard(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.portalDashboard(context.schoolId(), context.userId(), context.roleName());
    }

    @GetMapping("/portal/learners")
    public SchoolPortalDtos.LearnerListResponse portalLearners(
            Principal principal,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "className", required = false) String className
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.portalLearners(context.schoolId(), context.userId(), context.roleName(), search, grade, className);
    }

    @GetMapping("/portal/learners/{learnerUserId}")
    public SchoolPortalDtos.LearnerProfileResponse learnerProfile(Principal principal, @PathVariable UUID learnerUserId) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.portalLearnerProfile(context.schoolId(), context.userId(), context.roleName(), learnerUserId);
    }

    @GetMapping("/portal/academic-insights")
    public SchoolPortalDtos.AcademicInsightsResponse academicInsights(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.academicInsights(context.schoolId(), context.userId(), context.roleName());
    }

    @GetMapping("/portal/career-readiness")
    public SchoolPortalDtos.CareerReadinessResponse careerReadiness(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.careerReadiness(context.schoolId(), context.userId(), context.roleName());
    }

    @GetMapping("/portal/bursary-readiness")
    public SchoolPortalDtos.BursaryReadinessResponse bursaryReadiness(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.bursaryReadiness(context.schoolId(), context.userId(), context.roleName());
    }

    @GetMapping("/portal/interventions")
    public SchoolPortalDtos.CollectionResponse<SchoolPortalDtos.InterventionReportItem> interventions(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return new SchoolPortalDtos.CollectionResponse<>(teacherService.interventions(context.schoolId(), context.userId(), context.roleName()));
    }

    @PostMapping("/portal/interventions")
    public SchoolPortalDtos.InterventionReportItem createIntervention(Principal principal, @Valid @RequestBody SchoolPortalDtos.InterventionRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.createIntervention(context.schoolId(), context.userId(), context.roleName(), request);
    }

    @GetMapping("/portal/reports/export")
    public SchoolPortalDtos.ReportExportResponse exportReport(
            Principal principal,
            @RequestParam("type") String type,
            @RequestParam(value = "format", defaultValue = "csv") String format
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.exportReport(context.schoolId(), context.userId(), context.roleName(), type, format);
    }

    @GetMapping("/portal/settings")
    public SchoolPortalDtos.PortalSettingsResponse settings(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return teacherService.portalSettings(context.schoolId());
    }
}



