package com.edurite.school.portal.controller;

import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.service.SchoolAccessService;
import com.edurite.school.service.SchoolService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/school", "/api/school"})
public class SchoolAdminPortalController {

    private final SchoolAccessService schoolAccessService;
    private final SchoolService schoolService;

    public SchoolAdminPortalController(SchoolAccessService schoolAccessService, SchoolService schoolService) {
        this.schoolAccessService = schoolAccessService;
        this.schoolService = schoolService;
    }

    @GetMapping("/dashboard")
    public SchoolPortalDtos.DashboardResponse dashboard(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.dashboard(context.schoolId(), context.userId());
    }

    @PutMapping("/profile")
    public Object upsertProfile(Principal principal, @Valid @RequestBody SchoolPortalDtos.SchoolProfileUpsertRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.upsertSchool(context.schoolId(), request);
    }

    @PostMapping("/classes")
    public Object createClass(Principal principal, @Valid @RequestBody SchoolPortalDtos.SchoolClassRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.createClass(context.schoolId(), request);
    }

    @GetMapping("/classes")
    public Object classes(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.classes(context.schoolId());
    }

    @PutMapping("/classes/{classId}")
    public Object updateClass(Principal principal, @PathVariable UUID classId, @Valid @RequestBody SchoolPortalDtos.SchoolClassUpdateRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.updateClass(context.schoolId(), classId, request);
    }

