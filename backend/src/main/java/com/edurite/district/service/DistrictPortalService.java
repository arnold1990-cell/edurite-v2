package com.edurite.district.service;

import com.edurite.admin.entity.AuditLog;
import com.edurite.admin.repository.AuditLogRepository;
import com.edurite.district.dto.DistrictDtos;
import com.edurite.district.entity.District;
import com.edurite.district.entity.DistrictAnnouncement;
import com.edurite.district.entity.DistrictIntervention;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictAnnouncementRepository;
import com.edurite.district.repository.DistrictInterventionRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.notification.service.NotificationService;
import com.edurite.psychometric.repository.PsychometricSubmissionRepository;
import com.edurite.school.admin.entity.SchoolAnnouncement;
import com.edurite.school.admin.entity.SchoolSupportRequest;
import com.edurite.school.admin.repository.SchoolAnnouncementRepository;
import com.edurite.school.admin.repository.SchoolSupportRequestRepository;
import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.portal.entity.LearningNote;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolIntervention;
import com.edurite.school.portal.entity.SchoolTask;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.entity.TaskSubmission;
import com.edurite.school.portal.repository.LearningNoteRepository;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolInterventionRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolTaskRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.portal.repository.TaskSubmissionRepository;
import com.edurite.school.service.SchoolAccessService;
import com.edurite.school.service.SchoolService;
import com.edurite.student.dto.StudentSubjectAchievementDto;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentProfileCompletionService;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistrictPortalService {

    private static final String EMPTY_AI_MESSAGE = "District AI insights will appear once schools upload learner reports and academic data.";

    private final DistrictRepository districtRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolTaskRepository schoolTaskRepository;
    private final LearningNoteRepository learningNoteRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileCompletionService studentProfileCompletionService;
    private final PsychometricSubmissionRepository psychometricSubmissionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DistrictAdminProfileRepository districtAdminProfileRepository;
    private final DistrictAnnouncementRepository districtAnnouncementRepository;
    private final DistrictInterventionRepository districtInterventionRepository;
    private final SchoolAnnouncementRepository schoolAnnouncementRepository;
    private final SchoolSupportRequestRepository schoolSupportRequestRepository;
    private final SchoolInterventionRepository schoolInterventionRepository;
    private final SchoolService schoolService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public DistrictPortalService(
            DistrictRepository districtRepository,
            SchoolRepository schoolRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolTaskRepository schoolTaskRepository,
            LearningNoteRepository learningNoteRepository,
            TaskSubmissionRepository taskSubmissionRepository,
            StudentProfileRepository studentProfileRepository,
            StudentProfileCompletionService studentProfileCompletionService,
            PsychometricSubmissionRepository psychometricSubmissionRepository,
            SubscriptionRepository subscriptionRepository,
            DistrictAdminProfileRepository districtAdminProfileRepository,
            DistrictAnnouncementRepository districtAnnouncementRepository,
            DistrictInterventionRepository districtInterventionRepository,
            SchoolAnnouncementRepository schoolAnnouncementRepository,
            SchoolSupportRequestRepository schoolSupportRequestRepository,
            SchoolInterventionRepository schoolInterventionRepository,
            SchoolService schoolService,
            UserRepository userRepository,
            NotificationService notificationService,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper
    ) {
        this.districtRepository = districtRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolTaskRepository = schoolTaskRepository;
        this.learningNoteRepository = learningNoteRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentProfileCompletionService = studentProfileCompletionService;
        this.psychometricSubmissionRepository = psychometricSubmissionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.districtAdminProfileRepository = districtAdminProfileRepository;
        this.districtAnnouncementRepository = districtAnnouncementRepository;
        this.districtInterventionRepository = districtInterventionRepository;
        this.schoolAnnouncementRepository = schoolAnnouncementRepository;
        this.schoolSupportRequestRepository = schoolSupportRequestRepository;
        this.schoolInterventionRepository = schoolInterventionRepository;
        this.schoolService = schoolService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DistrictDtos.DistrictDashboardResponse dashboard(UUID districtId, UUID viewerUserId) {
        DistrictContext context = loadContext(districtId);
        District district = context.district();

        long totalSchools = context.schools().size();
        long totalLearners = context.learnerProfiles().size();
        long totalTeachers = countProfiles(context.teacherProfiles());
        long activeClasses = context.activeClasses();
        long reportsUploaded = context.totalReportArtifacts();
        long schoolsPendingReports = context.schoolSummaries().stream().filter(summary -> !"Complete".equals(summary.reportUploadStatus())).count();
        BigDecimal averageAps = averageAps(context.learnerProfiles());
        long learnersAtRisk = context.learnerProfiles().stream().filter(this::isAtRisk).count();
        long subjectGapAlerts = subjectGapBreakdown(context.learnerProfiles()).values().stream().mapToLong(Long::longValue).sum();
        long aiUsage = context.learnerProfiles().stream().filter(profile -> psychometricSubmissionRepository.existsByStudentId(profile.getUserId())).count();
        long interventionCases = context.schoolInterventions().size() + context.districtInterventions().size();
        String licensingStatus = district.getLicensingStatus();

        return new DistrictDtos.DistrictDashboardResponse(
                district.getDistrictName(),
                district.getDistrictCode(),
                district.getProvince(),
                licensingStatus,
                totalSchools + " schools monitored with " + learnersAtRisk + " learners at risk and " + schoolsPendingReports + " schools still pending full reporting.",
                List.of(
                        metric("Total schools", totalSchools, "Schools assigned to this district", "neutral"),
                        metric("Total learners", totalLearners, "Consent-aware district learner records", "neutral"),
                        metric("Total teachers", totalTeachers, "Teachers linked through schools", "neutral"),
                        metric("Active classes", activeClasses, "Current live class structures", "neutral"),
                        metric("Reports uploaded", reportsUploaded, "Notes and learner submissions", reportsUploaded > 0 ? "positive" : "warning"),
                        metric("Schools pending reports", schoolsPendingReports, "Schools still missing reporting artefacts", schoolsPendingReports > 0 ? "warning" : "positive"),
                        metric("Average APS score", averageAps.toPlainString(), "Across learners with academic data", averageAps.compareTo(new BigDecimal("28")) >= 0 ? "positive" : "warning"),
                        metric("Learners at risk", learnersAtRisk, "Aggregated risk signals across schools", learnersAtRisk > 0 ? "warning" : "positive"),
                        metric("Subject gap alerts", subjectGapAlerts, "Weak subject combinations observed", subjectGapAlerts > 0 ? "warning" : "positive"),
                        metric("AI usage across schools", aiUsage, "Learners with AI-linked evidence", aiUsage > 0 ? "positive" : "neutral"),
                        metric("Intervention cases", interventionCases, "School and district interventions", interventionCases > 0 ? "warning" : "neutral"),
                        metric("Subscription / licensing status", licensingStatus, "District subscription posture", "neutral")
                ),
                districtPerformanceTrends(context),
                riskDistribution(context.learnerProfiles()),
                reportUploadProgress(context),
                topSchoolRanking(context.schoolSummaries()),
                urgentInterventionInsights(context),
                aiHighlights(context),
                recentSupportRequests(context),
                recentAnnouncements(context)
        );
    }

    @Transactional(readOnly = true)
    public DistrictDtos.DistrictSchoolsResponse schools(UUID districtId, String search, String riskLevel, String complianceStatus) {
        DistrictContext context = loadContext(districtId);
        List<DistrictDtos.DistrictSchoolSummaryDto> items = context.schoolSummaries().stream()
                .filter(item -> matchesText(item.schoolName(), search) || matchesText(item.district(), search))
                .filter(item -> riskLevel == null || riskLevel.isBlank() || item.riskLevel().equalsIgnoreCase(riskLevel))
                .filter(item -> complianceStatus == null || complianceStatus.isBlank() || item.complianceStatus().equalsIgnoreCase(complianceStatus))
                .toList();
        return new DistrictDtos.DistrictSchoolsResponse(
                List.of(
                        metric("Schools", context.schools().size(), "Assigned schools", "neutral"),
                        metric("Learners", context.learnerProfiles().size(), "Visible learner records", "neutral"),
                        metric("Teachers", countProfiles(context.teacherProfiles()), "Active teacher profiles", "neutral"),
                        metric("Pending reports", context.schoolSummaries().stream().filter(item -> !"Complete".equals(item.reportUploadStatus())).count(), "Schools needing uploads", "warning")
                ),
                items,
                items.size()
        );
    }

    @Transactional
    public DistrictDtos.DistrictSchoolDetailResponse schoolDetail(UUID districtId, UUID schoolId, UUID viewerUserId) {
        DistrictContext context = loadContext(districtId);
        School school = context.schoolMap().get(schoolId);
        if (school == null) {
            throw new IllegalArgumentException("School not found in district scope.");
        }
        writeAudit(viewerUserId, "DISTRICT_SCHOOL_DRILLDOWN_VIEW", schoolId, Map.of("districtId", districtId, "schoolId", schoolId));
        SchoolSnapshot snapshot = snapshotForSchool(context, school);

        return new DistrictDtos.DistrictSchoolDetailResponse(
                school.getId(),
                school.getSchoolName(),
                school.getRegistrationNumber(),
                school.getDistrict(),
                school.getProvince(),
                school.getContactEmail(),
                school.getContactPhone(),
                school.getAddress(),
                List.of(
                        metric("Learner statistics", snapshot.rawLearnerProfileCount(), "Learners assigned to school", "neutral"),
                        metric("Teacher statistics", snapshot.teacherProfiles().size(), "Teachers assigned to school", "neutral"),
                        metric("Subject performance", averageAps(snapshot.learners()).toPlainString(), "APS-linked performance baseline", averageAps(snapshot.learners()).compareTo(new BigDecimal("28")) >= 0 ? "positive" : "warning"),
                        metric("APS readiness", readinessLabel(averageAps(snapshot.learners())), "School readiness summary", readinessTone(averageAps(snapshot.learners()))),
                        metric("Report uploads", snapshot.reportArtifacts(), "Notes and submissions uploaded", snapshot.reportArtifacts() > 0 ? "positive" : "warning"),
                        metric("Learners at risk", snapshot.learners().stream().filter(this::isAtRisk).count(), "Intervention signals", snapshot.learners().stream().anyMatch(this::isAtRisk) ? "warning" : "positive"),
                        metric("Career readiness", careerReadinessPercent(snapshot.learners()) + "%", "Learners with declared pathways", careerReadinessPercent(snapshot.learners()) >= 60 ? "positive" : "warning"),
                        metric("Teacher activity", snapshot.teacherActivitySummary(), "Active teacher engagement score", "neutral")
                ),
                subjectPerformance(snapshot.learners()),
                apsDistribution(snapshot.learners()),
                List.of(
                        new DistrictDtos.DistributionItemDto("Teacher notes", snapshot.notes().size(), snapshot.notes().isEmpty() ? "warning" : "positive"),
                        new DistrictDtos.DistributionItemDto("Task submissions", snapshot.submissions().size(), snapshot.submissions().isEmpty() ? "warning" : "positive"),
                        new DistrictDtos.DistributionItemDto("School announcements", snapshot.schoolAnnouncements().size(), snapshot.schoolAnnouncements().isEmpty() ? "neutral" : "positive")
                ),
                List.of(
                        new DistrictDtos.InsightItemDto("Teacher activity", snapshot.teacherActivitySummary(), "neutral"),
                        new DistrictDtos.InsightItemDto("Career readiness", careerReadinessPercent(snapshot.learners()) + "% of learners have pathway data", careerReadinessPercent(snapshot.learners()) >= 60 ? "positive" : "warning"),
                        new DistrictDtos.InsightItemDto("School compliance", schoolCompliance(snapshot), schoolCompliance(snapshot).equals("Compliant") ? "positive" : "warning")
                ),
                aiRecommendations(snapshot),
                "school-comparison-report"
        );
    }

    @Transactional(readOnly = true)
    public DistrictDtos.DistrictSchoolAnalyticsResponse schoolAnalytics(UUID districtId, UUID schoolId) {
        DistrictContext context = loadContext(districtId);
        School school = context.schoolMap().get(schoolId);
        if (school == null) {
            throw new IllegalArgumentException("School not found in district scope.");
        }
        SchoolSnapshot snapshot = snapshotForSchool(context, school);
        return new DistrictDtos.DistrictSchoolAnalyticsResponse(
                school.getId(),
                school.getSchoolName(),
                subjectPerformance(snapshot.learners()),
                subjectPerformance(snapshot.learners()),
                gradeComparison(snapshot),
                careerPathwayDistribution(snapshot.learners()),
                aiRecommendations(snapshot)
        );
    }

    @Transactional(readOnly = true)
    public DistrictDtos.DistrictAnalyticsResponse analytics(UUID districtId) {
        DistrictContext context = loadContext(districtId);
        return new DistrictDtos.DistrictAnalyticsResponse(
                districtPerformanceTrends(context),
                topSchoolRanking(context.schoolSummaries()),
                subjectPerformance(context.learnerProfiles()),
                apsDistribution(context.learnerProfiles()),
                careerPathwayDistribution(context.learnerProfiles()),
                riskDistribution(context.learnerProfiles()),
                reportUploadProgress(context),
                gradeComparisonAcrossDistrict(context),
                urgentInterventionInsights(context)
        );
    }

    @Transactional(readOnly = true)
    public DistrictDtos.DistrictAiInsightsResponse aiInsights(UUID districtId) {
        DistrictContext context = loadContext(districtId);
        boolean dataAvailable = !context.learnerProfiles().isEmpty() && context.totalReportArtifacts() > 0;
        return new DistrictDtos.DistrictAiInsightsResponse(
                dataAvailable,
                dataAvailable ? null : EMPTY_AI_MESSAGE,
                urgentInterventionInsights(context),
                context.schoolSummaries().stream()
                        .filter(item -> !"Low".equalsIgnoreCase(item.riskLevel()))
                        .map(item -> new DistrictDtos.InsightItemDto(item.schoolName(), item.learnerCount() + " learners, risk level " + item.riskLevel(), "warning"))
                        .limit(8)
                        .toList(),
                subjectGapBreakdown(context.learnerProfiles()).entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(8)
                        .map(entry -> new DistrictDtos.InsightItemDto(entry.getKey(), entry.getValue() + " learner signals", "warning"))
                        .toList(),
                context.schoolSummaries().stream()
                        .filter(item -> !"On Track".equalsIgnoreCase(item.apsReadiness()))
                        .map(item -> new DistrictDtos.InsightItemDto(item.schoolName(), "APS readiness " + item.apsReadiness(), "warning"))
                        .limit(8)
                        .toList(),
                careerPathwayDistribution(context.learnerProfiles()).stream()
                        .filter(item -> item.value() < 2)
                        .map(item -> new DistrictDtos.InsightItemDto(item.label(), "Low representation across the district", "neutral"))
                        .limit(6)
                        .toList(),
                context.schoolSummaries().stream()
                        .filter(item -> item.averageApsScore().compareTo(new BigDecimal("28")) < 0)
                        .map(item -> new DistrictDtos.InsightItemDto(item.schoolName(), "Funding-readiness indicators suggest targeted support", "warning"))
                        .limit(6)
                        .toList(),
                context.schoolSummaries().stream()
                        .filter(item -> item.teacherCount() == 0 || item.activeClasses() == 0)
                        .map(item -> new DistrictDtos.InsightItemDto(item.schoolName(), "Teacher activity is below expected baseline", "warning"))
                        .limit(6)
                        .toList(),
                aiHighlights(context)
        );
    }

    @Transactional(readOnly = true)
    public List<DistrictDtos.ReportItemDto> reports() {
        return List.of(
                new DistrictDtos.ReportItemDto("district-performance-report", "District performance report", "District-wide APS, readiness, and risk summary.", true, true),
                new DistrictDtos.ReportItemDto("school-comparison-report", "School comparison report", "School-by-school comparison across core performance indicators.", true, true),
                new DistrictDtos.ReportItemDto("learner-readiness-report", "Learner readiness report", "Aggregated readiness posture and intervention counts.", true, true),
                new DistrictDtos.ReportItemDto("subject-gap-report", "Subject gap report", "Weak subject patterns affecting readiness.", true, true),
                new DistrictDtos.ReportItemDto("teacher-activity-report", "Teacher activity report", "Teacher coverage and activity baseline.", true, true),
                new DistrictDtos.ReportItemDto("aps-readiness-report", "APS readiness report", "APS band distribution and readiness warnings.", true, true),
                new DistrictDtos.ReportItemDto("career-pathway-report", "Career pathway report", "Career pathway distribution across schools.", true, true),
                new DistrictDtos.ReportItemDto("intervention-report", "Intervention report", "School and district intervention workload.", true, true)
        );
    }

    @Transactional
    public SchoolPortalDtos.ReportExportResponse exportReport(UUID districtId, UUID actorUserId, DistrictDtos.ReportExportRequest request) {
        DistrictContext context = loadContext(districtId);
        String normalizedType = normalize(request.type());
        String normalizedFormat = normalize(request.format());

        List<String> lines = new ArrayList<>();
        lines.add("District,Schools,Learners,Teachers,Average APS,Learners At Risk,Reports Uploaded");
        lines.add(csv(context.district().getDistrictName()) + "," + context.schools().size() + "," + context.learnerProfiles().size() + "," + countProfiles(context.teacherProfiles()) + "," + averageAps(context.learnerProfiles()) + "," + context.learnerProfiles().stream().filter(this::isAtRisk).count() + "," + context.totalReportArtifacts());
        lines.add("");
        lines.add("School,Teachers,Learners,Active Classes,Average APS,Risk Level,Compliance");
        context.schoolSummaries().forEach(item -> lines.add(csv(item.schoolName()) + "," + item.teacherCount() + "," + item.learnerCount() + "," + item.activeClasses() + "," + item.averageApsScore() + "," + csv(item.riskLevel()) + "," + csv(item.complianceStatus())));

        String fileBase = normalizedType == null ? "district-report" : normalizedType;
        String extension = "pdf".equals(normalizedFormat) ? "pdf" : "xlsx".equals(normalizedFormat) ? "xlsx" : "csv";
        String content = String.join("\n", lines);
        writeAudit(actorUserId, "DISTRICT_REPORT_EXPORT", districtId, Map.of("type", request.type(), "format", request.format()));
        return new SchoolPortalDtos.ReportExportResponse(
                fileBase + "." + extension,
                "pdf".equals(normalizedFormat) ? "application/pdf" : "xlsx".equals(normalizedFormat) ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "text/csv",
                Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Transactional
    public DistrictDtos.AnnouncementItemDto createAnnouncement(UUID districtId, UUID actorUserId, DistrictDtos.AnnouncementCreateRequest request) {
        DistrictContext context = loadContext(districtId);
        validateSchoolScope(context.schoolMap(), request.schoolId(), request.deliveryScope());

        DistrictAnnouncement announcement = new DistrictAnnouncement();
        announcement.setDistrictId(districtId);
        announcement.setSchoolId(request.schoolId());
        announcement.setCreatedByUserId(actorUserId);
        announcement.setAudience(request.audience().trim().toUpperCase(Locale.ROOT));
        announcement.setTitle(request.title().trim());
        announcement.setMessage(request.message().trim());
        announcement.setDeliveryScope(request.deliveryScope() == null || request.deliveryScope().isBlank() ? "ALL_SCHOOLS" : request.deliveryScope().trim().toUpperCase(Locale.ROOT));
        announcement.setStatus("SENT");
        announcement.setSentAt(OffsetDateTime.now());
        DistrictAnnouncement saved = districtAnnouncementRepository.save(announcement);

        resolveAnnouncementRecipients(context, saved).forEach(userId ->
                notificationService.createInApp(userId, "DISTRICT_ANNOUNCEMENT", saved.getTitle(), saved.getMessage())
        );
        writeAudit(actorUserId, "DISTRICT_ANNOUNCEMENT_SENT", saved.getId(), Map.of("districtId", districtId, "schoolId", request.schoolId(), "audience", saved.getAudience()));
        return toAnnouncementDto(saved, context.schoolMap());
    }

    @Transactional(readOnly = true)
    public DistrictDtos.DistrictInterventionsResponse interventions(UUID districtId) {
        DistrictContext context = loadContext(districtId);
        List<DistrictDtos.DistrictInterventionItemDto> items = context.districtInterventions().stream()
                .map(item -> toInterventionDto(item, context.schoolMap()))
                .toList();
        return new DistrictDtos.DistrictInterventionsResponse(
                "District interventions track school-level support, escalation demand, and district response planning.",
                List.of(
                        metric("District interventions", items.size(), "Cases opened by district officials", items.isEmpty() ? "neutral" : "warning"),
                        metric("School interventions", context.schoolInterventions().size(), "Linked school intervention records", context.schoolInterventions().isEmpty() ? "neutral" : "warning"),
                        metric("Open support requests", context.supportRequests().size(), "School support queue", context.supportRequests().isEmpty() ? "positive" : "warning")
                ),
                interventionTypeInsights(items),
                items
        );
    }

    @Transactional
    public DistrictDtos.DistrictInterventionItemDto createIntervention(UUID districtId, UUID actorUserId, DistrictDtos.DistrictInterventionCreateRequest request) {
        DistrictContext context = loadContext(districtId);
        validateSchoolScope(context.schoolMap(), request.schoolId(), request.targetScope());

        DistrictIntervention intervention = new DistrictIntervention();
        intervention.setDistrictId(districtId);
        intervention.setSchoolId(request.schoolId());
        intervention.setCreatedByUserId(actorUserId);
        intervention.setTitle(request.title().trim());
        intervention.setCategory(request.category().trim());
        intervention.setPriority(request.priority().trim().toUpperCase(Locale.ROOT));
        intervention.setNotes(request.notes().trim());
        intervention.setTargetScope(request.targetScope() == null || request.targetScope().isBlank() ? "DISTRICT" : request.targetScope().trim().toUpperCase(Locale.ROOT));
        intervention.setFollowUpDate(request.followUpDate());
        DistrictIntervention saved = districtInterventionRepository.save(intervention);
        writeAudit(actorUserId, "DISTRICT_INTERVENTION_CREATED", saved.getId(), Map.of("districtId", districtId, "schoolId", request.schoolId(), "category", saved.getCategory()));
        return toInterventionDto(saved, context.schoolMap());
    }

    @Transactional(readOnly = true)
    public DistrictDtos.DistrictSettingsResponse settings(UUID districtId) {
        DistrictContext context = loadContext(districtId);
        District district = context.district();
        return new DistrictDtos.DistrictSettingsResponse(
                district.getDistrictName(),
                district.getDistrictCode(),
                district.getProvince(),
                district.getContactEmail(),
                district.getContactPhone(),
                district.getLicensingStatus(),
                List.of(DistrictAccessService.ROLE_DISTRICT_ADMIN, SchoolAccessService.ROLE_SCHOOL_ADMIN, SchoolAccessService.ROLE_TEACHER, SchoolAccessService.ROLE_SCHOOL_STUDENT),
                context.schools().stream()
                        .map(school -> schoolService.portalSettings(school.getId()))
                        .toList(),
                context.supportRequests().stream().map(item -> toSupportRequestDto(item, context.schoolMap())).toList(),
                context.districtAnnouncements().stream().map(item -> toAnnouncementDto(item, context.schoolMap())).toList(),
                districtAdminAuditLogs(districtId).stream()
                        .limit(20)
                        .map(log -> new DistrictDtos.AuditItemDto(log.getAction(), log.getEntityType(), log.getEntityId(), log.getCreatedAt()))
                        .toList()
        );
    }

    private DistrictContext loadContext(UUID districtId) {
        District district = districtRepository.findById(districtId).orElseThrow();
        List<School> schools = schoolRepository.findByDistrictIdOrderBySchoolNameAsc(districtId);
        List<UUID> schoolIds = schools.stream().map(School::getId).toList();
        Map<UUID, School> schoolMap = schools.stream().collect(Collectors.toMap(School::getId, Function.identity()));
        List<SchoolUserProfile> allSchoolProfiles = schoolIds.isEmpty() ? List.of() : schoolUserProfileRepository.findBySchoolIdInAndDeletedFalse(schoolIds);
        List<SchoolUserProfile> teacherProfiles = allSchoolProfiles.stream().filter(item -> SchoolAccessService.ROLE_TEACHER.equals(item.getRoleName())).toList();
        List<SchoolUserProfile> learnerProfiles = schoolIds.isEmpty() ? List.of() : schoolUserProfileRepository.findBySchoolIdInAndRoleNameAndDeletedFalse(schoolIds, SchoolAccessService.ROLE_SCHOOL_STUDENT);
        List<SchoolUserProfile> consentedLearnerProfiles = learnerProfiles.stream().filter(this::isConsentCaptured).toList();
        Map<UUID, StudentProfile> learnerProfileMap = studentProfileRepository.findAll().stream()
                .filter(profile -> consentedLearnerProfiles.stream().anyMatch(item -> item.getUserId().equals(profile.getUserId())))
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
        List<SchoolTask> tasks = schoolTaskRepository.findAll().stream().filter(task -> schoolMap.containsKey(task.getSchoolId())).toList();
        Set<UUID> taskIds = tasks.stream().map(SchoolTask::getId).collect(Collectors.toSet());
        List<TaskSubmission> submissions = taskSubmissionRepository.findAll().stream().filter(item -> taskIds.contains(item.getTaskId())).toList();
        List<LearningNote> notes = learningNoteRepository.findAll().stream().filter(item -> schoolMap.containsKey(item.getSchoolId())).toList();
        List<SchoolIntervention> schoolInterventions = schoolIds.isEmpty() ? List.of() : schoolInterventionRepository.findBySchoolIdInAndActiveTrue(schoolIds);
        List<SchoolSupportRequest> supportRequests = schoolIds.isEmpty() ? List.of() : schoolSupportRequestRepository.findBySchoolIdInAndActiveTrueOrderByCreatedAtDesc(schoolIds);
        List<SchoolAnnouncement> schoolAnnouncements = schoolAnnouncementRepository.findAll().stream().filter(item -> schoolMap.containsKey(item.getSchoolId())).toList();
        List<DistrictIntervention> districtInterventions = districtInterventionRepository.findByDistrictIdAndActiveTrueOrderByCreatedAtDesc(districtId);
        List<DistrictAnnouncement> districtAnnouncements = districtAnnouncementRepository.findByDistrictIdAndActiveTrueOrderByCreatedAtDesc(districtId);

        List<DistrictDtos.DistrictSchoolSummaryDto> summaries = schools.stream()
                .map(school -> summarizeSchool(school, teacherProfiles, learnerProfiles, learnerProfileMap, tasks, submissions, notes))
                .toList();

        long activeClasses = schoolIds.isEmpty() ? 0 : schoolIds.stream().mapToLong(id -> schoolClassRepository.findBySchoolIdAndActiveTrue(id).size()).sum();

        return new DistrictContext(
                district,
                schools,
                schoolMap,
                allSchoolProfiles,
                teacherProfiles,
                learnerProfiles,
                learnerProfileMap,
                tasks,
                submissions,
                notes,
                schoolInterventions,
                supportRequests,
                schoolAnnouncements,
                districtInterventions,
                districtAnnouncements,
                summaries,
                activeClasses
        );
    }

    private DistrictDtos.DistrictSchoolSummaryDto summarizeSchool(
            School school,
            List<SchoolUserProfile> teacherProfiles,
            List<SchoolUserProfile> learnerProfiles,
            Map<UUID, StudentProfile> learnerProfileMap,
            List<SchoolTask> tasks,
            List<TaskSubmission> submissions,
            List<LearningNote> notes
    ) {
        List<SchoolUserProfile> schoolTeachers = teacherProfiles.stream().filter(item -> school.getId().equals(item.getSchoolId())).toList();
        List<StudentProfile> schoolLearners = learnerProfiles.stream()
                .filter(item -> school.getId().equals(item.getSchoolId()))
                .map(item -> learnerProfileMap.get(item.getUserId()))
                .filter(java.util.Objects::nonNull)
                .toList();
        List<SchoolUserProfile> schoolLearnerProfiles = learnerProfiles.stream()
                .filter(item -> school.getId().equals(item.getSchoolId()))
                .toList();
        long activeClasses = schoolClassRepository.findBySchoolIdAndActiveTrue(school.getId()).size();
        long schoolTasks = tasks.stream().filter(item -> school.getId().equals(item.getSchoolId())).count();
        long schoolSubmissions = submissions.stream().filter(item -> tasks.stream().anyMatch(task -> task.getId().equals(item.getTaskId()) && school.getId().equals(task.getSchoolId()))).count();
        long schoolNotes = notes.stream().filter(item -> school.getId().equals(item.getSchoolId())).count();
        BigDecimal averageAps = averageAps(schoolLearners);
        long atRisk = schoolLearners.stream().filter(this::isAtRisk).count();
        String reportStatus = schoolSubmissions + schoolNotes >= Math.max(1, schoolTasks) ? "Complete" : schoolSubmissions + schoolNotes > 0 ? "In Progress" : "Pending";
        boolean consentCoverageComplete = !schoolLearnerProfiles.isEmpty() && schoolLearnerProfiles.stream().allMatch(this::isConsentCaptured);
        boolean profileCoverageComplete = !schoolLearners.isEmpty()
                && schoolLearners.size() == schoolLearnerProfiles.size()
                && schoolLearners.stream().allMatch(studentProfileCompletionService::isProfileCompleted);
        String complianceStatus = consentCoverageComplete && profileCoverageComplete ? "Compliant" : "Review Needed";

        return new DistrictDtos.DistrictSchoolSummaryDto(
                school.getId(),
                school.getSchoolName(),
                school.getRegistrationNumber(),
                school.getDistrict(),
                school.getProvince(),
                schoolLearnerProfiles.size(),
                schoolTeachers.size(),
                activeClasses,
                reportStatus,
                averageAps,
                averageAps.compareTo(new BigDecimal("30")) >= 0 ? "Strong" : averageAps.compareTo(new BigDecimal("24")) >= 0 ? "Stable" : "Needs Support",
                readinessLabel(averageAps),
                atRisk == 0 ? "Low" : atRisk < 5 ? "Medium" : "High",
                complianceStatus
        );
    }

    private SchoolSnapshot snapshotForSchool(DistrictContext context, School school) {
        List<SchoolUserProfile> teacherProfiles = context.teacherProfiles().stream().filter(item -> school.getId().equals(item.getSchoolId())).toList();
        long rawLearnerProfileCount = context.learnerProfilesRaw().stream().filter(item -> school.getId().equals(item.getSchoolId())).count();
        List<StudentProfile> learners = context.learnerProfilesRaw().stream()
                .filter(item -> school.getId().equals(item.getSchoolId()))
                .map(item -> context.learnerProfilesByUserId().get(item.getUserId()))
                .filter(java.util.Objects::nonNull)
                .toList();
        List<SchoolTask> tasks = context.tasks().stream().filter(item -> school.getId().equals(item.getSchoolId())).toList();
        Set<UUID> taskIds = tasks.stream().map(SchoolTask::getId).collect(Collectors.toSet());
        List<TaskSubmission> submissions = context.submissions().stream().filter(item -> taskIds.contains(item.getTaskId())).toList();
        List<LearningNote> notes = context.notes().stream().filter(item -> school.getId().equals(item.getSchoolId())).toList();
        List<SchoolAnnouncement> schoolAnnouncements = context.schoolAnnouncements().stream().filter(item -> school.getId().equals(item.getSchoolId())).toList();
        return new SchoolSnapshot(school, teacherProfiles, rawLearnerProfileCount, learners, tasks, submissions, notes, schoolAnnouncements);
    }

    private List<DistrictDtos.TrendPointDto> districtPerformanceTrends(DistrictContext context) {
        return context.schoolSummaries().stream()
                .sorted(Comparator.comparing(DistrictDtos.DistrictSchoolSummaryDto::schoolName))
                .map(item -> new DistrictDtos.TrendPointDto(item.schoolName(), item.averageApsScore(), readinessTone(item.averageApsScore())))
                .toList();
    }

    private List<DistrictDtos.InsightItemDto> topSchoolRanking(List<DistrictDtos.DistrictSchoolSummaryDto> items) {
        return items.stream()
                .sorted(Comparator.comparing(DistrictDtos.DistrictSchoolSummaryDto::averageApsScore).reversed())
                .limit(6)
                .map(item -> new DistrictDtos.InsightItemDto(item.schoolName(), "APS " + item.averageApsScore() + " | " + item.performanceSummary(), item.averageApsScore().compareTo(new BigDecimal("28")) >= 0 ? "positive" : "warning"))
                .toList();
    }

    private List<DistrictDtos.InsightItemDto> urgentInterventionInsights(DistrictContext context) {
        return context.schoolSummaries().stream()
                .filter(item -> "High".equalsIgnoreCase(item.riskLevel()) || !"Complete".equalsIgnoreCase(item.reportUploadStatus()))
                .sorted(Comparator.comparing(DistrictDtos.DistrictSchoolSummaryDto::riskLevel).reversed())
                .limit(8)
                .map(item -> new DistrictDtos.InsightItemDto(item.schoolName(), item.riskLevel() + " risk | reports " + item.reportUploadStatus(), "warning"))
                .toList();
    }

    private List<DistrictDtos.InsightItemDto> aiHighlights(DistrictContext context) {
        List<DistrictDtos.InsightItemDto> highlights = new ArrayList<>();
        highlights.addAll(urgentInterventionInsights(context).stream().limit(3).toList());
        subjectGapBreakdown(context.learnerProfiles()).entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> highlights.add(new DistrictDtos.InsightItemDto(entry.getKey(), entry.getValue() + " learners show this subject gap", "warning")));
        return highlights;
    }

    private List<DistrictDtos.DistributionItemDto> riskDistribution(Collection<StudentProfile> learners) {
        long high = learners.stream().filter(this::isAtRisk).count();
        long medium = learners.stream().filter(profile -> !isAtRisk(profile) && apsScore(profile) < 28).count();
        long low = Math.max(0, learners.size() - high - medium);
        return List.of(
                new DistrictDtos.DistributionItemDto("High risk", high, high > 0 ? "warning" : "positive"),
                new DistrictDtos.DistributionItemDto("Moderate risk", medium, medium > 0 ? "warning" : "neutral"),
                new DistrictDtos.DistributionItemDto("Low risk", low, "positive")
        );
    }

    private List<DistrictDtos.DistributionItemDto> reportUploadProgress(DistrictContext context) {
        long complete = context.schoolSummaries().stream().filter(item -> "Complete".equals(item.reportUploadStatus())).count();
        long inProgress = context.schoolSummaries().stream().filter(item -> "In Progress".equals(item.reportUploadStatus())).count();
        long pending = context.schoolSummaries().stream().filter(item -> "Pending".equals(item.reportUploadStatus())).count();
        return List.of(
                new DistrictDtos.DistributionItemDto("Complete", complete, complete > 0 ? "positive" : "neutral"),
                new DistrictDtos.DistributionItemDto("In progress", inProgress, inProgress > 0 ? "warning" : "neutral"),
                new DistrictDtos.DistributionItemDto("Pending", pending, pending > 0 ? "warning" : "positive")
        );
    }

    private List<DistrictDtos.TrendPointDto> subjectPerformance(Collection<StudentProfile> learners) {
        Map<String, List<Integer>> subjectScores = new LinkedHashMap<>();
        learners.forEach(profile -> parseAchievements(profile).forEach(item -> {
            if (item.subjectName() != null && item.achievementLevel() != null) {
                subjectScores.computeIfAbsent(item.subjectName(), ignored -> new ArrayList<>()).add(item.achievementLevel());
            }
        }));
        return subjectScores.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(8)
                .map(entry -> new DistrictDtos.TrendPointDto(entry.getKey(), BigDecimal.valueOf(entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0)).setScale(1, RoundingMode.HALF_UP), entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0) >= 4 ? "positive" : "warning"))
                .toList();
    }

    private List<DistrictDtos.DistributionItemDto> apsDistribution(Collection<StudentProfile> learners) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("0-19", 0L);
        counts.put("20-27", 0L);
        counts.put("28-34", 0L);
        counts.put("35+", 0L);
        learners.forEach(profile -> {
            long aps = apsScore(profile);
            String band = aps < 20 ? "0-19" : aps < 28 ? "20-27" : aps < 35 ? "28-34" : "35+";
            counts.put(band, counts.get(band) + 1);
        });
        return counts.entrySet().stream()
                .map(entry -> new DistrictDtos.DistributionItemDto(entry.getKey(), entry.getValue(), "0-19".equals(entry.getKey()) || "20-27".equals(entry.getKey()) ? "warning" : "positive"))
                .toList();
    }

    private List<DistrictDtos.DistributionItemDto> careerPathwayDistribution(Collection<StudentProfile> learners) {
        Map<String, Long> counts = learners.stream()
                .map(StudentProfile::getCareerGoals)
                .map(this::clean)
                .filter(value -> value != null)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        if (counts.isEmpty()) {
            return List.of(new DistrictDtos.DistributionItemDto("No pathway data", 0, "warning"));
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(entry -> new DistrictDtos.DistributionItemDto(entry.getKey(), entry.getValue(), "neutral"))
                .toList();
    }

    private List<DistrictDtos.DistributionItemDto> gradeComparison(SchoolSnapshot snapshot) {
        Map<String, Long> counts = snapshot.learners().stream()
                .map(StudentProfile::getSelectedGrade)
                .map(this::clean)
                .filter(value -> value != null)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> new DistrictDtos.DistributionItemDto(entry.getKey(), entry.getValue(), "neutral"))
                .toList();
    }

    private List<DistrictDtos.DistributionItemDto> gradeComparisonAcrossDistrict(DistrictContext context) {
        Map<String, Long> counts = context.learnerProfiles().stream()
                .map(StudentProfile::getSelectedGrade)
                .map(this::clean)
                .filter(value -> value != null)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> new DistrictDtos.DistributionItemDto(entry.getKey(), entry.getValue(), "neutral"))
                .toList();
    }

    private Map<String, Long> subjectGapBreakdown(Collection<StudentProfile> learners) {
        Map<String, Long> counts = new LinkedHashMap<>();
        learners.forEach(profile -> parseAchievements(profile).stream()
                .filter(item -> item.subjectName() != null && item.achievementLevel() != null && item.achievementLevel() <= 3)
                .forEach(item -> counts.merge(item.subjectName(), 1L, Long::sum)));
        return counts;
    }

    private List<DistrictDtos.InsightItemDto> aiRecommendations(SchoolSnapshot snapshot) {
        List<DistrictDtos.InsightItemDto> items = new ArrayList<>();
        long atRisk = snapshot.learners().stream().filter(this::isAtRisk).count();
        if (atRisk > 0) {
            items.add(new DistrictDtos.InsightItemDto("Targeted learner support", atRisk + " learners require intervention sequencing.", "warning"));
        }
        if (snapshot.reportArtifacts() == 0) {
            items.add(new DistrictDtos.InsightItemDto("Reporting gap", "School has not uploaded enough academic evidence yet.", "warning"));
        }
        if (snapshot.teacherProfiles().isEmpty()) {
            items.add(new DistrictDtos.InsightItemDto("Teacher coverage alert", "No active teacher profiles are linked to this school.", "critical"));
        }
        if (items.isEmpty()) {
            items.add(new DistrictDtos.InsightItemDto("District recommendation", "Maintain current support cadence and continue report uploads.", "positive"));
        }
        return items;
    }

    private List<DistrictDtos.SupportRequestItemDto> recentSupportRequests(DistrictContext context) {
        return context.supportRequests().stream().limit(6).map(item -> toSupportRequestDto(item, context.schoolMap())).toList();
    }

    private List<DistrictDtos.AnnouncementItemDto> recentAnnouncements(DistrictContext context) {
        return context.districtAnnouncements().stream().limit(6).map(item -> toAnnouncementDto(item, context.schoolMap())).toList();
    }

    private DistrictDtos.SupportRequestItemDto toSupportRequestDto(SchoolSupportRequest item, Map<UUID, School> schoolMap) {
        School school = schoolMap.get(item.getSchoolId());
        return new DistrictDtos.SupportRequestItemDto(
                item.getId(),
                item.getSchoolId(),
                school == null ? "Unknown school" : school.getSchoolName(),
                item.getCategory(),
                item.getTitle(),
                item.getMessage(),
                item.getStatus(),
                item.getPriority(),
                item.getCreatedAt()
        );
    }

    private DistrictDtos.AnnouncementItemDto toAnnouncementDto(DistrictAnnouncement item, Map<UUID, School> schoolMap) {
        School school = item.getSchoolId() == null ? null : schoolMap.get(item.getSchoolId());
        return new DistrictDtos.AnnouncementItemDto(
                item.getId(),
                item.getAudience(),
                item.getTitle(),
                item.getMessage(),
                item.getDeliveryScope(),
                item.getSchoolId(),
                school == null ? null : school.getSchoolName(),
                item.getStatus(),
                item.getSentAt(),
                item.getCreatedAt()
        );
    }

    private DistrictDtos.DistrictInterventionItemDto toInterventionDto(DistrictIntervention item, Map<UUID, School> schoolMap) {
        School school = item.getSchoolId() == null ? null : schoolMap.get(item.getSchoolId());
        return new DistrictDtos.DistrictInterventionItemDto(
                item.getId(),
                item.getTitle(),
                item.getCategory(),
                item.getPriority(),
                item.getStatus(),
                item.getTargetScope(),
                item.getSchoolId(),
                school == null ? null : school.getSchoolName(),
                item.getNotes(),
                item.getFollowUpDate(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private List<DistrictDtos.InsightItemDto> interventionTypeInsights(List<DistrictDtos.DistrictInterventionItemDto> items) {
        Map<String, Long> counts = items.stream()
                .collect(Collectors.groupingBy(DistrictDtos.DistrictInterventionItemDto::category, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> new DistrictDtos.InsightItemDto(entry.getKey(), entry.getValue() + " district cases", "warning"))
                .toList();
    }

    private void validateSchoolScope(Map<UUID, School> schoolMap, UUID schoolId, String scope) {
        if (schoolId != null && !schoolMap.containsKey(schoolId)) {
            throw new IllegalArgumentException("School is outside district scope.");
        }
        String normalizedScope = normalize(scope);
        if ("selected_school".equals(normalizedScope) && schoolId == null) {
            throw new IllegalArgumentException("Selected school scope requires a schoolId.");
        }
    }

    private List<UUID> resolveAnnouncementRecipients(DistrictContext context, DistrictAnnouncement announcement) {
        return context.allSchoolProfiles().stream()
                .filter(profile -> {
                    if (announcement.getSchoolId() != null && !announcement.getSchoolId().equals(profile.getSchoolId())) {
                        return false;
                    }
                    return switch (announcement.getAudience()) {
                        case "SCHOOL_ADMINS" -> SchoolAccessService.ROLE_SCHOOL_ADMIN.equals(profile.getRoleName());
                        case "TEACHERS" -> SchoolAccessService.ROLE_TEACHER.equals(profile.getRoleName());
                        default -> true;
                    };
                })
                .map(SchoolUserProfile::getUserId)
                .distinct()
                .toList();
    }

    private BigDecimal averageAps(Collection<StudentProfile> learners) {
        if (learners.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(learners.stream().mapToLong(this::apsScore).average().orElse(0)).setScale(1, RoundingMode.HALF_UP);
    }

    private long apsScore(StudentProfile profile) {
        return parseAchievements(profile).stream()
                .map(StudentSubjectAchievementDto::achievementLevel)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private boolean isAtRisk(StudentProfile profile) {
        return apsScore(profile) < 24 || !studentProfileCompletionService.isProfileCompleted(profile);
    }

    private String readinessLabel(BigDecimal aps) {
        if (aps.compareTo(new BigDecimal("28")) >= 0) {
            return "On Track";
        }
        if (aps.compareTo(new BigDecimal("22")) >= 0) {
            return "Watchlist";
        }
        return "Critical";
    }

    private String readinessTone(BigDecimal aps) {
        return aps.compareTo(new BigDecimal("28")) >= 0 ? "positive" : aps.compareTo(new BigDecimal("22")) >= 0 ? "warning" : "critical";
    }

    private long careerReadinessPercent(Collection<StudentProfile> learners) {
        if (learners.isEmpty()) {
            return 0;
        }
        long mapped = learners.stream().map(StudentProfile::getCareerGoals).map(this::clean).filter(value -> value != null).count();
        return Math.round((mapped * 100.0) / learners.size());
    }

    private String schoolCompliance(SchoolSnapshot snapshot) {
        return snapshot.learners().stream().allMatch(studentProfileCompletionService::isProfileCompleted) && !snapshot.learners().isEmpty()
                ? "Compliant"
                : "Review Needed";
    }

    private long countProfiles(List<SchoolUserProfile> profiles) {
        return profiles.stream().filter(SchoolUserProfile::isActive).count();
    }

    private List<StudentSubjectAchievementDto> parseAchievements(StudentProfile profile) {
        try {
            return objectMapper.readValue(profile.getSubjectAchievementsJson(), new TypeReference<List<StudentSubjectAchievementDto>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private DistrictDtos.MetricCardDto metric(String label, long value, String helperText, String tone) {
        return new DistrictDtos.MetricCardDto(label, String.valueOf(value), helperText, tone);
    }

    private DistrictDtos.MetricCardDto metric(String label, String value, String helperText, String tone) {
        return new DistrictDtos.MetricCardDto(label, value, helperText, tone);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalize(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private boolean isConsentCaptured(SchoolUserProfile profile) {
        return "consent captured".equals(normalize(profile.getConsentStatus()));
    }

    private boolean matchesText(String value, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT));
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private void writeAudit(UUID actorId, String action, UUID entityId, Map<String, Object> details) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setAction(action);
        log.setEntityType("DISTRICT_ADMIN");
        log.setEntityId(entityId);
        log.setDetails(objectMapper.valueToTree(details));
        auditLogRepository.save(log);
    }

    private List<AuditLog> districtAdminAuditLogs(UUID districtId) {
        List<UUID> actorIds = districtAdminProfileRepository.findByDistrictIdAndActiveTrueAndDeletedFalse(districtId).stream()
                .map(profile -> profile.getUserId())
                .toList();
        if (actorIds.isEmpty()) {
            return List.of();
        }
        return auditLogRepository.findTop100ByActorIdInOrderByCreatedAtDesc(actorIds).stream()
                .filter(log -> "DISTRICT_ADMIN".equalsIgnoreCase(log.getEntityType()))
                .toList();
    }

    private record DistrictContext(
            District district,
            List<School> schools,
            Map<UUID, School> schoolMap,
            List<SchoolUserProfile> allSchoolProfiles,
            List<SchoolUserProfile> teacherProfiles,
            List<SchoolUserProfile> learnerProfilesRaw,
            Map<UUID, StudentProfile> learnerProfilesByUserId,
            List<SchoolTask> tasks,
            List<TaskSubmission> submissions,
            List<LearningNote> notes,
            List<SchoolIntervention> schoolInterventions,
            List<SchoolSupportRequest> supportRequests,
            List<SchoolAnnouncement> schoolAnnouncements,
            List<DistrictIntervention> districtInterventions,
            List<DistrictAnnouncement> districtAnnouncements,
            List<DistrictDtos.DistrictSchoolSummaryDto> schoolSummaries,
            long activeClasses
    ) {
        Collection<StudentProfile> learnerProfiles() {
            return learnerProfilesByUserId.values();
        }

        long totalReportArtifacts() {
            return notes.size() + submissions.size();
        }
    }

    private record SchoolSnapshot(
            School school,
            List<SchoolUserProfile> teacherProfiles,
            long rawLearnerProfileCount,
            List<StudentProfile> learners,
            List<SchoolTask> tasks,
            List<TaskSubmission> submissions,
            List<LearningNote> notes,
            List<SchoolAnnouncement> schoolAnnouncements
    ) {
        long reportArtifacts() {
            return notes.size() + submissions.size();
        }

        String teacherActivitySummary() {
            if (teacherProfiles.isEmpty()) {
                return "No active teacher profiles";
            }
            return teacherProfiles.size() + " teachers covering " + tasks.size() + " tasks and " + notes.size() + " note uploads.";
        }
    }
}
