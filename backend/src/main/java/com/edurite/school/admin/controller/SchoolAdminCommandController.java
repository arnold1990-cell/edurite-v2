package com.edurite.school.admin.controller;

import com.edurite.school.admin.dto.SchoolAdminDtos;
import com.edurite.school.admin.service.SchoolAdminCommandService;
import com.edurite.notification.dto.NotificationDtos;
import com.edurite.notification.service.NotificationService;
import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.service.SchoolAccessService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/school-admin", "/api/school-admin"})
public class SchoolAdminCommandController {

    private final SchoolAccessService schoolAccessService;
    private final SchoolAdminCommandService schoolAdminCommandService;
    private final NotificationService notificationService;

    public SchoolAdminCommandController(
            SchoolAccessService schoolAccessService,
            SchoolAdminCommandService schoolAdminCommandService,
            NotificationService notificationService
    ) {
        this.schoolAccessService = schoolAccessService;
        this.schoolAdminCommandService = schoolAdminCommandService;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    public SchoolAdminDtos.SchoolAdminDashboardResponse dashboard(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.dashboard(context.schoolId(), context.userId());
    }

    @GetMapping("/analytics")
    public SchoolAdminDtos.SchoolAdminAnalyticsResponse analytics(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.analytics(context.schoolId(), context.userId());
    }

    @GetMapping("/ai-insights")
    public SchoolAdminDtos.SchoolAdminAiInsightsResponse aiInsights(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.aiInsights(context.schoolId(), context.userId());
    }

    @GetMapping("/learners")
    public SchoolAdminDtos.LearnerAdminListResponse learners(
            Principal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String className
    ) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.learners(context.schoolId(), context.userId(), search, grade, className);
    }

    @GetMapping("/learners/{id}")
    public SchoolAdminDtos.LearnerAdminProfileResponse learnerProfile(Principal principal, @PathVariable("id") UUID learnerUserId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.learnerProfile(context.schoolId(), context.userId(), learnerUserId);
    }