    @PatchMapping("/classes/{classId}/deactivate")
    public void deactivateClass(Principal principal, @PathVariable UUID classId) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        schoolService.deactivateClass(context.schoolId(), classId);
    }

    @PostMapping("/subjects")
    public Object createSubject(Principal principal, @Valid @RequestBody SchoolPortalDtos.SchoolSubjectRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.createSubject(context.schoolId(), request);
    }

    @GetMapping("/subjects")
    public Object subjects(
            Principal principal,
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
            @RequestParam(value = "view", required = false) String view
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        if ("management".equalsIgnoreCase(view)) {
            return schoolService.subjectViews(context.schoolId(), includeInactive);
        }
        return schoolService.subjects(context.schoolId());
    }

    @GetMapping("/subjects/catalogue")
    public Object subjectCatalogue(Principal principal) {
        schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.subjectCatalogue();
    }

    @GetMapping("/subjects/summary")
    public SchoolPortalDtos.SubjectManagementSummary subjectSummary(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.subjectSummary(context.schoolId());
    }

    @GetMapping("/atp-topics")
    public List<SchoolPortalDtos.AtpTopicView> atpTopics(
            Principal principal,
            @RequestParam(value = "phase", required = false) String phase,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "subjectId", required = false) UUID subjectId,
            @RequestParam(value = "term", required = false) String term
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.atpTopics(context.schoolId(), phase, grade, subjectId, term);
    }

    @PutMapping("/subjects/{subjectId}")
    public Object updateSubject(Principal principal, @PathVariable UUID subjectId, @Valid @RequestBody SchoolPortalDtos.SchoolSubjectUpdateRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.updateSubject(context.schoolId(), subjectId, request);
    }

    @PatchMapping("/subjects/{subjectId}/deactivate")
    public void deactivateSubject(Principal principal, @PathVariable UUID subjectId) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        schoolService.deactivateSubject(context.schoolId(), subjectId);
    }

    @PostMapping("/teacher-assignments")
    public Object assignTeacher(Principal principal, @Valid @RequestBody SchoolPortalDtos.TeacherAssignmentRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.assignTeacher(context.schoolId(), request);
    }

    @GetMapping("/teacher-assignments")
    public Object teacherAssignments(Principal principal, @RequestParam(value = "teacherUserId", required = false) UUID teacherUserId) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        if (teacherUserId != null) {
            return schoolService.teacherAssignmentViews(context.schoolId(), teacherUserId);
        }
        return schoolService.teacherAssignments(context.schoolId());
    }

    @PutMapping("/teachers/{teacherUserId}/assignments")
    public List<SchoolPortalDtos.TeacherAssignmentView> replaceTeacherAssignments(
            Principal principal,
            @PathVariable UUID teacherUserId,
            @Valid @RequestBody SchoolPortalDtos.TeacherAssignmentBulkRequest request
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.replaceTeacherAssignments(context.schoolId(), teacherUserId, request);
    }

    @PostMapping("/learner-enrollments")
    public Object enrollLearner(Principal principal, @Valid @RequestBody SchoolPortalDtos.LearnerEnrollmentRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.enrollLearner(context.schoolId(), request);
    }

    @GetMapping("/learner-enrollments")
    public Object learnerEnrollments(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.learnerEnrollments(context.schoolId());
    }

    @GetMapping("/teachers")
    public Object teachers(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.teachers(context.schoolId());
    }

    @GetMapping("/learners")
    public Object learners(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.learners(context.schoolId());
    }

    @PostMapping("/users")
    public Object createUser(Principal principal, @Valid @RequestBody SchoolPortalDtos.SchoolUserCreateRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.createSchoolUser(context.schoolId(), request);
    }

    @PostMapping("/learners/bulk-upload")
    public SchoolPortalDtos.BulkLearnerUploadResult bulkUploadLearners(Principal principal, @RequestPart("file") MultipartFile file) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.bulkUploadLearners(context.schoolId(), file);
    }

    @PutMapping("/users/{userId}")
    public Object updateUser(Principal principal, @PathVariable UUID userId, @Valid @RequestBody SchoolPortalDtos.SchoolUserUpdateRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.updateSchoolUser(context.schoolId(), userId, request);
    }

    @DeleteMapping("/users/{userId}")
    public void deactivateUser(Principal principal, @PathVariable UUID userId) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        schoolService.deactivateSchoolUser(context.schoolId(), userId);
    }

    @GetMapping("/tasks")
    public Object tasks(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.tasks(context.schoolId());
    }

    @PostMapping("/tasks")
    public Object createTask(Principal principal, @Valid @RequestBody SchoolPortalDtos.SchoolTaskRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.createTask(context.schoolId(), request);
    }

    @GetMapping("/assessments")
    public Object assessments(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.assessments(context.schoolId());
    }

    @GetMapping("/submissions")
    public Object submissions(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return new SchoolPortalDtos.CollectionResponse<>(schoolService.submissions(context.schoolId()));
    }

    @GetMapping("/results")
    public Object results(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return new SchoolPortalDtos.CollectionResponse<>(schoolService.results(context.schoolId()));
    }

    @GetMapping("/notes")
    public Object notes(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.notes(context.schoolId());
    }

    @GetMapping("/progress")
    public SchoolPortalDtos.ProgressSummaryResponse progress(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.progress(context.schoolId(), context.userId());
    }

    @GetMapping("/portal/dashboard")
    public SchoolPortalDtos.DashboardSnapshot portalDashboard(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.portalDashboard(context.schoolId(), context.userId(), context.roleName());
    }

    @GetMapping("/portal/learners")
    public SchoolPortalDtos.LearnerListResponse portalLearners(
            Principal principal,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "className", required = false) String className
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.portalLearners(context.schoolId(), context.userId(), context.roleName(), search, grade, className);
    }

    @GetMapping("/portal/learners/{learnerUserId}")
    public SchoolPortalDtos.LearnerProfileResponse learnerProfile(Principal principal, @PathVariable UUID learnerUserId) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.portalLearnerProfile(context.schoolId(), context.userId(), context.roleName(), learnerUserId);
    }

    @GetMapping("/portal/academic-insights")
    public SchoolPortalDtos.AcademicInsightsResponse academicInsights(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.academicInsights(context.schoolId(), context.userId(), context.roleName());
    }

    @GetMapping("/portal/career-readiness")
    public SchoolPortalDtos.CareerReadinessResponse careerReadiness(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.careerReadiness(context.schoolId(), context.userId(), context.roleName());
    }

    @GetMapping("/portal/bursary-readiness")
    public SchoolPortalDtos.BursaryReadinessResponse bursaryReadiness(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.bursaryReadiness(context.schoolId(), context.userId(), context.roleName());
    }

    @GetMapping("/portal/interventions")
    public Object interventions(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return new SchoolPortalDtos.CollectionResponse<>(schoolService.interventions(context.schoolId(), context.userId(), context.roleName()));
    }

    @PostMapping("/portal/interventions")
    public SchoolPortalDtos.InterventionReportItem createIntervention(Principal principal, @Valid @RequestBody SchoolPortalDtos.InterventionRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.createIntervention(context.schoolId(), context.userId(), context.roleName(), request);
    }

    @PatchMapping("/portal/interventions/{interventionId}")
    public SchoolPortalDtos.InterventionReportItem updateIntervention(Principal principal, @PathVariable UUID interventionId, @Valid @RequestBody SchoolPortalDtos.InterventionProgressRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.updateIntervention(context.schoolId(), context.userId(), context.roleName(), interventionId, request);
    }

    @GetMapping("/portal/reports/export")
    public SchoolPortalDtos.ReportExportResponse exportReport(
            Principal principal,
            @RequestParam("type") String type,
            @RequestParam(value = "format", defaultValue = "csv") String format
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.exportReport(context.schoolId(), context.userId(), context.roleName(), type, format);
    }

    @GetMapping("/portal/settings")
    public SchoolPortalDtos.PortalSettingsResponse settings(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return schoolService.portalSettings(context.schoolId());
    }
}



