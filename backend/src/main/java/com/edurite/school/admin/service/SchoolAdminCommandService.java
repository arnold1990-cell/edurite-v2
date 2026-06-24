package com.edurite.school.admin.service;

import com.edurite.admin.entity.AuditLog;
import com.edurite.admin.repository.AuditLogRepository;
import com.edurite.psychometric.repository.PsychometricSubmissionRepository;
import com.edurite.district.entity.SupportRequest;
import com.edurite.district.repository.SupportRequestRepository;
import com.edurite.school.admin.dto.SchoolAdminDtos;
import com.edurite.school.admin.entity.SchoolAnnouncement;
import com.edurite.school.admin.entity.SchoolSupportRequest;
import com.edurite.school.admin.repository.SchoolAnnouncementRepository;
import com.edurite.school.admin.repository.SchoolSupportRequestRepository;
import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.portal.entity.LearnerEnrollment;
import com.edurite.school.portal.entity.LearningNote;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolClass;
import com.edurite.school.portal.entity.SchoolSubject;
import com.edurite.school.portal.entity.SchoolTask;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.entity.SubmissionFeedback;
import com.edurite.school.portal.entity.TaskSubmission;
import com.edurite.school.portal.entity.TeacherAssignment;
import com.edurite.school.portal.repository.LearnerEnrollmentRepository;
import com.edurite.school.portal.repository.LearningNoteRepository;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.SchoolTaskRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.portal.repository.SubmissionFeedbackRepository;
import com.edurite.school.portal.repository.TaskSubmissionRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.school.service.SchoolAccessService;
import com.edurite.school.service.SchoolService;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.notification.service.NotificationService;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SchoolAdminCommandService {

    private static final String EMPTY_AI_MESSAGE = "AI insights will appear once learner reports and academic data are available.";

    private final SchoolService schoolService;
    private final SchoolRepository schoolRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolSubjectRepository schoolSubjectRepository;
    private final SchoolTaskRepository schoolTaskRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final SubmissionFeedbackRepository submissionFeedbackRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final LearningNoteRepository learningNoteRepository;
    private final LearnerEnrollmentRepository learnerEnrollmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PsychometricSubmissionRepository psychometricSubmissionRepository;
    private final SchoolAnnouncementRepository schoolAnnouncementRepository;
    private final SchoolSupportRequestRepository schoolSupportRequestRepository;
    private final SupportRequestRepository districtSupportRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public SchoolAdminCommandService(
            SchoolService schoolService,
            SchoolRepository schoolRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolSubjectRepository schoolSubjectRepository,
            SchoolTaskRepository schoolTaskRepository,
            TaskSubmissionRepository taskSubmissionRepository,
            SubmissionFeedbackRepository submissionFeedbackRepository,
            TeacherAssignmentRepository teacherAssignmentRepository,
            LearningNoteRepository learningNoteRepository,
            LearnerEnrollmentRepository learnerEnrollmentRepository,
            SubscriptionRepository subscriptionRepository,
            PsychometricSubmissionRepository psychometricSubmissionRepository,
            SchoolAnnouncementRepository schoolAnnouncementRepository,
            SchoolSupportRequestRepository schoolSupportRequestRepository,
            SupportRequestRepository districtSupportRequestRepository,
            AuditLogRepository auditLogRepository,
            NotificationService notificationService,
            ObjectMapper objectMapper
    ) {
        this.schoolService = schoolService;
        this.schoolRepository = schoolRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolSubjectRepository = schoolSubjectRepository;
        this.schoolTaskRepository = schoolTaskRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.submissionFeedbackRepository = submissionFeedbackRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.learningNoteRepository = learningNoteRepository;
        this.learnerEnrollmentRepository = learnerEnrollmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.psychometricSubmissionRepository = psychometricSubmissionRepository;
        this.schoolAnnouncementRepository = schoolAnnouncementRepository;
        this.schoolSupportRequestRepository = schoolSupportRequestRepository;
        this.districtSupportRequestRepository = districtSupportRequestRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.SchoolAdminDashboardResponse dashboard(UUID schoolId, UUID viewerUserId) {
        School school = schoolRepository.findById(schoolId).orElseThrow();
        SchoolPortalDtos.DashboardSnapshot portalSnapshot = schoolService.portalDashboard(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        SchoolPortalDtos.LearnerListResponse learners = schoolService.portalLearners(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, null, null, null);
        List<SchoolPortalDtos.LearnerListItem> learnerItems = learners.items();
        List<SchoolPortalDtos.SchoolUserAdminView> teachers = schoolService.teachers(schoolId);
        List<SchoolClass> classes = schoolService.classes(schoolId);
        List<SchoolSubject> subjects = schoolService.subjects(schoolId);
        List<SchoolTask> tasks = schoolService.tasks(schoolId);
        List<SchoolTask> assessments = schoolService.assessments(schoolId);
        long reportUploads = learningNoteRepository.findAll().stream().filter(note -> schoolId.equals(note.getSchoolId())).count()
                + taskSubmissionRepository.findAll().stream().filter(submission -> belongsToSchool(submission.getTaskId(), schoolId)).count();
        long pendingTeacherApprovals = teachers.stream().filter(teacher -> "PENDING".equalsIgnoreCase(teacher.status())).count();
        long pendingAssignments = tasks.stream().filter(task -> task.getDueAt() != null && task.getDueAt().isAfter(OffsetDateTime.now())).count();
        BigDecimal assessmentCompletion = completionRate(schoolId, assessments);
        long activeLearnersThisMonth = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, SchoolAccessService.ROLE_SCHOOL_STUDENT).stream()
                .filter(SchoolUserProfile::isActive)
                .filter(profile -> profile.getUpdatedAt() != null && profile.getUpdatedAt().isAfter(OffsetDateTime.now().minusDays(30)))
                .count();
        long completeProfiles = learnerItems.stream().filter(SchoolPortalDtos.LearnerListItem::profileComplete).count();
        long careerMapped = learnerItems.stream().filter(item -> normalize(item.careerGoal()) != null).count();
        long bursaryEligible = learnerItems.stream().filter(SchoolPortalDtos.LearnerListItem::bursaryEligible).count();
        BigDecimal averageAps = learnerItems.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(learnerItems.stream().mapToLong(SchoolPortalDtos.LearnerListItem::apsPoints).average().orElse(0)).setScale(1, RoundingMode.HALF_UP);
        long atRisk = learnerItems.stream().filter(SchoolPortalDtos.LearnerListItem::needsIntervention).count();
        long aiUsage = learnerItems.stream().filter(item -> psychometricSubmissionRepository.existsByStudentId(item.learnerUserId())).count();
        String subscriptionStatus = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(viewerUserId)
                .map(this::describeSubscription)
                .orElse("Not linked");

        SchoolAdminDtos.SchoolAdminAnalyticsResponse analytics = analytics(schoolId, viewerUserId);
        List<SchoolAdminDtos.MetricCardDto> metrics = List.of(
                metric("Total registered learners", learnerItems.size(), "School roll", "neutral"),
                metric("Active learners this month", activeLearnersThisMonth, "Recent learner activity", activeLearnersThisMonth > 0 ? "positive" : "neutral"),
                metric("Report uploads", reportUploads, "Notes and learner submissions", "positive"),
                metric("Learners with complete profiles", completeProfiles, "POPIA-aware learner data ready", completeProfiles == learnerItems.size() && !learnerItems.isEmpty() ? "positive" : "warning"),
                metric("Learners career-mapped", careerMapped, "Career goal recorded", careerMapped > 0 ? "positive" : "warning"),
                metric("Learners needing intervention", atRisk, "Intervention candidates", atRisk > 0 ? "warning" : "positive"),
                metric("Bursary-pathway eligible", bursaryEligible, "Matched to at least one funding path", bursaryEligible > 0 ? "positive" : "neutral"),
                metric("Pending teacher approvals", pendingTeacherApprovals, "Requires admin action", pendingTeacherApprovals > 0 ? "warning" : "positive"),
                metric("Teacher activity summary", teacherActivitySummary(teachers(schoolId, viewerUserId).items()), "Average engagement score", "neutral"),
                metric("Average APS score", averageAps.toPlainString(), "Across visible learners", averageAps.compareTo(new BigDecimal("28")) >= 0 ? "positive" : "warning"),
                metric("Pending assignments", pendingAssignments, "Open upcoming tasks", pendingAssignments > 0 ? "warning" : "neutral"),
                metric("Assessment completion rate", assessmentCompletion + "%", "Released assessment evidence", assessmentCompletion.compareTo(new BigDecimal("70")) >= 0 ? "positive" : "warning"),
                metric("AI usage", aiUsage, "Learners with AI-linked readiness data", aiUsage > 0 ? "positive" : "neutral"),
                metric("Subscription / licensing", subscriptionStatus, "Current school admin account", "neutral")
        );
        return new SchoolAdminDtos.SchoolAdminDashboardResponse(
                school.getSchoolName(),
                "School Admin / Principal",
                "Online",
                subscriptionStatus,
                careerMapped + " learners mapped. " + atRisk + " require subject support. " + bursaryEligible + " qualify for at least one bursary pathway.",
                metrics,
                toInsightItems(portalSnapshot.topCareerInterests(), "selected learners", "positive"),
                toInsightItems(portalSnapshot.topSubjectRiskAreas(), "risk signals", "warning"),
                teacherActivityHighlights(teachers(schoolId, viewerUserId).items()),
                analytics.schoolPerformanceTrends(),
                analytics.subjectPerformance(),
                analytics.apsBandDistribution(),
                analytics.gradePerformanceComparison(),
                reportUploadProgress(schoolId),
                analytics.districtReadyReportingSummary()
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.SchoolAdminAnalyticsResponse analytics(UUID schoolId, UUID viewerUserId) {
        SchoolPortalDtos.AcademicInsightsResponse academic = schoolService.academicInsights(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        SchoolPortalDtos.CareerReadinessResponse career = schoolService.careerReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        SchoolPortalDtos.LearnerListResponse learners = schoolService.portalLearners(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, null, null, null);

        List<SchoolAdminDtos.InsightItemDto> recommendations = learners.items().stream()
                .filter(SchoolPortalDtos.LearnerListItem::needsIntervention)
                .limit(8)
                .map(item -> new SchoolAdminDtos.InsightItemDto(
                        item.learnerName(),
                        "APS " + item.apsPoints() + " with risk flags in " + nullSafe(item.className(), "unassigned class"),
                        "warning"
                ))
                .toList();
        List<SchoolAdminDtos.InsightItemDto> readinessOverview = career.learners().stream()
                .limit(8)
                .map(item -> new SchoolAdminDtos.InsightItemDto(
                        item.learnerName(),
                        nullSafe(item.careerGoal(), "No pathway selected") + " | " + item.readinessGap(),
                        item.aligned() ? "positive" : "warning"
                ))
                .toList();
        return new SchoolAdminDtos.SchoolAdminAnalyticsResponse(
                toTrendPoints(academic.gradePerformance()),
                toTrendPoints(academic.subjectPerformance()),
                apsBands(learners.items()),
                toDistribution(academic.classPerformance()),
                recommendations,
                readinessOverview,
                districtSummary(schoolId, learners.items())
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.SchoolAdminAiInsightsResponse aiInsights(UUID schoolId, UUID viewerUserId) {
        SchoolPortalDtos.LearnerListResponse learners = schoolService.portalLearners(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, null, null, null);
        SchoolPortalDtos.AcademicInsightsResponse academic = schoolService.academicInsights(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        SchoolPortalDtos.CareerReadinessResponse career = schoolService.careerReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        SchoolPortalDtos.BursaryReadinessResponse bursaries = schoolService.bursaryReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        List<SchoolAdminDtos.InsightItemDto> atRisk = learners.items().stream()
                .filter(SchoolPortalDtos.LearnerListItem::needsIntervention)
                .map(item -> new SchoolAdminDtos.InsightItemDto(item.learnerName(), "Needs intervention support in " + nullSafe(item.className(), "unassigned class"), "critical"))
                .toList();
        boolean dataAvailable = !learners.items().isEmpty() && (!academic.subjectPerformance().isEmpty() || !career.learners().isEmpty() || !bursaries.matches().isEmpty());
        return new SchoolAdminDtos.SchoolAdminAiInsightsResponse(
                dataAvailable,
                dataAvailable ? null : EMPTY_AI_MESSAGE,
                atRisk,
                academic.subjectsAffectingCareerEligibility().stream()
                        .map(item -> new SchoolAdminDtos.InsightItemDto(item.label(), item.value() + " learners affected", "warning"))
                        .toList(),
                career.learners().stream()
                        .filter(item -> !item.aligned())
                        .limit(8)
                        .map(item -> new SchoolAdminDtos.InsightItemDto(item.learnerName(), item.readinessGap(), "warning"))
                        .toList(),
                career.learners().stream()
                        .filter(item -> !item.aligned())
                        .limit(8)
                        .map(item -> new SchoolAdminDtos.InsightItemDto(item.learnerName(), item.alternativePathway(), "neutral"))
                        .toList(),
                bursaries.deadlineAlerts().stream()
                        .limit(8)
                        .map(item -> new SchoolAdminDtos.InsightItemDto(item.learnerName(), item.bursaryTitle() + " deadline " + item.deadline(), "warning"))
                        .toList(),
                teachers(schoolId, viewerUserId).items().stream()
                        .filter(item -> item.engagementScore().compareTo(new BigDecimal("45")) < 0)
                        .limit(6)
                        .map(item -> new SchoolAdminDtos.InsightItemDto(item.fullName(), "Engagement score " + item.engagementScore(), "warning"))
                        .toList(),
                buildRecommendedInterventions(learners.items(), career.learners(), bursaries.matches())
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.LearnerAdminListResponse learners(UUID schoolId, UUID viewerUserId, String search, String grade, String className) {
        SchoolPortalDtos.LearnerListResponse learners = schoolService.portalLearners(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, search, grade, className);
        Map<UUID, SchoolUserProfile> profiles = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, SchoolAccessService.ROLE_SCHOOL_STUDENT).stream()
                .collect(Collectors.toMap(SchoolUserProfile::getUserId, profile -> profile));
        List<SchoolAdminDtos.LearnerAdminItemDto> items = learners.items().stream()
                .map(item -> {
                    SchoolUserProfile profile = profiles.get(item.learnerUserId());
                    return new SchoolAdminDtos.LearnerAdminItemDto(
                            item.learnerUserId(),
                            item.learnerName(),
                            profile == null ? synthesizeUsername(item.learnerName()) : nullSafe(profile.getPortalUsername(), synthesizeUsername(item.learnerName())),
                            item.email(),
                            profile == null ? null : profile.getInitialPassword(),
                            item.grade(),
                            item.className(),
                            item.teacherName(),
                            item.apsPoints(),
                            item.profileComplete(),
                            item.needsIntervention(),
                            item.bursaryEligible(),
                            learnerReadinessStatus(item),
                            item.bursaryEligible() ? 1 : 0,
                            profile == null ? null : profile.getGuardianName(),
                            profile == null ? null : profile.getGuardianPhone(),
                            profile == null ? null : profile.getGuardianEmail(),
                            profile == null ? item.popiaStatus() : nullSafe(profile.getConsentStatus(), item.popiaStatus()),
                            profile == null ? "Pending" : nullSafe(profile.getReportUploadStatus(), "Pending"),
                            item.careerGoal(),
                            profile != null && profile.getUpdatedAt() != null ? profile.getUpdatedAt().toString() : null
                    );
                })
                .toList();
        return new SchoolAdminDtos.LearnerAdminListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.LearnerAdminProfileResponse learnerProfile(UUID schoolId, UUID viewerUserId, UUID learnerUserId) {
        SchoolPortalDtos.LearnerProfileResponse profile = schoolService.portalLearnerProfile(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, learnerUserId);
        SchoolUserProfile schoolProfile = schoolUserProfileRepository.findBySchoolIdAndUserIdAndDeletedFalse(schoolId, learnerUserId).orElse(null);
        List<SchoolPortalDtos.CareerReadinessLearnerView> readinessItems = schoolService.careerReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN).learners();
        SchoolPortalDtos.CareerReadinessLearnerView readiness = readinessItems.stream()
                .filter(item -> learnerUserId.equals(item.learnerUserId()))
                .findFirst()
                .orElse(null);
        List<SchoolAdminDtos.SchoolPortalCourseDto> qualifiedCourses = profile.courseEligibility().stream()
                .filter(SchoolPortalDtos.MatchedCourseView::eligible)
                .map(item -> new SchoolAdminDtos.SchoolPortalCourseDto(item.name(), item.level(), true, item.reason()))
                .toList();
        List<SchoolAdminDtos.SchoolPortalCourseDto> closeCourses = profile.courseEligibility().stream()
                .filter(item -> !item.eligible())
                .map(item -> new SchoolAdminDtos.SchoolPortalCourseDto(item.name(), item.level(), false, item.reason()))
                .limit(8)
                .toList();
        List<SchoolAdminDtos.SchoolPortalBursaryDto> bursaryMatches = profile.bursaryMatches().stream()
                .map(item -> new SchoolAdminDtos.SchoolPortalBursaryDto(item.title(), item.provider(), item.deadline() == null ? null : item.deadline().toString(), item.eligible(), item.missingRequirements()))
                .toList();
        List<SchoolAdminDtos.LearnerRequirementDto> missingRequirements = new ArrayList<>();
        closeCourses.stream().limit(4).forEach(item -> missingRequirements.add(new SchoolAdminDtos.LearnerRequirementDto(item.name(), item.reason(), "warning")));
        bursaryMatches.stream()
                .filter(item -> normalize(item.missingRequirements()) != null)
                .limit(4)
                .forEach(item -> missingRequirements.add(new SchoolAdminDtos.LearnerRequirementDto(item.title(), item.missingRequirements(), "warning")));
        return new SchoolAdminDtos.LearnerAdminProfileResponse(
                profile.learnerUserId(),
                profile.learnerName(),
                profile.email(),
                profile.grade(),
                profile.className(),
                profile.teacherName(),
                profile.profileComplete(),
                profile.apsPoints(),
                profile.careerGoal(),
                readiness == null ? learnerReadinessStatus(profile.apsPoints(), profile.profileComplete(), !profile.interventions().isEmpty()) : readiness.aligned() ? "Aligned" : "Needs support",
                schoolProfile == null ? "Pending" : nullSafe(schoolProfile.getReportUploadStatus(), "Pending"),
                schoolProfile == null ? profile.popiaStatus() : nullSafe(schoolProfile.getConsentStatus(), profile.popiaStatus()),
                profile.qualificationLevel(),
                profile.interests(),
                profile.skills(),
                profile.subjects().stream()
                        .map(item -> new SchoolAdminDtos.LearnerRequirementDto(item.subjectName(), item.risk() ? "Below recommended performance" : "On track", item.risk() ? "warning" : "positive"))
                        .toList(),
                profile.subjects().stream()
                        .map(item -> new SchoolAdminDtos.LearnerRequirementDto(item.subjectName(), item.markPercent().setScale(0, RoundingMode.HALF_UP) + "%", item.risk() ? "warning" : "positive"))
                        .toList(),
                qualifiedCourses,
                closeCourses,
                readiness == null ? List.of() : List.of(new SchoolAdminDtos.InsightItemDto(readiness.learnerName(), readiness.alternativePathway(), "neutral")),
                bursaryMatches,
                missingRequirements,
                bursaryMatches.stream()
                        .filter(item -> normalize(item.deadline()) != null)
                        .map(item -> new SchoolAdminDtos.LearnerRequirementDto(item.title(), item.deadline(), "warning"))
                        .toList(),
                profile.interventions().stream().map(this::toAdminIntervention).toList(),
                teacherNotes(schoolId, learnerUserId),
                profile.interventions().stream().map(SchoolPortalDtos.InterventionSummaryView::followUpDate).filter(Objects::nonNull).map(LocalDate::toString).findFirst().orElse(null),
                profile.activityTimeline().stream().map(item -> new SchoolAdminDtos.SchoolAdminTimelineDto(item.title(), item.detail(), item.occurredAt() == null ? null : item.occurredAt().toString(), item.type())).toList()
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.SchoolAdminCareerReadinessResponse careerReadiness(UUID schoolId, UUID viewerUserId) {
        SchoolPortalDtos.CareerReadinessResponse career = schoolService.careerReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        long selectedGoals = career.learners().stream().filter(item -> normalize(item.careerGoal()) != null).count();
        long aligned = career.learners().stream().filter(SchoolPortalDtos.CareerReadinessLearnerView::aligned).count();
        long notAligned = career.learners().size() - aligned;
        return new SchoolAdminDtos.SchoolAdminCareerReadinessResponse(
                topCareerReadinessHeadline(career),
                List.of(
                        metric("Learners with selected career goals", selectedGoals, "Career pathways recorded", selectedGoals > 0 ? "positive" : "warning"),
                        metric("Learners aligned to chosen careers", aligned, "Ready against target pathway", aligned > 0 ? "positive" : "warning"),
                        metric("Learners not aligned", notAligned, "Need subject or pathway intervention", notAligned > 0 ? "warning" : "positive")
                ),
                toInsightItems(career.topCareerInterests(), "interested learners", "neutral"),
                toInsightItems(career.readinessGaps(), "readiness gaps", "warning"),
                career.learners().stream()
                        .filter(item -> !item.aligned())
                        .limit(8)
                        .map(item -> new SchoolAdminDtos.InsightItemDto(item.learnerName(), item.readinessGap(), "warning"))
                        .toList(),
                toInsightItems(career.alternativePathways(), "alternative recommendations", "neutral")
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.SchoolAdminCoursesResponse courses(UUID schoolId, UUID viewerUserId) {
        List<SchoolPortalDtos.LearnerListItem> learners = schoolService.portalLearners(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, null, null, null).items();
        List<SchoolAdminDtos.SchoolAdminCourseMatchDto> learnerMatches = learners.stream()
                .limit(20)
                .map(item -> {
                    SchoolPortalDtos.LearnerProfileResponse profile = schoolService.portalLearnerProfile(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, item.learnerUserId());
                    List<SchoolAdminDtos.SchoolPortalCourseDto> qualified = profile.courseEligibility().stream()
                            .filter(SchoolPortalDtos.MatchedCourseView::eligible)
                            .map(course -> new SchoolAdminDtos.SchoolPortalCourseDto(course.name(), course.level(), true, course.reason()))
                            .toList();
                    List<SchoolAdminDtos.SchoolPortalCourseDto> close = profile.courseEligibility().stream()
                            .filter(course -> !course.eligible())
                            .map(course -> new SchoolAdminDtos.SchoolPortalCourseDto(course.name(), course.level(), false, course.reason()))
                            .limit(4)
                            .toList();
                    return new SchoolAdminDtos.SchoolAdminCourseMatchDto(item.learnerUserId(), item.learnerName(), item.careerGoal(), item.apsPoints(), qualified, close);
                })
                .toList();
        Map<String, Long> qualifiedCounts = learnerMatches.stream()
                .flatMap(item -> item.qualifiedCourses().stream())
                .collect(Collectors.groupingBy(SchoolAdminDtos.SchoolPortalCourseDto::name, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> gapCounts = learnerMatches.stream()
                .flatMap(item -> item.closeCourses().stream())
                .collect(Collectors.groupingBy(SchoolAdminDtos.SchoolPortalCourseDto::reason, LinkedHashMap::new, Collectors.counting()));
        long universityReady = learnerMatches.stream().filter(item -> !item.qualifiedCourses().isEmpty()).count();
        long vocationalReady = learnerMatches.stream().filter(item -> item.qualifiedCourses().stream().anyMatch(course -> normalize(course.level()) != null && course.level().toLowerCase(Locale.ROOT).contains("tvet"))).count();
        return new SchoolAdminDtos.SchoolAdminCoursesResponse(
                universityReady + " learners qualify for at least one study pathway. " + vocationalReady + " already show a TVET or vocational route.",
                List.of(
                        metric("University-ready learners", universityReady, "At least one current match", universityReady > 0 ? "positive" : "warning"),
                        metric("Vocational-pathway-ready learners", vocationalReady, "TVET and vocational alternatives", vocationalReady > 0 ? "positive" : "neutral"),
                        metric("Most common qualification gaps", gapCounts.values().stream().mapToLong(Long::longValue).sum(), "Learners close to qualifying", gapCounts.isEmpty() ? "positive" : "warning")
                ),
                qualifiedCounts.entrySet().stream().limit(6).map(entry -> new SchoolAdminDtos.InsightItemDto(entry.getKey(), entry.getValue() + " learners matched", "positive")).toList(),
                qualifiedCounts.entrySet().stream().limit(8).map(entry -> new SchoolAdminDtos.InsightItemDto(entry.getKey(), entry.getValue() + " current matches", "neutral")).toList(),
                gapCounts.entrySet().stream().limit(8).map(entry -> new SchoolAdminDtos.InsightItemDto(entry.getKey(), entry.getValue() + " learners affected", "warning")).toList(),
                learnerMatches
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.SchoolAdminBursaryReadinessResponse bursaries(UUID schoolId, UUID viewerUserId) {
        SchoolPortalDtos.BursaryReadinessResponse bursaries = schoolService.bursaryReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        Map<String, Long> providers = bursaries.matches().stream()
                .collect(Collectors.groupingBy(item -> nullSafe(item.provider(), "Unspecified provider"), LinkedHashMap::new, Collectors.counting()));
        return new SchoolAdminDtos.SchoolAdminBursaryReadinessResponse(
                bursaries.matches().size() + " bursary matches identified. " + bursaries.deadlineAlerts().size() + " deadline alerts require follow-up.",
                List.of(
                        metric("Learners matched to bursaries", bursaries.matches().stream().map(SchoolPortalDtos.BursaryReadinessItem::learnerUserId).distinct().count(), "At least one funding pathway", !bursaries.matches().isEmpty() ? "positive" : "warning"),
                        metric("Deadline alerts", bursaries.deadlineAlerts().size(), "Upcoming bursary deadlines", !bursaries.deadlineAlerts().isEmpty() ? "warning" : "positive"),
                        metric("Learners needing application support", bursaries.missingRequirements().stream().mapToLong(SchoolPortalDtos.TopBreakdownItem::value).sum(), "Checklist and missing requirement support", !bursaries.missingRequirements().isEmpty() ? "warning" : "neutral")
                ),
                providers.entrySet().stream().limit(6).map(entry -> new SchoolAdminDtos.InsightItemDto(entry.getKey(), entry.getValue() + " matched learners", "neutral")).toList(),
                toInsightItems(bursaries.missingRequirements(), "missing requirement flags", "warning"),
                bursaries.deadlineAlerts().stream().limit(8).map(item -> new SchoolAdminDtos.InsightItemDto(item.learnerName(), item.bursaryTitle() + " deadline " + item.deadline(), "warning")).toList(),
                bursaries.matches().stream().limit(12).map(item -> new SchoolAdminDtos.SchoolPortalBursaryDto(item.bursaryTitle(), item.provider(), item.deadline() == null ? null : item.deadline().toString(), true, item.checklist())).toList()
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.SchoolAdminInterventionsResponse interventions(UUID schoolId, UUID viewerUserId) {
        List<SchoolPortalDtos.InterventionReportItem> items = schoolService.interventions(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        Map<String, Long> typeCounts = items.stream()
                .collect(Collectors.groupingBy(SchoolPortalDtos.InterventionReportItem::supportType, LinkedHashMap::new, Collectors.counting()));
        long dueFollowUps = items.stream().filter(item -> item.followUpDate() != null && !item.followUpDate().isAfter(LocalDate.now().plusDays(7))).count();
        return new SchoolAdminDtos.SchoolAdminInterventionsResponse(
                items.size() + " interventions logged. " + dueFollowUps + " follow-ups are due within 7 days.",
                List.of(
                        metric("Flagged learners", items.stream().map(SchoolPortalDtos.InterventionReportItem::learnerUserId).distinct().count(), "Learners with active support history", !items.isEmpty() ? "warning" : "positive"),
                        metric("Open interventions", items.stream().filter(item -> !"COMPLETED".equalsIgnoreCase(item.status())).count(), "Require continued tracking", items.stream().anyMatch(item -> !"COMPLETED".equalsIgnoreCase(item.status())) ? "warning" : "positive"),
                        metric("Follow-up due", dueFollowUps, "Scheduled follow-ups in the next 7 days", dueFollowUps > 0 ? "warning" : "positive")
                ),
                typeCounts.entrySet().stream().map(entry -> new SchoolAdminDtos.InsightItemDto(entry.getKey(), entry.getValue() + " interventions", "neutral")).toList(),
                items.stream().map(this::toAdminInterventionReport).toList()
        );
    }

    @Transactional
    public SchoolAdminDtos.SchoolAdminInterventionReportDto createIntervention(
            UUID schoolId,
            UUID actorUserId,
            SchoolPortalDtos.InterventionRequest request
    ) {
        SchoolPortalDtos.InterventionReportItem item = schoolService.createIntervention(schoolId, actorUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, request);
        writeAudit(actorUserId, "SCHOOL_INTERVENTION_CREATED", item.interventionId(), Map.of("schoolId", schoolId, "supportType", item.supportType()));
        return toAdminInterventionReport(item);
    }

    @Transactional
    public SchoolAdminDtos.SchoolAdminInterventionReportDto updateIntervention(
            UUID schoolId,
            UUID actorUserId,
            UUID interventionId,
            SchoolPortalDtos.InterventionProgressRequest request
    ) {
        SchoolPortalDtos.InterventionReportItem item = schoolService.updateIntervention(schoolId, actorUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, interventionId, request);
        writeAudit(actorUserId, "SCHOOL_INTERVENTION_UPDATED", interventionId, Map.of("schoolId", schoolId, "status", request.status()));
        return toAdminInterventionReport(item);
    }

    @Transactional
    public SchoolAdminDtos.LearnerAdminItemDto createLearner(UUID schoolId, UUID actorUserId, SchoolAdminDtos.LearnerCreateRequest request) {
        String username = normalize(request.username()) == null
                ? synthesizeUsername(request.firstName() + " " + request.lastName())
                : request.username().trim().toLowerCase(Locale.ROOT);
        String password = normalize(request.password()) == null ? generatePassword(request.firstName(), request.lastName()) : request.password().trim();
        String email = normalize(request.email()) == null ? username + "@school.local" : request.email().trim().toLowerCase(Locale.ROOT);
        SchoolPortalDtos.SchoolUserAdminView saved = schoolService.createSchoolUser(schoolId, new SchoolPortalDtos.SchoolUserCreateRequest(
                email,
                password,
                request.firstName(),
                request.lastName(),
                SchoolAccessService.ROLE_SCHOOL_STUDENT,
                null,
                null,
                request.grade(),
                request.careerGoal(),
                request.popiaConsentAccepted(),
                request.consentVersion()
        ));
        SchoolUserProfile profile = schoolUserProfileRepository.findBySchoolIdAndUserIdAndDeletedFalse(schoolId, saved.userId()).orElseThrow();
        profile.setPortalUsername(username);
        profile.setInitialPassword(password);
        profile.setGuardianName(trim(request.parentGuardianName()));
        profile.setGuardianPhone(trim(request.parentGuardianPhone()));
        profile.setGuardianEmail(trim(request.parentGuardianEmail()));
        profile.setConsentStatus(Boolean.TRUE.equals(request.popiaConsentAccepted()) ? "Consent captured" : "Pending consent");
        profile.setReportUploadStatus("Pending");
        schoolUserProfileRepository.save(profile);
        linkLearnerToClassAndSubject(schoolId, saved.userId(), request.grade(), request.className());
        writeAudit(actorUserId, "SCHOOL_LEARNER_CREATED", saved.userId(), Map.of("schoolId", schoolId, "username", username));
        return learners(schoolId, saved.userId(), null, null, null).items().stream()
                .filter(item -> item.learnerUserId().equals(saved.userId()))
                .findFirst()
                .orElseGet(() -> new SchoolAdminDtos.LearnerAdminItemDto(
                        saved.userId(),
                        saved.fullName(),
                        username,
                        email,
                        password,
                        request.grade(),
                        request.className(),
                        null,
                        0,
                        false,
                        false,
                        false,
                        "Profile setup pending",
                        0,
                        request.parentGuardianName(),
                        request.parentGuardianPhone(),
                        request.parentGuardianEmail(),
                        Boolean.TRUE.equals(request.popiaConsentAccepted()) ? "Consent captured" : "Pending consent",
                        "Pending",
                        request.careerGoal(),
                        null
                ));
    }

    @Transactional
    public SchoolPortalDtos.BulkLearnerUploadResult importLearners(UUID schoolId, MultipartFile file) {
        SchoolPortalDtos.BulkLearnerUploadResult result = schoolService.bulkUploadLearners(schoolId, file);
        List<SchoolUserProfile> learners = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, SchoolAccessService.ROLE_SCHOOL_STUDENT);
        for (SchoolUserProfile learner : learners) {
            if (normalize(learner.getPortalUsername()) == null) {
                User user = userRepository.findById(learner.getUserId()).orElse(null);
                if (user == null) {
                    continue;
                }
                learner.setPortalUsername(synthesizeUsername((user.getFirstName() + " " + user.getLastName()).trim()));
                if (normalize(learner.getInitialPassword()) == null) {
                    learner.setInitialPassword(generatePassword(user.getFirstName(), user.getLastName()));
                }
                learner.setConsentStatus(nullSafe(learner.getConsentStatus(), "Pending consent"));
                learner.setReportUploadStatus(nullSafe(learner.getReportUploadStatus(), "Pending"));
                schoolUserProfileRepository.save(learner);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.LearnerCredentialsExportDto learnerCredentials(UUID schoolId) {
        List<SchoolAdminDtos.LearnerCredentialItemDto> items = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, SchoolAccessService.ROLE_SCHOOL_STUDENT).stream()
                .map(profile -> {
                    User user = userRepository.findById(profile.getUserId()).orElse(null);
                    if (user == null) {
                        return null;
                    }
                    String learnerName = (nullSafe(user.getFirstName(), "") + " " + nullSafe(user.getLastName(), "")).trim();
                    String classLabel = learnerClassLabel(schoolId, profile.getUserId());
                    return new SchoolAdminDtos.LearnerCredentialItemDto(
                            learnerName,
                            nullSafe(profile.getPortalUsername(), synthesizeUsername(learnerName)),
                            nullSafe(profile.getInitialPassword(), "Password unavailable"),
                            classLabel.contains(" ") ? classLabel.substring(0, classLabel.indexOf(' ')) : classLabel,
                            classLabel
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SchoolAdminDtos.LearnerCredentialItemDto::learnerName))
                .toList();
        StringBuilder csv = new StringBuilder("learner,username,password,grade,class\n");
        for (SchoolAdminDtos.LearnerCredentialItemDto item : items) {
            csv.append(csv(item.learnerName())).append(',')
                    .append(csv(item.username())).append(',')
                    .append(csv(item.password())).append(',')
                    .append(csv(item.grade())).append(',')
                    .append(csv(item.className())).append('\n');
        }
        return new SchoolAdminDtos.LearnerCredentialsExportDto(
                "learner-credentials.csv",
                "text/csv",
                Base64.getEncoder().encodeToString(csv.toString().getBytes(StandardCharsets.UTF_8)),
                items
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.ClassAdminListResponse classes(UUID schoolId, UUID viewerUserId) {
        List<ClassSnapshot> snapshots = classSnapshots(schoolId, viewerUserId);
        long totalLearners = snapshots.stream().mapToLong(item -> item.summary().learnerCount()).sum();
        BigDecimal avgAps = snapshots.isEmpty()
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(snapshots.stream().mapToLong(item -> item.summary().averageAps()).average().orElse(0)).setScale(1, RoundingMode.HALF_UP);
        BigDecimal avgAttendance = averageDecimal(snapshots.stream().map(ClassSnapshot::attendanceRate).toList());
        BigDecimal avgAssignments = averageDecimal(snapshots.stream().map(ClassSnapshot::assignmentCompletionRate).toList());
        long careerReady = snapshots.stream().mapToLong(ClassSnapshot::careerReadyLearners).sum();
        long bursaryReady = snapshots.stream().mapToLong(ClassSnapshot::bursaryReadyLearners).sum();
        long atRisk = snapshots.stream().mapToLong(ClassSnapshot::atRiskLearners).sum();
        long interventionCases = snapshots.stream().mapToLong(item -> item.summary().interventionCount()).sum();
        BigDecimal reportCompletion = averageDecimal(snapshots.stream().map(item -> item.summary().reportUploadCompletion()).toList());
        return new SchoolAdminDtos.ClassAdminListResponse(
                List.of(
                        metric("Total Classes", snapshots.size(), "Configured school classes", "neutral"),
                        metric("Active Classes", snapshots.stream().filter(item -> item.summary().active()).count(), "Classes currently active", "positive"),
                        metric("Total Learners", totalLearners, "Learners enrolled across classes", totalLearners > 0 ? "positive" : "neutral"),
                        metric("Average APS", avgAps.toPlainString(), "Across enrolled learners", avgAps.compareTo(new BigDecimal("28")) >= 0 ? "positive" : "warning"),
                        metric("Career Ready Learners", careerReady, "Aligned to pathway requirements", careerReady > 0 ? "positive" : "neutral"),
                        metric("Bursary Ready Learners", bursaryReady, "Matched to funding pathways", bursaryReady > 0 ? "positive" : "neutral"),
                        metric("At-Risk Learners", atRisk, "Need support or intervention", atRisk > 0 ? "warning" : "positive"),
                        metric("Intervention Cases", interventionCases, "Open and completed class interventions", interventionCases > 0 ? "warning" : "positive"),
                        metric("Report Upload Completion", reportCompletion.toPlainString() + "%", "Learner report/upload readiness", reportCompletion.compareTo(new BigDecimal("60")) >= 0 ? "positive" : "warning"),
                        metric("Attendance Rate", avgAttendance.toPlainString() + "%", "Last 30-day learner activity proxy", avgAttendance.compareTo(new BigDecimal("70")) >= 0 ? "positive" : "warning"),
                        metric("Assignment Completion Rate", avgAssignments.toPlainString() + "%", "Expected learner submissions", avgAssignments.compareTo(new BigDecimal("70")) >= 0 ? "positive" : "warning")
                ),
                snapshots.stream().map(ClassSnapshot::summary).toList(),
                rankClasses(snapshots, Comparator.comparing((ClassSnapshot item) -> item.summary().averageAps()).reversed(), item -> "APS " + item.summary().averageAps() + " | Career readiness " + item.summary().careerReadinessPercent() + "%", "positive"),
                rankClasses(snapshots, Comparator.comparing(ClassSnapshot::assignmentCompletionRate).reversed(), item -> "Assignment completion " + item.assignmentCompletionRate().toPlainString() + "%", "positive"),
                rankClasses(snapshots, Comparator.comparing((ClassSnapshot item) -> item.atRiskLearners() + item.summary().interventionCount()).reversed(), item -> item.atRiskLearners() + " at risk | " + item.summary().interventionCount() + " interventions", "warning"),
                rankClasses(snapshots, Comparator.comparing((ClassSnapshot item) -> item.summary().careerReadinessPercent()).reversed(), item -> "Career readiness " + item.summary().careerReadinessPercent() + "%", "positive")
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.ClassProfileResponse classProfile(UUID schoolId, UUID viewerUserId, UUID classId) {
        ClassSnapshot snapshot = classSnapshots(schoolId, viewerUserId).stream().filter(item -> item.summary().classId().equals(classId)).findFirst().orElseThrow();
        SchoolAdminDtos.ClassAnalyticsResponse analytics = classAnalytics(schoolId, viewerUserId, classId);
        SchoolAdminDtos.ClassCareerReadinessResponse career = classCareerReadiness(schoolId, viewerUserId, classId);
        SchoolAdminDtos.ClassBursaryReadinessResponse bursaries = classBursaries(schoolId, viewerUserId, classId);
        SchoolAdminDtos.ClassAiInsightsResponse ai = classAiInsights(schoolId, viewerUserId, classId);
        return new SchoolAdminDtos.ClassProfileResponse(
                snapshot.summary(),
                List.of(
                        new SchoolAdminDtos.InsightItemDto("Average APS", String.valueOf(snapshot.summary().averageAps()), "positive"),
                        new SchoolAdminDtos.InsightItemDto("Subjects", String.valueOf(snapshot.summary().subjectCount()), "neutral"),
                        new SchoolAdminDtos.InsightItemDto("Assignment completion", snapshot.assignmentCompletionRate().toPlainString() + "%", snapshot.assignmentCompletionRate().compareTo(new BigDecimal("70")) >= 0 ? "positive" : "warning")
                ),
                career.careerInterests(),
                bursaries.applicationReadiness(),
                snapshot.interventions().stream().limit(8).map(item -> new SchoolAdminDtos.InsightItemDto(item.learnerName(), item.supportType() + " | " + item.status(), "warning")).toList(),
                ai.items(),
                buildClassTimeline(snapshot, analytics),
                snapshot.learners(),
                snapshot.subjectTeachers()
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.ClassAnalyticsResponse classAnalytics(UUID schoolId, UUID viewerUserId, UUID classId) {
        ClassSnapshot snapshot = classSnapshots(schoolId, viewerUserId).stream().filter(item -> item.summary().classId().equals(classId)).findFirst().orElseThrow();
        return new SchoolAdminDtos.ClassAnalyticsResponse(
                classId,
                snapshot.summary().grade() + " " + snapshot.summary().className(),
                List.of(
                        metric("Average APS", String.valueOf(snapshot.summary().averageAps()), "Across class learners", snapshot.summary().averageAps() >= 28 ? "positive" : "warning"),
                        metric("Career Readiness", snapshot.summary().careerReadinessPercent().toPlainString() + "%", "Learners aligned to career requirements", snapshot.summary().careerReadinessPercent().compareTo(new BigDecimal("60")) >= 0 ? "positive" : "warning"),
                        metric("Learner Engagement", snapshot.attendanceRate().toPlainString() + "%", "Recent learner activity proxy", snapshot.attendanceRate().compareTo(new BigDecimal("70")) >= 0 ? "positive" : "warning"),
                        metric("Assessment Completion", snapshot.assessmentCompletionRate().toPlainString() + "%", "Assessment evidence coverage", snapshot.assessmentCompletionRate().compareTo(new BigDecimal("70")) >= 0 ? "positive" : "warning"),
                        metric("Assignment Completion", snapshot.assignmentCompletionRate().toPlainString() + "%", "Learner submission completion", snapshot.assignmentCompletionRate().compareTo(new BigDecimal("70")) >= 0 ? "positive" : "warning"),
                        metric("Report Upload Progress", snapshot.summary().reportUploadCompletion().toPlainString() + "%", "Learner report readiness", snapshot.summary().reportUploadCompletion().compareTo(new BigDecimal("60")) >= 0 ? "positive" : "warning")
                ),
                apsDistributionForClass(snapshot.learners()),
                subjectPerformanceForClass(snapshot),
                List.of(
                        new SchoolAdminDtos.TrendPointDto("Career Ready", snapshot.summary().careerReadinessPercent(), "positive"),
                        new SchoolAdminDtos.TrendPointDto("Bursary Ready", snapshot.summary().bursaryReadinessPercent(), "neutral"),
                        new SchoolAdminDtos.TrendPointDto("At Risk", BigDecimal.valueOf(snapshot.atRiskLearners()), "warning")
                ),
                List.of(
                        new SchoolAdminDtos.TrendPointDto("Open", BigDecimal.valueOf(snapshot.interventions().stream().filter(item -> !"COMPLETED".equalsIgnoreCase(item.status())).count()), "warning"),
                        new SchoolAdminDtos.TrendPointDto("Completed", BigDecimal.valueOf(snapshot.interventions().stream().filter(item -> "COMPLETED".equalsIgnoreCase(item.status())).count()), "positive"),
                        new SchoolAdminDtos.TrendPointDto("Follow-up Due", BigDecimal.valueOf(snapshot.interventions().stream().filter(item -> item.followUpDate() != null).count()), "neutral")
                )
        );
    }

    @Transactional(readOnly = true)
    public List<SchoolAdminDtos.LearnerAdminItemDto> classLearners(UUID schoolId, UUID viewerUserId, UUID classId) {
        return classSnapshots(schoolId, viewerUserId).stream().filter(item -> item.summary().classId().equals(classId)).findFirst().orElseThrow().learners();
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.ClassCareerReadinessResponse classCareerReadiness(UUID schoolId, UUID viewerUserId, UUID classId) {
        ClassSnapshot snapshot = classSnapshots(schoolId, viewerUserId).stream().filter(item -> item.summary().classId().equals(classId)).findFirst().orElseThrow();
        Map<String, Long> interests = snapshot.learners().stream()
                .map(SchoolAdminDtos.LearnerAdminItemDto::careerGoal)
                .filter(value -> normalize(value) != null)
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()));
        return new SchoolAdminDtos.ClassCareerReadinessResponse(
                classId,
                snapshot.summary().grade() + " " + snapshot.summary().className(),
                snapshot.summary().careerReadinessPercent(),
                interests.entrySet().stream().limit(8).map(entry -> new SchoolAdminDtos.InsightItemDto(entry.getKey(), entry.getValue() + " learners interested", "neutral")).toList(),
                List.of(
                        new SchoolAdminDtos.InsightItemDto("Career readiness %", snapshot.summary().careerReadinessPercent().toPlainString() + "%", snapshot.summary().careerReadinessPercent().compareTo(new BigDecimal("60")) >= 0 ? "positive" : "warning"),
                        new SchoolAdminDtos.InsightItemDto("APS readiness", snapshot.learnersMeetingApsRequirements() + " learners meet APS threshold", snapshot.learnersMeetingApsRequirements() > 0 ? "positive" : "warning")
                ),
                List.of(
                        new SchoolAdminDtos.InsightItemDto("Subject deficiencies", snapshot.atRiskLearners() + " learners have subject or readiness risk flags", snapshot.atRiskLearners() > 0 ? "warning" : "positive"),
                        new SchoolAdminDtos.InsightItemDto("APS deficiencies", (snapshot.summary().learnerCount() - snapshot.learnersMeetingApsRequirements()) + " learners are below APS threshold", (snapshot.summary().learnerCount() - snapshot.learnersMeetingApsRequirements()) > 0 ? "warning" : "positive")
                )
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.ClassBursaryReadinessResponse classBursaries(UUID schoolId, UUID viewerUserId, UUID classId) {
        ClassSnapshot snapshot = classSnapshots(schoolId, viewerUserId).stream().filter(item -> item.summary().classId().equals(classId)).findFirst().orElseThrow();
        long ready = snapshot.bursaryReadyLearners();
        long nearlyReady = snapshot.learners().stream().filter(item -> item.bursaryMatchCount() == 0 && item.apsPoints() >= 24).count();
        long notReady = Math.max(0, snapshot.summary().learnerCount() - ready - nearlyReady);
        return new SchoolAdminDtos.ClassBursaryReadinessResponse(
                classId,
                snapshot.summary().grade() + " " + snapshot.summary().className(),
                List.of(
                        metric("Bursary Ready", ready, "Matched to funding options", ready > 0 ? "positive" : "neutral"),
                        metric("Nearly Ready", nearlyReady, "Close to funding readiness", nearlyReady > 0 ? "warning" : "neutral"),
                        metric("Not Ready", notReady, "Require more support", notReady > 0 ? "warning" : "positive")
                ),
                snapshot.bursaryDeadlines().stream().limit(8).map(item -> new SchoolAdminDtos.InsightItemDto(item.label(), item.value(), item.tone())).toList(),
                snapshot.learners().stream().limit(8).map(item -> new SchoolAdminDtos.InsightItemDto(item.learnerName(), item.bursaryMatchCount() + " bursary matches | " + item.reportUploadStatus(), item.bursaryMatchCount() > 0 ? "positive" : "warning")).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<SchoolAdminDtos.SchoolAdminInterventionReportDto> classInterventions(UUID schoolId, UUID viewerUserId, UUID classId) {
        return classSnapshots(schoolId, viewerUserId).stream().filter(item -> item.summary().classId().equals(classId)).findFirst().orElseThrow().interventions();
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.ClassAiInsightsResponse classAiInsights(UUID schoolId, UUID viewerUserId, UUID classId) {
        ClassSnapshot snapshot = classSnapshots(schoolId, viewerUserId).stream().filter(item -> item.summary().classId().equals(classId)).findFirst().orElseThrow();
        boolean dataAvailable = snapshot.summary().learnerCount() > 0;
        List<SchoolAdminDtos.InsightItemDto> items = List.of(
                new SchoolAdminDtos.InsightItemDto(snapshot.summary().grade() + " " + snapshot.summary().className(), "APS " + snapshot.summary().averageAps() + " | Career readiness " + snapshot.summary().careerReadinessPercent() + "%", snapshot.summary().averageAps() >= 28 ? "positive" : "warning"),
                new SchoolAdminDtos.InsightItemDto("Career interest", dominantCareerInterest(snapshot.learners()), "neutral"),
                new SchoolAdminDtos.InsightItemDto("Interventions needed", snapshot.atRiskLearners() + " learners require intervention before application milestones.", snapshot.atRiskLearners() > 0 ? "warning" : "positive")
        );
        return new SchoolAdminDtos.ClassAiInsightsResponse(dataAvailable, dataAvailable ? null : EMPTY_AI_MESSAGE, items);
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.TeacherAdminListResponse teachers(UUID schoolId, UUID viewerUserId) {
        List<SchoolAdminDtos.TeacherAdminItemDto> items = teacherSnapshots(schoolId, viewerUserId).stream()
                .map(TeacherSnapshot::summary)
                .sorted(Comparator.comparing(SchoolAdminDtos.TeacherAdminItemDto::fullName))
                .toList();
        long pendingApprovals = items.stream().filter(item -> "PENDING".equalsIgnoreCase(item.status())).count();
        return new SchoolAdminDtos.TeacherAdminListResponse(items, items.size(), pendingApprovals);
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.TeacherAdminDashboardResponse teacherAnalytics(UUID schoolId, UUID viewerUserId) {
        List<TeacherSnapshot> snapshots = teacherSnapshots(schoolId, viewerUserId);
        long totalTeachers = snapshots.size();
        long activeTeachers = snapshots.stream().filter(item -> item.summary().active()).count();
        long pendingTeachers = snapshots.stream().filter(item -> "PENDING".equalsIgnoreCase(item.summary().status())).count();
        long suspendedTeachers = snapshots.stream().filter(item -> "SUSPENDED".equalsIgnoreCase(item.summary().status())).count();
        long resourcesUploaded = snapshots.stream().mapToLong(item -> item.summary().resourcesUploaded()).sum();
        long assignmentsCreated = snapshots.stream().mapToLong(item -> item.summary().createdAssignments()).sum();
        long assessmentsCreated = snapshots.stream().mapToLong(item -> item.summary().createdAssessments()).sum();
        long learnersSupported = snapshots.stream().mapToLong(item -> item.summary().learnersSupported()).sum();
        long activeInterventions = snapshots.stream().mapToLong(item -> item.summary().activeInterventions()).sum();
        long guidanceSessions = snapshots.stream().mapToLong(item -> item.summary().careerGuidanceSessions()).sum();
        BigDecimal avgEngagement = averageDecimal(snapshots.stream().map(item -> item.summary().engagementScore()).toList());
        BigDecimal avgPerformance = averageDecimal(snapshots.stream().map(item -> item.summary().averageLearnerPerformance()).toList());
        List<SchoolAdminDtos.MetricCardDto> metrics = List.of(
                metric("Total Teachers", totalTeachers, "All teacher accounts", "neutral"),
                metric("Active Teachers", activeTeachers, "Currently active in EduRite", activeTeachers > 0 ? "positive" : "neutral"),
                metric("Pending Teacher Approvals", pendingTeachers, "Require principal action", pendingTeachers > 0 ? "warning" : "positive"),
                metric("Suspended Teachers", suspendedTeachers, "Removed from active teaching", suspendedTeachers > 0 ? "warning" : "positive"),
                metric("Teacher Engagement Score", avgEngagement.toPlainString(), "Average across active teachers", avgEngagement.compareTo(new BigDecimal("50")) >= 0 ? "positive" : "warning"),
                metric("Resources Uploaded", resourcesUploaded, "Notes and learning resources", resourcesUploaded > 0 ? "positive" : "neutral"),
                metric("Assignments Created", assignmentsCreated, "Teacher-created assignments", assignmentsCreated > 0 ? "positive" : "neutral"),
                metric("Assessments Created", assessmentsCreated, "Tests, exams, SBA artifacts", assessmentsCreated > 0 ? "positive" : "neutral"),
                metric("Learners Supported", learnersSupported, "Distinct learner reach", learnersSupported > 0 ? "positive" : "neutral"),
                metric("Active Interventions", activeInterventions, "Open teacher-led support actions", activeInterventions > 0 ? "warning" : "positive"),
                metric("Career Guidance Sessions", guidanceSessions, "Teacher-led guidance interventions", guidanceSessions > 0 ? "positive" : "neutral"),
                metric("Average Teacher Performance", avgPerformance.toPlainString(), "Average learner performance across teachers", avgPerformance.compareTo(new BigDecimal("60")) >= 0 ? "positive" : "warning")
        );
        return new SchoolAdminDtos.TeacherAdminDashboardResponse(
                metrics,
                teacherWorkload(schoolId, viewerUserId).alerts(),
                snapshots.stream()
                        .sorted(Comparator.comparing((TeacherSnapshot item) -> item.summary().resourcesUploaded() + item.summary().createdAssignments()).reversed())
                        .limit(5)
                        .map(item -> new SchoolAdminDtos.InsightItemDto(item.summary().fullName(), item.summary().resourcesUploaded() + " resources, " + item.summary().createdAssignments() + " assignments, engagement " + item.summary().engagementScore(), "positive"))
                        .toList(),
                snapshots.stream()
                        .filter(item -> "PENDING".equalsIgnoreCase(item.summary().status()) || "SUSPENDED".equalsIgnoreCase(item.summary().status()))
                        .limit(6)
                        .map(item -> new SchoolAdminDtos.InsightItemDto(item.summary().fullName(), "Status " + item.summary().status() + " | " + item.summary().assignedSubjects() + " subjects", "warning"))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.TeacherActivityResponse teacherEngagement(UUID schoolId, UUID viewerUserId) {
        List<SchoolAdminDtos.TeacherActivityItemDto> all = teacherSnapshots(schoolId, viewerUserId).stream()
                .flatMap(item -> item.activities().stream())
                .sorted(Comparator.comparing(SchoolAdminDtos.TeacherActivityItemDto::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        OffsetDateTime now = OffsetDateTime.now();
        return new SchoolAdminDtos.TeacherActivityResponse(
                all.stream().filter(item -> item.occurredAt() != null && item.occurredAt().isAfter(now.minusDays(1))).toList(),
                all.stream().filter(item -> item.occurredAt() != null && item.occurredAt().isAfter(now.minusDays(7))).toList(),
                all.stream().filter(item -> item.occurredAt() != null && item.occurredAt().isAfter(now.minusDays(30))).toList()
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.TeacherAiInsightResponse teacherAiInsights(UUID schoolId, UUID viewerUserId) {
        List<TeacherSnapshot> snapshots = teacherSnapshots(schoolId, viewerUserId);
        boolean dataAvailable = snapshots.stream().anyMatch(item -> item.summary().learnerCount() > 0 || item.summary().resourcesUploaded() > 0 || item.summary().interventionCount() > 0);
        List<SchoolAdminDtos.InsightItemDto> items = snapshots.stream().limit(8).map(item -> {
            SchoolAdminDtos.TeacherAdminItemDto teacher = item.summary();
            String message;
            String severity;
            if (teacher.careerReadyLearners() > teacher.learnersAtRisk()) {
                message = teacher.fullName() + "'s learners show strong " + dominantSubject(item.subjects()) + " readiness.";
                severity = "positive";
            } else if (teacher.learnersAtRisk() > 0) {
                message = teacher.fullName() + "'s assigned learners need additional " + dominantSubject(item.subjects()) + " support and intervention follow-up.";
                severity = "warning";
            } else {
                message = teacher.fullName() + " is building readiness coverage through assignments, resources, and learner support.";
                severity = "neutral";
            }
            return new SchoolAdminDtos.InsightItemDto(teacher.fullName(), message, severity);
        }).toList();
        return new SchoolAdminDtos.TeacherAiInsightResponse(dataAvailable, dataAvailable ? null : EMPTY_AI_MESSAGE, items);
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.TeacherWorkloadResponse teacherWorkload(UUID schoolId, UUID viewerUserId) {
        List<TeacherSnapshot> snapshots = teacherSnapshots(schoolId, viewerUserId);
        List<SchoolAdminDtos.TeacherWorkloadItemDto> items = snapshots.stream()
                .map(item -> new SchoolAdminDtos.TeacherWorkloadItemDto(
                        item.summary().teacherUserId(),
                        item.summary().fullName(),
                        item.summary().assignedClasses(),
                        item.summary().assignedSubjects(),
                        item.summary().learnerCount(),
                        item.summary().createdAssessments(),
                        item.summary().createdAssignments(),
                        workloadBand(item.summary())
                ))
                .toList();
        List<SchoolAdminDtos.InsightItemDto> alerts = new ArrayList<>();
        items.stream().filter(item -> "Overloaded".equals(item.workloadBand())).limit(5)
                .forEach(item -> alerts.add(new SchoolAdminDtos.InsightItemDto(item.teacherName(), item.learnersAssigned() + " learners across " + item.classesAssigned() + " classes.", "warning")));
        items.stream().filter(item -> "Underutilised".equals(item.workloadBand())).limit(5)
                .forEach(item -> alerts.add(new SchoolAdminDtos.InsightItemDto(item.teacherName(), "Only " + item.classesAssigned() + " classes and " + item.subjectsAssigned() + " subjects assigned.", "neutral")));
        missingCoverageAlerts(schoolId).forEach(alerts::add);
        return new SchoolAdminDtos.TeacherWorkloadResponse(items, alerts);
    }

    @Transactional(readOnly = true)
    public List<SchoolAdminDtos.SchoolAdminInterventionReportDto> teacherInterventions(UUID schoolId, UUID viewerUserId) {
        return teacherSnapshots(schoolId, viewerUserId).stream()
                .flatMap(item -> item.interventions().stream())
                .sorted(Comparator.comparing(SchoolAdminDtos.SchoolAdminInterventionReportDto::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.TeacherResourceResponse teacherResources(UUID schoolId, UUID viewerUserId) {
        List<TeacherSnapshot> snapshots = teacherSnapshots(schoolId, viewerUserId);
        List<SchoolAdminDtos.TeacherResourceItemDto> teachers = snapshots.stream()
                .map(item -> new SchoolAdminDtos.TeacherResourceItemDto(
                        item.summary().teacherUserId(),
                        item.summary().fullName(),
                        item.summary().resourcesUploaded(),
                        0,
                        inferResourceCategories(item.notes())
                ))
                .toList();
        List<SchoolAdminDtos.InsightItemDto> topContributors = teachers.stream()
                .sorted(Comparator.comparing(SchoolAdminDtos.TeacherResourceItemDto::uploadCount).reversed())
                .limit(5)
                .map(item -> new SchoolAdminDtos.InsightItemDto(item.teacherName(), item.uploadCount() + " uploaded resources", "positive"))
                .toList();
        List<SchoolAdminDtos.InsightItemDto> mostUsed = snapshots.stream()
                .flatMap(item -> item.notes().stream())
                .sorted(Comparator.comparing(LearningNote::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(note -> new SchoolAdminDtos.InsightItemDto(note.getTitle(), inferResourceCategory(note) + " resource", "neutral"))
                .toList();
        return new SchoolAdminDtos.TeacherResourceResponse(teachers, mostUsed, topContributors);
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.TeacherTrainingResponse teacherTraining(UUID schoolId, UUID viewerUserId) {
        List<SchoolAdminDtos.TeacherTrainingItemDto> items = teacherSnapshots(schoolId, viewerUserId).stream()
                .map(item -> new SchoolAdminDtos.TeacherTrainingItemDto(
                        item.summary().teacherUserId(),
                        item.summary().fullName(),
                        0,
                        0,
                        item.summary().careerGuidanceSessions(),
                        item.summary().resourcesUploaded() + item.summary().createdAssignments(),
                        BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                        null,
                        "Not yet captured",
                        item.summary().engagementScore().compareTo(new BigDecimal("60")) >= 0 ? "Strong platform usage" : "Development data pending"
                ))
                .toList();
        return new SchoolAdminDtos.TeacherTrainingResponse(
                items,
                List.of(new SchoolAdminDtos.InsightItemDto("Professional development", "Training, certifications, CPD hours, and AI tool usage will appear once teacher development records are captured in EduRite.", "neutral"))
        );
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.TeacherDetailResponse teacherDetail(UUID schoolId, UUID viewerUserId, UUID teacherId) {
        TeacherSnapshot snapshot = teacherSnapshots(schoolId, viewerUserId).stream()
                .filter(item -> item.summary().teacherUserId().equals(teacherId))
                .findFirst()
                .orElseThrow();
        return new SchoolAdminDtos.TeacherDetailResponse(
                snapshot.summary(),
                snapshot.classes(),
                snapshot.subjects(),
                List.of(
                        new SchoolAdminDtos.TeacherProfileSectionDto("Name", snapshot.summary().fullName()),
                        new SchoolAdminDtos.TeacherProfileSectionDto("Email", snapshot.summary().email()),
                        new SchoolAdminDtos.TeacherProfileSectionDto("Phone", nullSafe(snapshot.user() == null ? null : snapshot.user().getPhoneNumber(), "Not set")),
                        new SchoolAdminDtos.TeacherProfileSectionDto("Employee Number", nullSafe(snapshot.profile() == null ? null : snapshot.profile().getEmployeeOrStudentNo(), "Not set")),
                        new SchoolAdminDtos.TeacherProfileSectionDto("Status", snapshot.summary().status())
                ),
                List.of(
                        new SchoolAdminDtos.TeacherProfileSectionDto("Qualifications", "Not yet captured in EduRite"),
                        new SchoolAdminDtos.TeacherProfileSectionDto("Experience", "Not yet captured in EduRite"),
                        new SchoolAdminDtos.TeacherProfileSectionDto("Registration Details", nullSafe(snapshot.profile() == null ? null : snapshot.profile().getEmployeeOrStudentNo(), "No registration details recorded"))
                ),
                List.of(
                        new SchoolAdminDtos.TeacherProfileSectionDto("Assignment Completion", snapshot.summary().submissionRate() + "% submission rate"),
                        new SchoolAdminDtos.TeacherProfileSectionDto("Assessment Completion", String.valueOf(snapshot.summary().createdAssessments())),
                        new SchoolAdminDtos.TeacherProfileSectionDto("Average Learner Performance", snapshot.summary().averageLearnerPerformance().toPlainString()),
                        new SchoolAdminDtos.TeacherProfileSectionDto("APS Impact", snapshot.summary().apsImpact().toPlainString())
                ),
                List.of(
                        new SchoolAdminDtos.TeacherProfileSectionDto("Career Mapped Learners", String.valueOf(snapshot.summary().careerMappedLearners())),
                        new SchoolAdminDtos.TeacherProfileSectionDto("Bursary Ready Learners", String.valueOf(snapshot.summary().bursaryReadyLearners())),
                        new SchoolAdminDtos.TeacherProfileSectionDto("At Risk Learners", String.valueOf(snapshot.summary().learnersAtRisk()))
                ),
                snapshot.activities(),
                List.of(),
                snapshot.notes().stream().limit(8).map(note -> new SchoolAdminDtos.InsightItemDto(note.getTitle(), nullSafe(note.getNoteText(), "Resource uploaded"), "neutral")).toList(),
                snapshot.interventions(),
                approvalHistoryForTeacher(teacherId)
        );
    }

    @Transactional
    public SchoolAdminDtos.TeacherAdminItemDto approveTeacher(UUID schoolId, UUID actorUserId, UUID teacherId) {
        return updateTeacherStatus(schoolId, actorUserId, teacherId, UserStatus.ACTIVE, true, "SCHOOL_TEACHER_APPROVED");
    }

    @Transactional
    public SchoolAdminDtos.TeacherAdminItemDto rejectTeacher(UUID schoolId, UUID actorUserId, UUID teacherId) {
        return updateTeacherStatus(schoolId, actorUserId, teacherId, UserStatus.SUSPENDED, false, "SCHOOL_TEACHER_REJECTED");
    }

    @Transactional
    public SchoolAdminDtos.TeacherAdminItemDto suspendTeacher(UUID schoolId, UUID actorUserId, UUID teacherId) {
        return updateTeacherStatus(schoolId, actorUserId, teacherId, UserStatus.SUSPENDED, false, "SCHOOL_TEACHER_SUSPENDED");
    }

    @Transactional
    public SchoolAdminDtos.TeacherAdminItemDto reactivateTeacher(UUID schoolId, UUID actorUserId, UUID teacherId) {
        return updateTeacherStatus(schoolId, actorUserId, teacherId, UserStatus.ACTIVE, true, "SCHOOL_TEACHER_REACTIVATED");
    }

    @Transactional
    public void deleteTeacher(UUID schoolId, UUID actorUserId, UUID teacherId) {
        schoolService.deactivateSchoolUser(schoolId, teacherId);
        writeAudit(actorUserId, "SCHOOL_TEACHER_DELETED", teacherId, Map.of("schoolId", schoolId));
    }

    @Transactional(readOnly = true)
    public List<SchoolAdminDtos.ReportItemDto> reports() {
        return List.of(
                new SchoolAdminDtos.ReportItemDto("school-performance-report", "School performance report", "Whole-school academic readiness and APS coverage.", true, true),
                new SchoolAdminDtos.ReportItemDto("learner-readiness-report", "Learner readiness report", "Learners requiring readiness support and profile completion follow-up.", true, true),
                new SchoolAdminDtos.ReportItemDto("subject-gap-report", "Subject gap report", "Subject weaknesses affecting progression and pathway alignment.", true, true),
                new SchoolAdminDtos.ReportItemDto("teacher-activity-report", "Teacher activity report", "Assignments, notes, and engagement activity by teacher.", true, true),
                new SchoolAdminDtos.ReportItemDto("aps-readiness-report", "APS readiness report", "APS distribution and warning bands.", true, true),
                new SchoolAdminDtos.ReportItemDto("career-pathway-report", "Career pathway report", "Career alignment and pathway alternatives.", true, true),
                new SchoolAdminDtos.ReportItemDto("district-ready-report", "District-ready report", "Executive summary prepared for district submission.", true, true)
        );
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.ReportExportResponse exportReport(UUID schoolId, UUID viewerUserId, SchoolAdminDtos.ReportExportRequest request) {
        String normalizedType = normalize(request.type());
        String legacyType = switch (normalizedType == null ? "" : normalizedType) {
            case "school-performance-report", "learner-readiness-report", "district-ready-report" -> "whole-school-readiness";
            case "subject-gap-report" -> "subject-gap";
            case "teacher-activity-report" -> "grade-readiness";
            case "aps-readiness-report" -> "at-risk-learner";
            case "career-pathway-report" -> "career-interest";
            default -> "bursary-readiness";
        };
        String normalizedFormat = normalize(request.format());
        String actualFormat = "xlsx".equals(normalizedFormat) || "excel".equals(normalizedFormat) ? "csv" : nullSafe(normalizedFormat, "pdf");
        return schoolService.exportReport(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, legacyType, actualFormat);
    }

    @Transactional
    public SchoolAdminDtos.AnnouncementItemDto createAnnouncement(UUID schoolId, UUID actorUserId, SchoolAdminDtos.AnnouncementCreateRequest request) {
        SchoolAnnouncement announcement = new SchoolAnnouncement();
        announcement.setSchoolId(schoolId);
        announcement.setCreatedByUserId(actorUserId);
        announcement.setAudience(request.audience().trim().toUpperCase(Locale.ROOT));
        announcement.setTitle(request.title().trim());
        announcement.setMessage(request.message().trim());
        announcement.setSentAt(OffsetDateTime.now());
        SchoolAnnouncement saved = schoolAnnouncementRepository.save(announcement);
        announcementRecipients(schoolId, saved.getAudience()).forEach(userId ->
                notificationService.createInApp(userId, "SCHOOL_ANNOUNCEMENT", saved.getTitle(), saved.getMessage()));
        writeAudit(actorUserId, "SCHOOL_ANNOUNCEMENT_CREATED", saved.getId(), Map.of("schoolId", schoolId, "audience", saved.getAudience()));
        return toAnnouncementDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SchoolAdminDtos.AnnouncementItemDto> announcements(UUID schoolId) {
        return schoolAnnouncementRepository.findBySchoolIdAndActiveTrueOrderByCreatedAtDesc(schoolId).stream()
                .map(this::toAnnouncementDto)
                .toList();
    }

    @Transactional
    public SchoolAdminDtos.SupportRequestItemDto createSupportRequest(UUID schoolId, UUID actorUserId, SchoolAdminDtos.SupportRequestCreateRequest request) {
        SchoolSupportRequest supportRequest = new SchoolSupportRequest();
        supportRequest.setSchoolId(schoolId);
        supportRequest.setRequesterUserId(actorUserId);
        supportRequest.setCategory(request.category().trim());
        supportRequest.setTitle(request.title().trim());
        supportRequest.setMessage(request.message().trim());
        supportRequest.setPriority(normalizePriority(request.priority()));
        SchoolSupportRequest saved = schoolSupportRequestRepository.save(supportRequest);
        SupportRequest districtSupport = new SupportRequest();
        districtSupport.setSchoolId(schoolId);
        districtSupport.setRequestedBy(actorUserId);
        districtSupport.setRequestType(request.category().trim());
        districtSupport.setDescription(request.message().trim());
        districtSupport.setStatus("OPEN");
        districtSupportRequestRepository.save(districtSupport);
        writeAudit(actorUserId, "SCHOOL_SUPPORT_REQUEST_CREATED", saved.getId(), Map.of("schoolId", schoolId, "category", saved.getCategory()));
        return toSupportRequestDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SchoolAdminDtos.SupportRequestItemDto> supportRequests(UUID schoolId) {
        return schoolSupportRequestRepository.findBySchoolIdAndActiveTrueOrderByCreatedAtDesc(schoolId).stream()
                .map(this::toSupportRequestDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolAdminDtos.SchoolSettingsResponse settings(UUID schoolId) {
        School school = schoolRepository.findById(schoolId).orElseThrow();
        List<String> activeRoles = schoolUserProfileRepository.findBySchoolIdAndDeletedFalse(schoolId).stream()
                .map(SchoolUserProfile::getRoleName)
                .distinct()
                .sorted()
                .toList();
        Set<UUID> schoolUsers = schoolUserProfileRepository.findBySchoolIdAndDeletedFalse(schoolId).stream().map(SchoolUserProfile::getUserId).collect(Collectors.toSet());
        List<SchoolAdminDtos.AuditItemDto> recentAuditLogs = auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(log -> log.getActorId() != null && schoolUsers.contains(log.getActorId()))
                .limit(20)
                .map(log -> new SchoolAdminDtos.AuditItemDto(log.getAction(), log.getEntityType(), log.getEntityId(), log.getCreatedAt()))
                .toList();
        return new SchoolAdminDtos.SchoolSettingsResponse(
                school.getSchoolName(),
                school.getRegistrationNumber(),
                school.getDistrict(),
                school.getProvince(),
                school.getContactEmail(),
                school.getContactPhone(),
                school.getAddress(),
                activeRoles,
                recentAuditLogs
        );
    }

    private SchoolAdminDtos.TeacherAdminItemDto updateTeacherStatus(UUID schoolId, UUID actorUserId, UUID teacherId, UserStatus status, boolean active, String action) {
        SchoolUserProfile profile = schoolUserProfileRepository.findBySchoolIdAndUserIdAndDeletedFalse(schoolId, teacherId).orElseThrow();
        User user = userRepository.findById(teacherId).orElseThrow();
        profile.setActive(active);
        schoolUserProfileRepository.save(profile);
        user.setStatus(status);
        userRepository.save(user);
        writeAudit(actorUserId, action, teacherId, Map.of("schoolId", schoolId, "status", status.name()));
        return teachers(schoolId, actorUserId).items().stream().filter(item -> item.teacherUserId().equals(teacherId)).findFirst().orElseThrow();
    }

    private List<SchoolAdminDtos.InsightItemDto> buildRecommendedInterventions(
            List<SchoolPortalDtos.LearnerListItem> learners,
            List<SchoolPortalDtos.CareerReadinessLearnerView> careerItems,
            List<SchoolPortalDtos.BursaryReadinessItem> bursaryItems
    ) {
        List<SchoolAdminDtos.InsightItemDto> items = new ArrayList<>();
        learners.stream().filter(SchoolPortalDtos.LearnerListItem::needsIntervention).limit(3)
                .forEach(item -> items.add(new SchoolAdminDtos.InsightItemDto(item.learnerName(), "Schedule academic support and guardian follow-up.", "critical")));
        careerItems.stream().filter(item -> !item.aligned()).limit(3)
                .forEach(item -> items.add(new SchoolAdminDtos.InsightItemDto(item.learnerName(), "Review pathway alternatives: " + item.alternativePathway(), "warning")));
        bursaryItems.stream().filter(item -> item.deadline() != null && !item.deadline().isAfter(LocalDate.now().plusDays(14))).limit(3)
                .forEach(item -> items.add(new SchoolAdminDtos.InsightItemDto(item.learnerName(), "Complete bursary checklist for " + item.bursaryTitle(), "warning")));
        return items;
    }

    private record TeacherSnapshot(
            User user,
            SchoolUserProfile profile,
            SchoolAdminDtos.TeacherAdminItemDto summary,
            List<String> classes,
            List<String> subjects,
            List<LearningNote> notes,
            List<SchoolAdminDtos.SchoolAdminInterventionReportDto> interventions,
            List<SchoolAdminDtos.TeacherActivityItemDto> activities
    ) {}

    private record ClassSnapshot(
            SchoolClass schoolClass,
            SchoolAdminDtos.ClassAdminItemDto summary,
            List<SchoolAdminDtos.LearnerAdminItemDto> learners,
            List<SchoolAdminDtos.SchoolAdminInterventionReportDto> interventions,
            List<SchoolAdminDtos.InsightItemDto> subjectTeachers,
            BigDecimal attendanceRate,
            BigDecimal assignmentCompletionRate,
            BigDecimal assessmentCompletionRate,
            long careerReadyLearners,
            long bursaryReadyLearners,
            long atRiskLearners,
            long learnersMeetingApsRequirements,
            List<SchoolAdminDtos.LearnerRequirementDto> bursaryDeadlines
    ) {}

    private List<ClassSnapshot> classSnapshots(UUID schoolId, UUID viewerUserId) {
        List<SchoolClass> classes = schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId);
        List<SchoolPortalDtos.LearnerListItem> learnerViews = schoolService.portalLearners(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, null, null, null).items();
        List<SchoolAdminDtos.LearnerAdminItemDto> adminLearners = learners(schoolId, viewerUserId, null, null, null).items();
        Map<UUID, SchoolAdminDtos.LearnerAdminItemDto> adminLearnerById = adminLearners.stream().collect(Collectors.toMap(SchoolAdminDtos.LearnerAdminItemDto::learnerUserId, item -> item, (left, right) -> left));
        Map<UUID, SchoolPortalDtos.LearnerListItem> learnerById = learnerViews.stream().collect(Collectors.toMap(SchoolPortalDtos.LearnerListItem::learnerUserId, item -> item, (left, right) -> left));
        Map<UUID, SchoolPortalDtos.CareerReadinessLearnerView> readinessByLearner = schoolService.careerReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN).learners().stream()
                .collect(Collectors.toMap(SchoolPortalDtos.CareerReadinessLearnerView::learnerUserId, item -> item, (left, right) -> left));
        List<SchoolPortalDtos.BursaryReadinessItem> bursaryItems = schoolService.bursaryReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN).matches();
        List<SchoolPortalDtos.InterventionReportItem> interventionItems = schoolService.interventions(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        List<LearnerEnrollment> enrollments = learnerEnrollmentRepository.findAll().stream()
                .filter(item -> schoolId.equals(item.getSchoolId()) && item.isActive())
                .toList();
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findAll().stream()
                .filter(item -> schoolId.equals(item.getSchoolId()) && item.isActive())
                .toList();
        List<SchoolSubject> subjects = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId);
        Map<UUID, SchoolSubject> subjectById = subjects.stream().collect(Collectors.toMap(SchoolSubject::getId, item -> item));
        List<SchoolTask> tasks = schoolTaskRepository.findAll().stream().filter(item -> schoolId.equals(item.getSchoolId())).toList();
        List<TaskSubmission> submissions = taskSubmissionRepository.findAll();
        List<SubmissionFeedback> feedback = submissionFeedbackRepository.findAll();
        Map<UUID, User> users = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, user -> user));

        return classes.stream().map(schoolClass -> {
            Set<UUID> learnerIds = enrollments.stream().filter(item -> schoolClass.getId().equals(item.getClassId())).map(LearnerEnrollment::getLearnerUserId).collect(Collectors.toSet());
            Set<UUID> subjectIds = enrollments.stream().filter(item -> schoolClass.getId().equals(item.getClassId())).map(LearnerEnrollment::getSubjectId).collect(Collectors.toSet());
            List<SchoolAdminDtos.LearnerAdminItemDto> classLearners = learnerIds.stream().map(adminLearnerById::get).filter(Objects::nonNull).sorted(Comparator.comparing(SchoolAdminDtos.LearnerAdminItemDto::learnerName)).toList();
            long avgAps = Math.round(classLearners.stream().mapToLong(SchoolAdminDtos.LearnerAdminItemDto::apsPoints).average().orElse(0));
            long careerReady = learnerIds.stream().map(readinessByLearner::get).filter(Objects::nonNull).filter(SchoolPortalDtos.CareerReadinessLearnerView::aligned).count();
            long bursaryReady = bursaryItems.stream().map(SchoolPortalDtos.BursaryReadinessItem::learnerUserId).filter(learnerIds::contains).distinct().count();
            long atRisk = classLearners.stream().filter(SchoolAdminDtos.LearnerAdminItemDto::needsIntervention).count();
            long interventions = interventionItems.stream().filter(item -> learnerIds.contains(item.learnerUserId())).count();
            long apsReady = classLearners.stream().filter(item -> item.apsPoints() >= 28).count();
            BigDecimal careerReadinessPercent = percentage(careerReady, classLearners.size());
            BigDecimal bursaryReadinessPercent = percentage(bursaryReady, classLearners.size());
            BigDecimal reportUploadCompletion = percentage(classLearners.stream().filter(item -> normalize(item.reportUploadStatus()) != null && item.reportUploadStatus().toLowerCase(Locale.ROOT).contains("complete")).count(), classLearners.size());
            BigDecimal attendanceRate = percentage(
                    learnerIds.stream().map(users::get).filter(Objects::nonNull).filter(user -> user.getLastLoginAt() != null && user.getLastLoginAt().isAfter(OffsetDateTime.now().minusDays(30))).count(),
                    classLearners.size()
            );
            List<SchoolTask> classTasks = tasks.stream().filter(task -> schoolClass.getId().equals(task.getClassId())).toList();
            List<UUID> classTaskIds = classTasks.stream().map(SchoolTask::getId).toList();
            long expectedSubmissions = Math.max(1L, classLearners.size() * Math.max(1, classTasks.size()));
            long actualSubmissions = submissions.stream().filter(item -> classTaskIds.contains(item.getTaskId()) && learnerIds.contains(item.getLearnerUserId())).count();
            long classAssessments = classTasks.stream().filter(this::isAssessmentTask).count();
            long releasedAssessments = classTasks.stream()
                    .filter(this::isAssessmentTask)
                    .filter(task -> submissions.stream().filter(submission -> task.getId().equals(submission.getTaskId())).anyMatch(submission ->
                            feedback.stream().anyMatch(item -> submission.getId().equals(item.getSubmissionId()) && item.isReleased())))
                    .count();
            BigDecimal assignmentCompletion = BigDecimal.valueOf((actualSubmissions * 100.0) / expectedSubmissions).setScale(1, RoundingMode.HALF_UP);
            BigDecimal assessmentCompletion = percentage(releasedAssessments, Math.max(1L, classAssessments));
            List<TeacherAssignment> classAssignments = assignments.stream().filter(item -> schoolClass.getId().equals(item.getClassId())).toList();
            String classTeacher = classAssignments.stream()
                    .map(TeacherAssignment::getTeacherUserId)
                    .collect(Collectors.groupingBy(id -> id, LinkedHashMap::new, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .map(users::get)
                    .map(user -> (nullSafe(user.getFirstName(), "") + " " + nullSafe(user.getLastName(), "")).trim())
                    .orElse("Teacher pending");
            List<SchoolAdminDtos.InsightItemDto> subjectTeachers = classAssignments.stream()
                    .map(item -> new SchoolAdminDtos.InsightItemDto(
                            subjectById.get(item.getSubjectId()) == null ? "Subject" : subjectById.get(item.getSubjectId()).getSubjectName(),
                            users.get(item.getTeacherUserId()) == null ? "Teacher pending" : (nullSafe(users.get(item.getTeacherUserId()).getFirstName(), "") + " " + nullSafe(users.get(item.getTeacherUserId()).getLastName(), "")).trim(),
                            "neutral"
                    ))
                    .distinct()
                    .toList();
            List<SchoolAdminDtos.SchoolAdminInterventionReportDto> classInterventions = interventionItems.stream()
                    .filter(item -> learnerIds.contains(item.learnerUserId()))
                    .map(this::toAdminInterventionReport)
                    .toList();
            List<SchoolAdminDtos.LearnerRequirementDto> bursaryDeadlines = bursaryItems.stream()
                    .filter(item -> learnerIds.contains(item.learnerUserId()) && item.deadline() != null)
                    .map(item -> new SchoolAdminDtos.LearnerRequirementDto(item.learnerName(), item.bursaryTitle() + " deadline " + item.deadline(), "warning"))
                    .toList();
            return new ClassSnapshot(
                    schoolClass,
                    new SchoolAdminDtos.ClassAdminItemDto(
                            schoolClass.getId(),
                            schoolClass.getClassName(),
                            schoolClass.getGrade(),
                            schoolClass.getAcademicYear(),
                            schoolClass.getTerm(),
                            classTeacher,
                            classLearners.size(),
                            subjectIds.size(),
                            avgAps,
                            careerReadinessPercent,
                            bursaryReadinessPercent,
                            interventions,
                            schoolClass.isActive(),
                            attendanceRate,
                            assignmentCompletion,
                            reportUploadCompletion
                    ),
                    classLearners,
                    classInterventions,
                    subjectTeachers,
                    attendanceRate,
                    assignmentCompletion,
                    assessmentCompletion,
                    careerReady,
                    bursaryReady,
                    atRisk,
                    apsReady,
                    bursaryDeadlines
            );
        }).toList();
    }

    private List<TeacherSnapshot> teacherSnapshots(UUID schoolId, UUID viewerUserId) {
        List<SchoolPortalDtos.SchoolUserAdminView> teacherUsers = schoolService.teachers(schoolId);
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findAll().stream()
                .filter(item -> schoolId.equals(item.getSchoolId()) && item.isActive())
                .toList();
        List<SchoolTask> tasks = schoolTaskRepository.findAll().stream().filter(item -> schoolId.equals(item.getSchoolId())).toList();
        List<LearningNote> notes = learningNoteRepository.findAll().stream().filter(item -> schoolId.equals(item.getSchoolId())).toList();
        List<TaskSubmission> submissions = taskSubmissionRepository.findAll();
        List<SubmissionFeedback> feedback = submissionFeedbackRepository.findAll();
        List<LearnerEnrollment> enrollments = learnerEnrollmentRepository.findAll().stream()
                .filter(item -> schoolId.equals(item.getSchoolId()) && item.isActive())
                .toList();
        List<SchoolPortalDtos.LearnerListItem> learners = schoolService.portalLearners(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN, null, null, null).items();
        List<SchoolPortalDtos.CareerReadinessLearnerView> readiness = schoolService.careerReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN).learners();
        List<SchoolPortalDtos.BursaryReadinessItem> bursaries = schoolService.bursaryReadiness(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN).matches();
        List<SchoolPortalDtos.InterventionReportItem> interventionItems = schoolService.interventions(schoolId, viewerUserId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        Map<UUID, User> users = userRepository.findAllById(teacherUsers.stream().map(SchoolPortalDtos.SchoolUserAdminView::userId).toList()).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<UUID, SchoolUserProfile> profiles = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, SchoolAccessService.ROLE_TEACHER).stream()
                .collect(Collectors.toMap(SchoolUserProfile::getUserId, profile -> profile));
        Map<UUID, SchoolClass> classById = schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .collect(Collectors.toMap(SchoolClass::getId, item -> item));
        Map<UUID, SchoolSubject> subjectById = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .collect(Collectors.toMap(SchoolSubject::getId, item -> item));
        Map<UUID, List<LearnerEnrollment>> enrollmentsByClassId = enrollments.stream().collect(Collectors.groupingBy(LearnerEnrollment::getClassId));
        Map<UUID, SchoolPortalDtos.LearnerListItem> learnerById = learners.stream().collect(Collectors.toMap(SchoolPortalDtos.LearnerListItem::learnerUserId, item -> item));
        Map<UUID, SchoolPortalDtos.CareerReadinessLearnerView> readinessByLearner = readiness.stream().collect(Collectors.toMap(SchoolPortalDtos.CareerReadinessLearnerView::learnerUserId, item -> item, (left, right) -> left));

        return teacherUsers.stream().map(teacher -> {
            List<TeacherAssignment> teacherAssignments = assignments.stream().filter(item -> item.getTeacherUserId().equals(teacher.userId())).toList();
            List<UUID> classIds = teacherAssignments.stream().map(TeacherAssignment::getClassId).distinct().toList();
            List<UUID> subjectIds = teacherAssignments.stream().map(TeacherAssignment::getSubjectId).distinct().toList();
            Set<UUID> learnerIds = classIds.stream()
                    .flatMap(classId -> enrollmentsByClassId.getOrDefault(classId, List.of()).stream())
                    .map(LearnerEnrollment::getLearnerUserId)
                    .collect(Collectors.toSet());
            List<SchoolPortalDtos.LearnerListItem> teacherLearners = learnerIds.stream().map(learnerById::get).filter(Objects::nonNull).toList();
            List<SchoolTask> teacherTasks = tasks.stream().filter(task -> teacher.userId().equals(task.getTeacherUserId())).toList();
            List<UUID> taskIds = teacherTasks.stream().map(SchoolTask::getId).toList();
            List<TaskSubmission> teacherSubmissions = submissions.stream().filter(item -> taskIds.contains(item.getTaskId())).toList();
            List<SubmissionFeedback> teacherFeedback = feedback.stream().filter(item -> teacher.userId().equals(item.getTeacherUserId())).toList();
            List<LearningNote> teacherNotes = notes.stream().filter(item -> teacher.userId().equals(item.getTeacherUserId())).toList();
            List<SchoolPortalDtos.InterventionReportItem> teacherInterventions = interventionItems.stream()
                    .filter(item -> normalize(item.assignedBy()) != null && normalize(item.assignedBy()).equals(normalize(teacher.fullName())))
                    .toList();
            long createdAssignments = teacherTasks.stream().filter(task -> !isAssessmentTask(task)).count();
            long createdAssessments = teacherTasks.stream().filter(this::isAssessmentTask).count();
            long expectedSubmissions = Math.max(1L, teacherLearners.size() * Math.max(1L, teacherTasks.size()));
            BigDecimal submissionRate = BigDecimal.valueOf((teacherSubmissions.size() * 100.0) / expectedSubmissions).setScale(1, RoundingMode.HALF_UP);
            BigDecimal averageLearnerPerformance = averageDecimal(teacherFeedback.stream().filter(SubmissionFeedback::isReleased).map(SubmissionFeedback::getMarksAwarded).filter(Objects::nonNull).toList());
            BigDecimal apsImpact = BigDecimal.valueOf(teacherLearners.stream().mapToLong(SchoolPortalDtos.LearnerListItem::apsPoints).average().orElse(0)).setScale(1, RoundingMode.HALF_UP);
            long careerMapped = teacherLearners.stream().filter(item -> normalize(item.careerGoal()) != null).count();
            long careerReady = learnerIds.stream().map(readinessByLearner::get).filter(Objects::nonNull).filter(SchoolPortalDtos.CareerReadinessLearnerView::aligned).count();
            long bursaryReady = bursaries.stream().map(SchoolPortalDtos.BursaryReadinessItem::learnerUserId).filter(learnerIds::contains).distinct().count();
            long atRisk = teacherLearners.stream().filter(SchoolPortalDtos.LearnerListItem::needsIntervention).count();
            long learnersMeetingAps = teacherLearners.stream().filter(item -> item.apsPoints() >= 28).count();
            long guidanceSessions = teacherInterventions.stream().filter(item -> containsAny(item.supportType(), "career guidance", "vocational", "subject choice")).count();
            long activeInterventions = teacherInterventions.stream().filter(item -> !"COMPLETED".equalsIgnoreCase(item.status())).count();
            long learnersSupported = Math.max(teacherLearners.size(), teacherInterventions.stream().map(SchoolPortalDtos.InterventionReportItem::learnerUserId).distinct().toList().size());
            BigDecimal engagementScore = BigDecimal.valueOf(
                    (classIds.size() * 8L)
                            + (subjectIds.size() * 7L)
                            + (createdAssignments * 10L)
                            + (createdAssessments * 12L)
                            + (teacherNotes.size() * 8L)
                            + Math.min(25L, teacherSubmissions.size())
                            + Math.min(20L, guidanceSessions * 4L)
            ).min(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP);
            SchoolAdminDtos.TeacherAdminItemDto summary = new SchoolAdminDtos.TeacherAdminItemDto(
                    teacher.userId(),
                    teacher.fullName(),
                    teacher.email(),
                    nullSafe(users.get(teacher.userId()) == null ? null : users.get(teacher.userId()).getPhoneNumber(), null),
                    nullSafe(profiles.get(teacher.userId()) == null ? null : profiles.get(teacher.userId()).getEmployeeOrStudentNo(), null),
                    null,
                    teacher.status(),
                    teacher.active(),
                    classIds.size(),
                    subjectIds.size(),
                    teacherLearners.size(),
                    createdAssignments,
                    createdAssessments,
                    teacherNotes.size(),
                    teacherNotes.size(),
                    teacherSubmissions.size(),
                    submissionRate,
                    averageLearnerPerformance,
                    apsImpact,
                    teacherInterventions.size(),
                    guidanceSessions,
                    learnersSupported,
                    careerMapped,
                    careerReady,
                    bursaryReady,
                    atRisk,
                    learnersMeetingAps,
                    activeInterventions,
                    engagementScore
            );
            return new TeacherSnapshot(
                    users.get(teacher.userId()),
                    profiles.get(teacher.userId()),
                    summary,
                    classIds.stream().map(classById::get).filter(Objects::nonNull).map(item -> nullSafe(item.getGrade(), "") + " " + nullSafe(item.getClassName(), "")).toList(),
                    subjectIds.stream().map(subjectById::get).filter(Objects::nonNull).map(SchoolSubject::getSubjectName).toList(),
                    teacherNotes,
                    teacherInterventions.stream().map(this::toAdminInterventionReport).toList(),
                    buildTeacherActivities(teacher, teacherTasks, teacherNotes, teacherInterventions)
            );
        }).toList();
    }

    private List<SchoolAdminDtos.TeacherActivityItemDto> buildTeacherActivities(
            SchoolPortalDtos.SchoolUserAdminView teacher,
            List<SchoolTask> teacherTasks,
            List<LearningNote> teacherNotes,
            List<SchoolPortalDtos.InterventionReportItem> teacherInterventions
    ) {
        List<SchoolAdminDtos.TeacherActivityItemDto> items = new ArrayList<>();
        teacherNotes.forEach(note -> items.add(new SchoolAdminDtos.TeacherActivityItemDto(teacher.userId(), teacher.fullName(), "Uploaded " + note.getTitle(), inferResourceCategory(note) + " uploaded", "RESOURCE", note.getCreatedAt())));
        teacherTasks.forEach(task -> items.add(new SchoolAdminDtos.TeacherActivityItemDto(teacher.userId(), teacher.fullName(), "Created " + task.getTitle(), task.getTaskType(), isAssessmentTask(task) ? "ASSESSMENT" : "ASSIGNMENT", task.getCreatedAt())));
        teacherInterventions.forEach(item -> items.add(new SchoolAdminDtos.TeacherActivityItemDto(teacher.userId(), teacher.fullName(), "Added intervention note", item.supportType() + " | " + item.notes(), "INTERVENTION", item.updatedAt())));
        return items.stream().sorted(Comparator.comparing(SchoolAdminDtos.TeacherActivityItemDto::occurredAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(20).toList();
    }

    private List<SchoolAdminDtos.InsightItemDto> toInsightItems(List<SchoolPortalDtos.TopBreakdownItem> items, String suffix, String severity) {
        return items.stream()
                .map(item -> new SchoolAdminDtos.InsightItemDto(item.label(), item.value() + " " + suffix, severity))
                .toList();
    }

    private BigDecimal averageDecimal(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long value, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf((value * 100.0) / total).setScale(1, RoundingMode.HALF_UP);
    }

    private boolean isAssessmentTask(SchoolTask task) {
        String type = normalize(task.getTaskType());
        return type != null && containsAny(type, "assessment", "exam", "test", "quiz", "sba");
    }

    private boolean containsAny(String value, String... needles) {
        String normalized = normalize(value);
        if (normalized == null) {
            return false;
        }
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String workloadBand(SchoolAdminDtos.TeacherAdminItemDto item) {
        if (item.learnerCount() >= 120 || item.assignedClasses() >= 5 || item.createdAssessments() >= 8) {
            return "Overloaded";
        }
        if (item.assignedClasses() <= 1 && item.learnerCount() < 20) {
            return "Underutilised";
        }
        return "Balanced";
    }

    private List<SchoolAdminDtos.InsightItemDto> missingCoverageAlerts(UUID schoolId) {
        List<SchoolAdminDtos.InsightItemDto> alerts = new ArrayList<>();
        List<SchoolClass> classes = schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId);
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findAll().stream()
                .filter(item -> schoolId.equals(item.getSchoolId()) && item.isActive())
                .toList();
        classes.stream()
                .filter(item -> assignments.stream().noneMatch(assignment -> assignment.getClassId().equals(item.getId())))
                .limit(4)
                .forEach(item -> alerts.add(new SchoolAdminDtos.InsightItemDto("Grade staffing gap", item.getGrade() + " " + item.getClassName() + " has no active teacher allocation.", "warning")));
        return alerts;
    }

    private String inferResourceCategory(LearningNote note) {
        String text = (nullSafe(note.getTitle(), "") + " " + nullSafe(note.getNoteText(), "")).toLowerCase(Locale.ROOT);
        if (text.contains("lesson plan")) return "Lesson Plans";
        if (text.contains("study guide")) return "Study Guides";
        if (text.contains("worksheet")) return "Worksheets";
        if (text.contains("past paper")) return "Past Papers";
        if (text.contains("marking guide")) return "Marking Guides";
        if (text.contains("career")) return "Career Guidance Resources";
        return "Notes";
    }

    private List<SchoolAdminDtos.TeacherResourceCategoryDto> inferResourceCategories(List<LearningNote> notes) {
        Map<String, Long> counts = notes.stream()
                .collect(Collectors.groupingBy(this::inferResourceCategory, LinkedHashMap::new, Collectors.counting()));
        if (counts.isEmpty()) {
            return List.of(new SchoolAdminDtos.TeacherResourceCategoryDto("Notes", 0, 0));
        }
        return counts.entrySet().stream()
                .map(entry -> new SchoolAdminDtos.TeacherResourceCategoryDto(entry.getKey(), entry.getValue(), 0))
                .toList();
    }

    private List<SchoolAdminDtos.TeacherApprovalHistoryDto> approvalHistoryForTeacher(UUID teacherId) {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(log -> teacherId.equals(log.getEntityId()))
                .map(log -> new SchoolAdminDtos.TeacherApprovalHistoryDto(log.getAction(), log.getCreatedAt()))
                .toList();
    }

    private String dominantSubject(List<String> subjects) {
        return subjects.isEmpty() ? "teacher-led" : subjects.getFirst();
    }

    private String dominantCareerInterest(List<SchoolAdminDtos.LearnerAdminItemDto> learners) {
        return learners.stream()
                .map(SchoolAdminDtos.LearnerAdminItemDto::careerGoal)
                .filter(value -> normalize(value) != null)
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey() + " interest is highest at " + entry.getValue() + " learners.")
                .orElse("Career interest distribution will appear as learner pathway selections grow.");
    }

    private List<SchoolAdminDtos.InsightItemDto> rankClasses(
            List<ClassSnapshot> snapshots,
            Comparator<ClassSnapshot> comparator,
            java.util.function.Function<ClassSnapshot, String> detail,
            String severity
    ) {
        return snapshots.stream()
                .sorted(comparator)
                .limit(5)
                .map(item -> new SchoolAdminDtos.InsightItemDto(item.summary().grade() + item.summary().className(), detail.apply(item), severity))
                .toList();
    }

    private List<SchoolAdminDtos.TrendPointDto> apsDistributionForClass(List<SchoolAdminDtos.LearnerAdminItemDto> learners) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("0-19", 0L);
        counts.put("20-27", 0L);
        counts.put("28-34", 0L);
        counts.put("35+", 0L);
        learners.forEach(learner -> {
            String band = learner.apsPoints() < 20 ? "0-19" : learner.apsPoints() < 28 ? "20-27" : learner.apsPoints() < 35 ? "28-34" : "35+";
            counts.put(band, counts.get(band) + 1);
        });
        return counts.entrySet().stream()
                .map(entry -> new SchoolAdminDtos.TrendPointDto(entry.getKey(), BigDecimal.valueOf(entry.getValue()), entry.getKey().equals("0-19") ? "critical" : entry.getKey().equals("20-27") ? "warning" : "positive"))
                .toList();
    }

    private List<SchoolAdminDtos.TrendPointDto> subjectPerformanceForClass(ClassSnapshot snapshot) {
        Map<String, List<Long>> byCareer = snapshot.learners().stream()
                .filter(item -> normalize(item.careerGoal()) != null)
                .collect(Collectors.groupingBy(SchoolAdminDtos.LearnerAdminItemDto::careerGoal, LinkedHashMap::new, Collectors.mapping(SchoolAdminDtos.LearnerAdminItemDto::apsPoints, Collectors.toList())));
        if (byCareer.isEmpty()) {
            return List.of(new SchoolAdminDtos.TrendPointDto("APS", BigDecimal.valueOf(snapshot.summary().averageAps()), snapshot.summary().averageAps() >= 28 ? "positive" : "warning"));
        }
        return byCareer.entrySet().stream().limit(6)
                .map(entry -> new SchoolAdminDtos.TrendPointDto(entry.getKey(), BigDecimal.valueOf(entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0)).setScale(1, RoundingMode.HALF_UP), "neutral"))
                .toList();
    }

    private List<SchoolAdminDtos.SchoolAdminTimelineDto> buildClassTimeline(ClassSnapshot snapshot, SchoolAdminDtos.ClassAnalyticsResponse analytics) {
        List<SchoolAdminDtos.SchoolAdminTimelineDto> items = new ArrayList<>();
        items.add(new SchoolAdminDtos.SchoolAdminTimelineDto("Class APS updated", "Average APS now " + snapshot.summary().averageAps(), OffsetDateTime.now().toString(), "APS"));
        snapshot.interventions().stream().limit(5)
                .forEach(item -> items.add(new SchoolAdminDtos.SchoolAdminTimelineDto("Intervention", item.learnerName() + " | " + item.supportType() + " | " + item.status(), item.updatedAt(), "INTERVENTION")));
        analytics.subjectPerformanceTrends().stream().limit(4)
                .forEach(item -> items.add(new SchoolAdminDtos.SchoolAdminTimelineDto("Performance trend", item.label() + " " + item.value(), OffsetDateTime.now().toString(), "PERFORMANCE")));
        return items;
    }

    private String topCareerReadinessHeadline(SchoolPortalDtos.CareerReadinessResponse career) {
        SchoolPortalDtos.TopBreakdownItem topInterest = career.topCareerInterests().stream().findFirst().orElse(null);
        long notAligned = career.learners().stream().filter(item -> !item.aligned()).count();
        if (topInterest == null) {
            return "Career readiness intelligence will strengthen as learners select pathways and upload academic evidence.";
        }
        return topInterest.label() + " is the top selected pathway, and " + notAligned + " learners still need subject or pathway support.";
    }

    private String learnerReadinessStatus(SchoolPortalDtos.LearnerListItem item) {
        return learnerReadinessStatus(item.apsPoints(), item.profileComplete(), item.needsIntervention());
    }

    private String learnerReadinessStatus(long apsPoints, boolean profileComplete, boolean needsIntervention) {
        if (needsIntervention) {
            return "Intervention required";
        }
        if (!profileComplete) {
            return "Profile incomplete";
        }
        if (apsPoints >= 30) {
            return "On track";
        }
        return "Emerging readiness";
    }

    private String teacherActivitySummary(List<SchoolAdminDtos.TeacherAdminItemDto> items) {
        if (items.isEmpty()) {
            return "No teacher activity yet";
        }
        BigDecimal average = items.stream()
                .map(SchoolAdminDtos.TeacherAdminItemDto::engagementScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(items.size()), 1, RoundingMode.HALF_UP);
        return average.toPlainString();
    }

    private List<SchoolAdminDtos.InsightItemDto> teacherActivityHighlights(List<SchoolAdminDtos.TeacherAdminItemDto> items) {
        return items.stream()
                .sorted(Comparator.comparing(SchoolAdminDtos.TeacherAdminItemDto::engagementScore).reversed())
                .limit(6)
                .map(item -> new SchoolAdminDtos.InsightItemDto(item.fullName(), item.createdAssignments() + " assignments, " + item.uploadedNotes() + " resources, engagement " + item.engagementScore(), item.engagementScore().compareTo(new BigDecimal("45")) >= 0 ? "positive" : "warning"))
                .toList();
    }

    private List<SchoolAdminDtos.SchoolAdminNoteDto> teacherNotes(UUID schoolId, UUID learnerUserId) {
        String learnerClass = learnerClassLabel(schoolId, learnerUserId);
        if (normalize(learnerClass) == null) {
            return List.of();
        }
        String[] parts = learnerClass.split(" ", 2);
        if (parts.length < 2) {
            return List.of();
        }
        SchoolClass schoolClass = schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .filter(item -> parts[0].equalsIgnoreCase(nullSafe(item.getGrade(), "")) && parts[1].equalsIgnoreCase(nullSafe(item.getClassName(), "")))
                .findFirst()
                .orElse(null);
        if (schoolClass == null) {
            return List.of();
        }
        Set<UUID> teacherIds = teacherAssignmentRepository.findAll().stream()
                .filter(item -> schoolId.equals(item.getSchoolId()) && item.isActive() && schoolClass.getId().equals(item.getClassId()))
                .map(TeacherAssignment::getTeacherUserId)
                .collect(Collectors.toSet());
        Map<UUID, User> users = userRepository.findAllById(teacherIds).stream().collect(Collectors.toMap(User::getId, user -> user));
        return learningNoteRepository.findAll().stream()
                .filter(note -> schoolId.equals(note.getSchoolId()) && schoolClass.getId().equals(note.getClassId()) && teacherIds.contains(note.getTeacherUserId()))
                .sorted(Comparator.comparing(LearningNote::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(note -> {
                    User teacher = users.get(note.getTeacherUserId());
                    return new SchoolAdminDtos.SchoolAdminNoteDto(
                            note.getTitle(),
                            nullSafe(note.getNoteText(), note.getPdfUrl()),
                            teacher == null ? "Teacher" : (nullSafe(teacher.getFirstName(), "") + " " + nullSafe(teacher.getLastName(), "")).trim(),
                            note.getCreatedAt() == null ? null : note.getCreatedAt().toString()
                    );
                })
                .toList();
    }

    private SchoolAdminDtos.SchoolAdminInterventionDto toAdminIntervention(SchoolPortalDtos.InterventionSummaryView item) {
        return new SchoolAdminDtos.SchoolAdminInterventionDto(
                item.interventionId(),
                item.status(),
                item.priority(),
                item.supportType(),
                item.notes(),
                item.followUpDate() == null ? null : item.followUpDate().toString(),
                item.updatedAt() == null ? null : item.updatedAt().toString()
        );
    }

    private SchoolAdminDtos.SchoolAdminInterventionReportDto toAdminInterventionReport(SchoolPortalDtos.InterventionReportItem item) {
        return new SchoolAdminDtos.SchoolAdminInterventionReportDto(
                item.interventionId(),
                item.learnerUserId(),
                item.learnerName(),
                item.assignedBy(),
                item.supportType(),
                item.priority(),
                item.status(),
                item.notes(),
                item.followUpDate() == null ? null : item.followUpDate().toString(),
                item.updatedAt() == null ? null : item.updatedAt().toString()
        );
    }

    private List<SchoolAdminDtos.TrendPointDto> toTrendPoints(List<SchoolPortalDtos.PerformanceBandItem> items) {
        return items.stream()
                .map(item -> new SchoolAdminDtos.TrendPointDto(item.label(), item.value(), item.tone()))
                .toList();
    }

    private List<SchoolAdminDtos.DistributionItemDto> toDistribution(List<SchoolPortalDtos.PerformanceBandItem> items) {
        return items.stream()
                .map(item -> new SchoolAdminDtos.DistributionItemDto(item.label(), item.value().setScale(0, RoundingMode.HALF_UP).longValue(), item.tone()))
                .toList();
    }

    private List<SchoolAdminDtos.DistributionItemDto> apsBands(List<SchoolPortalDtos.LearnerListItem> learners) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("0-19", 0L);
        counts.put("20-27", 0L);
        counts.put("28-34", 0L);
        counts.put("35+", 0L);
        for (SchoolPortalDtos.LearnerListItem learner : learners) {
            String band = learner.apsPoints() < 20 ? "0-19" : learner.apsPoints() < 28 ? "20-27" : learner.apsPoints() < 35 ? "28-34" : "35+";
            counts.put(band, counts.get(band) + 1);
        }
        return counts.entrySet().stream().map(entry -> new SchoolAdminDtos.DistributionItemDto(entry.getKey(), entry.getValue(), entry.getKey().equals("0-19") ? "critical" : entry.getKey().equals("20-27") ? "warning" : "positive")).toList();
    }

    private List<SchoolAdminDtos.DistributionItemDto> reportUploadProgress(UUID schoolId) {
        long notes = learningNoteRepository.findAll().stream().filter(note -> schoolId.equals(note.getSchoolId())).count();
        long submissions = taskSubmissionRepository.findAll().stream().filter(submission -> belongsToSchool(submission.getTaskId(), schoolId)).count();
        long tasks = schoolTaskRepository.findAll().stream().filter(task -> schoolId.equals(task.getSchoolId())).count();
        return List.of(
                new SchoolAdminDtos.DistributionItemDto("Teacher notes", notes, notes > 0 ? "positive" : "warning"),
                new SchoolAdminDtos.DistributionItemDto("Learner submissions", submissions, submissions > 0 ? "positive" : "warning"),
                new SchoolAdminDtos.DistributionItemDto("Assessment artifacts", tasks, tasks > 0 ? "positive" : "warning")
        );
    }

    private List<SchoolAdminDtos.InsightItemDto> districtSummary(UUID schoolId, List<SchoolPortalDtos.LearnerListItem> learners) {
        long teachers = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, SchoolAccessService.ROLE_TEACHER).size();
        long learnersAtRisk = learners.stream().filter(SchoolPortalDtos.LearnerListItem::needsIntervention).count();
        long completeProfiles = learners.stream().filter(SchoolPortalDtos.LearnerListItem::profileComplete).count();
        return List.of(
                new SchoolAdminDtos.InsightItemDto("School roll", learners.size() + " learners | " + teachers + " teachers", "neutral"),
                new SchoolAdminDtos.InsightItemDto("Profile completion", completeProfiles + " learner profiles complete", completeProfiles == learners.size() && !learners.isEmpty() ? "positive" : "warning"),
                new SchoolAdminDtos.InsightItemDto("Intervention load", learnersAtRisk + " learners currently flagged", learnersAtRisk > 0 ? "warning" : "positive")
        );
    }

    private BigDecimal completionRate(UUID schoolId, List<SchoolTask> assessments) {
        if (assessments.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        long released = assessments.stream()
                .map(SchoolTask::getId)
                .filter(taskId -> taskSubmissionRepository.findByTaskId(taskId).stream()
                        .anyMatch(submission -> submissionFeedbackRepository.findBySubmissionId(submission.getId()).map(feedback -> feedback.isReleased() && belongsToSchool(taskId, schoolId)).orElse(false)))
                .count();
        return BigDecimal.valueOf((released * 100.0) / assessments.size()).setScale(1, RoundingMode.HALF_UP);
    }

    private boolean belongsToSchool(UUID taskId, UUID schoolId) {
        return schoolTaskRepository.findById(taskId).map(task -> schoolId.equals(task.getSchoolId())).orElse(false);
    }

    private String describeSubscription(SubscriptionRecord subscription) {
        String code = nullSafe(subscription.getPlanCode(), "UNKNOWN");
        String status = nullSafe(subscription.getStatus(), "UNKNOWN");
        return code + " / " + status;
    }

    private void linkLearnerToClassAndSubject(UUID schoolId, UUID learnerUserId, String grade, String className) {
        String normalizedGrade = normalize(grade);
        String normalizedClass = normalize(className);
        if (normalizedGrade == null || normalizedClass == null) {
            return;
        }
        SchoolClass schoolClass = schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .filter(item -> normalizedGrade.equals(normalize(item.getGrade())) && normalizedClass.equals(normalize(item.getClassName())))
                .findFirst()
                .orElse(null);
        if (schoolClass == null) {
            return;
        }
        SchoolSubject subject = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream().findFirst().orElse(null);
        if (subject == null) {
            return;
        }
        boolean alreadyLinked = learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndActiveTrue(schoolId, learnerUserId).stream()
                .anyMatch(item -> item.getClassId().equals(schoolClass.getId()) && item.getSubjectId().equals(subject.getId()));
        if (!alreadyLinked) {
            schoolService.enrollLearner(schoolId, new SchoolPortalDtos.LearnerEnrollmentRequest(learnerUserId, schoolClass.getId(), subject.getId()));
        }
    }

    private String learnerClassLabel(UUID schoolId, UUID learnerUserId) {
        LearnerEnrollment enrollment = learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndActiveTrue(schoolId, learnerUserId).stream().findFirst().orElse(null);
        if (enrollment == null) {
            return "";
        }
        SchoolClass schoolClass = schoolClassRepository.findById(enrollment.getClassId()).orElse(null);
        return schoolClass == null ? "" : nullSafe(schoolClass.getGrade(), "") + " " + nullSafe(schoolClass.getClassName(), "");
    }

    private SchoolAdminDtos.AnnouncementItemDto toAnnouncementDto(SchoolAnnouncement item) {
        return new SchoolAdminDtos.AnnouncementItemDto(item.getId(), item.getAudience(), item.getTitle(), item.getMessage(), item.getStatus(), item.getSentAt(), item.getCreatedAt());
    }

    private List<UUID> announcementRecipients(UUID schoolId, String audience) {
        Set<String> roles = switch (audience == null ? "" : audience.toUpperCase(Locale.ROOT)) {
            case "TEACHERS" -> Set.of(SchoolAccessService.ROLE_TEACHER);
            case "LEARNERS" -> Set.of(SchoolAccessService.ROLE_SCHOOL_STUDENT);
            default -> Set.of(SchoolAccessService.ROLE_TEACHER, SchoolAccessService.ROLE_SCHOOL_STUDENT);
        };
        return schoolUserProfileRepository.findBySchoolIdAndDeletedFalse(schoolId).stream()
                .filter(profile -> roles.contains(profile.getRoleName()))
                .map(SchoolUserProfile::getUserId)
                .toList();
    }

    private SchoolAdminDtos.SupportRequestItemDto toSupportRequestDto(SchoolSupportRequest item) {
        return new SchoolAdminDtos.SupportRequestItemDto(item.getId(), item.getCategory(), item.getTitle(), item.getMessage(), item.getStatus(), item.getPriority(), item.getCreatedAt());
    }

    private SchoolAdminDtos.MetricCardDto metric(String label, long value, String helperText, String tone) {
        return new SchoolAdminDtos.MetricCardDto(label, String.valueOf(value), helperText, tone);
    }

    private SchoolAdminDtos.MetricCardDto metric(String label, String value, String helperText, String tone) {
        return new SchoolAdminDtos.MetricCardDto(label, value, helperText, tone);
    }

    private String synthesizeUsername(String base) {
        String normalized = normalize(base);
        String compact = normalized == null ? "learner" : normalized.replace(" ", ".");
        return compact.replaceAll("[^a-z0-9.]", "");
    }

    private String generatePassword(String firstName, String lastName) {
        String prefix = (nullSafe(firstName, "Edu").substring(0, Math.min(3, nullSafe(firstName, "Edu").length()))
                + nullSafe(lastName, "Rite").substring(0, Math.min(2, nullSafe(lastName, "Rite").length()))).replaceAll("[^A-Za-z]", "E");
        return prefix + "@" + UUID.randomUUID().toString().substring(0, 6);
    }

    private String normalizePriority(String value) {
        String normalized = normalize(value);
        return normalized == null ? "MEDIUM" : normalized.toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void writeAudit(UUID actorId, String action, UUID entityId, Map<String, Object> details) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setAction(action);
        log.setEntityType("SCHOOL_ADMIN");
        log.setEntityId(entityId);
        log.setDetails(objectMapper.valueToTree(details));
        auditLogRepository.save(log);
    }
}