    @PostMapping("/learners")
    public SchoolAdminDtos.LearnerAdminItemDto createLearner(Principal principal, @Valid @RequestBody SchoolAdminDtos.LearnerCreateRequest request) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.createLearner(context.schoolId(), context.userId(), request);
    }

    @PostMapping("/learners/import")
    public SchoolPortalDtos.BulkLearnerUploadResult importLearners(Principal principal, @RequestPart("file") MultipartFile file) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.importLearners(context.schoolId(), file);
    }

    @GetMapping("/learners/credentials")
    public SchoolAdminDtos.LearnerCredentialsExportDto learnerCredentials(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.learnerCredentials(context.schoolId());
    }

    @GetMapping("/teachers")
    public SchoolAdminDtos.TeacherAdminListResponse teachers(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.teachers(context.schoolId(), context.userId());
    }

    @GetMapping("/classes")
    public SchoolAdminDtos.ClassAdminListResponse classes(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.classes(context.schoolId(), context.userId());
    }

    @GetMapping("/classes/{id}")
    public SchoolAdminDtos.ClassProfileResponse classProfile(Principal principal, @PathVariable("id") UUID classId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.classProfile(context.schoolId(), context.userId(), classId);
    }

    @GetMapping("/classes/{id}/analytics")
    public SchoolAdminDtos.ClassAnalyticsResponse classAnalytics(Principal principal, @PathVariable("id") UUID classId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.classAnalytics(context.schoolId(), context.userId(), classId);
    }

    @GetMapping("/classes/{id}/learners")
    public List<SchoolAdminDtos.LearnerAdminItemDto> classLearners(Principal principal, @PathVariable("id") UUID classId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.classLearners(context.schoolId(), context.userId(), classId);
    }

    @GetMapping("/classes/{id}/career-readiness")
    public SchoolAdminDtos.ClassCareerReadinessResponse classCareerReadiness(Principal principal, @PathVariable("id") UUID classId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.classCareerReadiness(context.schoolId(), context.userId(), classId);
    }

    @GetMapping("/classes/{id}/bursaries")
    public SchoolAdminDtos.ClassBursaryReadinessResponse classBursaries(Principal principal, @PathVariable("id") UUID classId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.classBursaries(context.schoolId(), context.userId(), classId);
    }

    @GetMapping("/classes/{id}/interventions")
    public List<SchoolAdminDtos.SchoolAdminInterventionReportDto> classInterventions(Principal principal, @PathVariable("id") UUID classId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.classInterventions(context.schoolId(), context.userId(), classId);
    }

    @GetMapping("/classes/{id}/ai-insights")
    public SchoolAdminDtos.ClassAiInsightsResponse classAiInsights(Principal principal, @PathVariable("id") UUID classId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.classAiInsights(context.schoolId(), context.userId(), classId);
    }

    @GetMapping("/teachers/{id}")
    public SchoolAdminDtos.TeacherDetailResponse teacherDetail(Principal principal, @PathVariable("id") UUID teacherId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.teacherDetail(context.schoolId(), context.userId(), teacherId);
    }

    @GetMapping("/teachers/analytics")
    public SchoolAdminDtos.TeacherAdminDashboardResponse teacherAnalytics(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.teacherAnalytics(context.schoolId(), context.userId());
    }

    @GetMapping("/teachers/engagement")
    public SchoolAdminDtos.TeacherActivityResponse teacherEngagement(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.teacherEngagement(context.schoolId(), context.userId());
    }

    @GetMapping("/teachers/ai-insights")
    public SchoolAdminDtos.TeacherAiInsightResponse teacherAiInsights(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.teacherAiInsights(context.schoolId(), context.userId());
    }

    @GetMapping("/teachers/workload")
    public SchoolAdminDtos.TeacherWorkloadResponse teacherWorkload(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.teacherWorkload(context.schoolId(), context.userId());
    }

    @GetMapping("/teachers/interventions")
    public List<SchoolAdminDtos.SchoolAdminInterventionReportDto> teacherInterventions(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.teacherInterventions(context.schoolId(), context.userId());
    }

    @GetMapping("/teachers/resources")
    public SchoolAdminDtos.TeacherResourceResponse teacherResources(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.teacherResources(context.schoolId(), context.userId());
    }

    @GetMapping("/teachers/training")
    public SchoolAdminDtos.TeacherTrainingResponse teacherTraining(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.teacherTraining(context.schoolId(), context.userId());
    }

    @GetMapping("/career-readiness")
    public SchoolAdminDtos.SchoolAdminCareerReadinessResponse careerReadiness(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.careerReadiness(context.schoolId(), context.userId());
    }

    @GetMapping("/courses")
    public SchoolAdminDtos.SchoolAdminCoursesResponse courses(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.courses(context.schoolId(), context.userId());
    }

    @GetMapping("/bursaries")
    public SchoolAdminDtos.SchoolAdminBursaryReadinessResponse bursaries(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.bursaries(context.schoolId(), context.userId());
    }

    @GetMapping("/interventions")
    public SchoolAdminDtos.SchoolAdminInterventionsResponse interventions(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.interventions(context.schoolId(), context.userId());
    }

    @PostMapping("/interventions")
    public SchoolAdminDtos.SchoolAdminInterventionReportDto createIntervention(
            Principal principal,
            @Valid @RequestBody SchoolPortalDtos.InterventionRequest request
    ) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.createIntervention(context.schoolId(), context.userId(), request);
    }

    @PatchMapping("/interventions/{id}")
    public SchoolAdminDtos.SchoolAdminInterventionReportDto updateIntervention(
            Principal principal,
            @PathVariable("id") UUID interventionId,
            @Valid @RequestBody SchoolPortalDtos.InterventionProgressRequest request
    ) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.updateIntervention(context.schoolId(), context.userId(), interventionId, request);
    }

    @PostMapping("/teachers/{id}/approve")
    public SchoolAdminDtos.TeacherAdminItemDto approveTeacher(Principal principal, @PathVariable("id") UUID teacherId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.approveTeacher(context.schoolId(), context.userId(), teacherId);
    }

    @PostMapping("/teachers/{id}/reject")
    public SchoolAdminDtos.TeacherAdminItemDto rejectTeacher(Principal principal, @PathVariable("id") UUID teacherId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.rejectTeacher(context.schoolId(), context.userId(), teacherId);
    }

    @PostMapping("/teachers/{id}/suspend")
    public SchoolAdminDtos.TeacherAdminItemDto suspendTeacher(Principal principal, @PathVariable("id") UUID teacherId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.suspendTeacher(context.schoolId(), context.userId(), teacherId);
    }

    @PostMapping("/teachers/{id}/reactivate")
    public SchoolAdminDtos.TeacherAdminItemDto reactivateTeacher(Principal principal, @PathVariable("id") UUID teacherId) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.reactivateTeacher(context.schoolId(), context.userId(), teacherId);
    }

    @DeleteMapping("/teachers/{id}")
    public void deleteTeacher(Principal principal, @PathVariable("id") UUID teacherId) {
        SchoolAccessService.AccessContext context = context(principal);
        schoolAdminCommandService.deleteTeacher(context.schoolId(), context.userId(), teacherId);
    }

    @GetMapping("/reports")
    public List<SchoolAdminDtos.ReportItemDto> reports() {
        return schoolAdminCommandService.reports();
    }

    @PostMapping("/reports/export")
    public SchoolPortalDtos.ReportExportResponse exportReport(Principal principal, @Valid @RequestBody SchoolAdminDtos.ReportExportRequest request) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.exportReport(context.schoolId(), context.userId(), request);
    }

    @GetMapping("/announcements")
    public List<SchoolAdminDtos.AnnouncementItemDto> announcements(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.announcements(context.schoolId());
    }

    @PostMapping("/announcements")
    public SchoolAdminDtos.AnnouncementItemDto createAnnouncement(Principal principal, @Valid @RequestBody SchoolAdminDtos.AnnouncementCreateRequest request) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.createAnnouncement(context.schoolId(), context.userId(), request);
    }

    @GetMapping("/support-requests")
    public List<SchoolAdminDtos.SupportRequestItemDto> supportRequests(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.supportRequests(context.schoolId());
    }

    @PostMapping("/support-requests")
    public SchoolAdminDtos.SupportRequestItemDto createSupportRequest(Principal principal, @Valid @RequestBody SchoolAdminDtos.SupportRequestCreateRequest request) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.createSupportRequest(context.schoolId(), context.userId(), request);
    }

    @GetMapping("/notifications")
    public Page<NotificationDtos.NotificationAssignmentDto> notifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "notificationCreatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        context(principal);
        return notificationService.mine(principal, page, size, sortBy, direction, status, type);
    }

    @GetMapping("/school-settings")
    public SchoolAdminDtos.SchoolSettingsResponse settings(Principal principal) {
        SchoolAccessService.AccessContext context = context(principal);
        return schoolAdminCommandService.settings(context.schoolId());
    }

    @GetMapping("/settings")
    public SchoolAdminDtos.SchoolSettingsResponse settingsAlias(Principal principal) {
        return settings(principal);
    }

    private SchoolAccessService.AccessContext context(Principal principal) {
        return schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
    }
}
