package com.edurite.school.service;

import com.edurite.admin.entity.AuditLog;
import com.edurite.admin.repository.AuditLogRepository;
import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.compliance.repository.ConsentRecordRepository;
import com.edurite.course.entity.Course;
import com.edurite.course.repository.CourseRepository;
import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.portal.entity.LearnerEnrollment;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolClass;
import com.edurite.school.portal.entity.SchoolIntervention;
import com.edurite.school.portal.entity.SchoolSubject;
import com.edurite.school.portal.entity.SchoolTask;
import com.edurite.school.portal.entity.SubmissionFeedback;
import com.edurite.school.portal.entity.TaskSubmission;
import com.edurite.school.portal.entity.TeacherAssignment;
import com.edurite.school.portal.repository.LearnerEnrollmentRepository;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolInterventionRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.SchoolTaskRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.portal.repository.SubmissionFeedbackRepository;
import com.edurite.school.portal.repository.TaskSubmissionRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.student.dto.StudentSubjectAchievementDto;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.SavedBursaryRepository;
import com.edurite.student.repository.SavedCareerRepository;
import com.edurite.student.repository.StudentProfileRepository;
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
import java.util.HashMap;
import java.util.HashSet;
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

@Service
public class SchoolPortalAnalyticsService {

    private static final TypeReference<List<StudentSubjectAchievementDto>> SUBJECTS_TYPE = new TypeReference<>() { };
    private static final Set<String> CORE_CAREER_SUBJECTS = Set.of(
            "mathematics", "physical sciences", "life sciences", "english", "home language", "accounting", "geography"
    );

    private final SchoolRepository schoolRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final LearnerEnrollmentRepository learnerEnrollmentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolSubjectRepository schoolSubjectRepository;
    private final SchoolTaskRepository schoolTaskRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final SubmissionFeedbackRepository submissionFeedbackRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final SavedCareerRepository savedCareerRepository;
    private final SavedBursaryRepository savedBursaryRepository;
    private final BursaryRepository bursaryRepository;
    private final CourseRepository courseRepository;
    private final SchoolInterventionRepository schoolInterventionRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public SchoolPortalAnalyticsService(
            SchoolRepository schoolRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            TeacherAssignmentRepository teacherAssignmentRepository,
            LearnerEnrollmentRepository learnerEnrollmentRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolSubjectRepository schoolSubjectRepository,
            SchoolTaskRepository schoolTaskRepository,
            TaskSubmissionRepository taskSubmissionRepository,
            SubmissionFeedbackRepository submissionFeedbackRepository,
            StudentProfileRepository studentProfileRepository,
            UserRepository userRepository,
            SavedCareerRepository savedCareerRepository,
            SavedBursaryRepository savedBursaryRepository,
            BursaryRepository bursaryRepository,
            CourseRepository courseRepository,
            SchoolInterventionRepository schoolInterventionRepository,
            ConsentRecordRepository consentRecordRepository,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper
    ) {
        this.schoolRepository = schoolRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.learnerEnrollmentRepository = learnerEnrollmentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolSubjectRepository = schoolSubjectRepository;
        this.schoolTaskRepository = schoolTaskRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.submissionFeedbackRepository = submissionFeedbackRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.savedCareerRepository = savedCareerRepository;
        this.savedBursaryRepository = savedBursaryRepository;
        this.bursaryRepository = bursaryRepository;
        this.courseRepository = courseRepository;
        this.schoolInterventionRepository = schoolInterventionRepository;
        this.consentRecordRepository = consentRecordRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.DashboardSnapshot dashboard(UUID schoolId, UUID viewerUserId, String roleName) {
        List<LearnerContext> learners = learnerContexts(schoolId, viewerUserId, roleName);
        List<SchoolPortalDtos.PortalMetric> metrics = List.of(
                metric("Total learners", learners.size(), "School roll", "neutral"),
                metric("Active learners", learners.stream().filter(LearnerContext::active).count(), "Current school users", "positive"),
                metric("Reports uploaded", reportUploadCount(schoolId), "Tasks, notes, submissions", "neutral"),
                metric("Complete profiles", learners.stream().filter(LearnerContext::profileComplete).count(), "Student profiles", "positive"),
                metric("Career-mapped learners", learners.stream().filter(LearnerContext::careerMapped).count(), "Career goal or saved career", "positive"),
                metric("Learners needing intervention", learners.stream().filter(LearnerContext::needsIntervention).count(), "At-risk or flagged", "warning"),
                metric("Learners eligible for bursaries", learners.stream().filter(LearnerContext::bursaryEligible).count(), "Has at least one match", "positive")
        );
        return new SchoolPortalDtos.DashboardSnapshot(
                roleName,
                schoolId,
                metrics,
                topBreakdown(learners.stream().map(LearnerContext::topCareerInterest).toList(), 5),
                topBreakdown(learners.stream().flatMap(context -> context.riskSubjects().stream()).toList(), 5)
        );
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.LearnerListResponse learners(UUID schoolId, UUID viewerUserId, String roleName, String search, String grade, String className) {
        String normalizedSearch = normalize(search);
        String normalizedGrade = normalize(grade);
        String normalizedClass = normalize(className);
        List<SchoolPortalDtos.LearnerListItem> items = learnerContexts(schoolId, viewerUserId, roleName).stream()
                .filter(context -> normalizedSearch == null || contains(context.learnerName(), normalizedSearch) || contains(context.email(), normalizedSearch))
                .filter(context -> normalizedGrade == null || contains(context.grade(), normalizedGrade))
                .filter(context -> normalizedClass == null || contains(context.className(), normalizedClass))
                .map(this::toLearnerListItem)
                .sorted(Comparator.comparing(SchoolPortalDtos.LearnerListItem::learnerName))
                .toList();
        return new SchoolPortalDtos.LearnerListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.LearnerProfileResponse learnerProfile(UUID schoolId, UUID viewerUserId, String roleName, UUID learnerUserId) {
        LearnerContext context = learnerContexts(schoolId, viewerUserId, roleName).stream()
                .filter(item -> item.learnerUserId().equals(learnerUserId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Learner not found for this portal view."));
        return new SchoolPortalDtos.LearnerProfileResponse(
                context.learnerUserId(),
                context.studentProfileId(),
                context.learnerName(),
                context.email(),
                context.grade(),
                context.className(),
                context.teacherName(),
                context.profileComplete(),
                context.apsPoints(),
                context.careerGoal(),
                context.qualificationLevel(),
                context.interests(),
                context.skills(),
                context.popiaStatus(),
                context.subjects(),
                context.courseEligibility(),
                context.bursaryMatches(),
                context.interventions().stream().map(this::toInterventionSummary).toList(),
                context.timeline()
        );
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.AcademicInsightsResponse academicInsights(UUID schoolId, UUID viewerUserId, String roleName) {
        List<LearnerContext> learners = learnerContexts(schoolId, viewerUserId, roleName);
        return new SchoolPortalDtos.AcademicInsightsResponse(
                averageBy(learners, LearnerContext::grade, LearnerContext::averageMarkPercent),
                averageBySubjects(learners),
                averageBy(learners, LearnerContext::className, LearnerContext::averageMarkPercent),
                learners.stream().filter(LearnerContext::needsIntervention).map(this::toLearnerListItem).toList(),
                topBreakdown(learners.stream().flatMap(context -> context.subjects().stream())
                        .filter(subject -> subject.risk() && CORE_CAREER_SUBJECTS.contains(normalize(subject.subjectName())))
                        .map(SchoolPortalDtos.SubjectMarkView::subjectName)
                        .toList(), 6)
        );
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.CareerReadinessResponse careerReadiness(UUID schoolId, UUID viewerUserId, String roleName) {
        List<LearnerContext> learners = learnerContexts(schoolId, viewerUserId, roleName);
        return new SchoolPortalDtos.CareerReadinessResponse(
                learners.stream().map(context -> new SchoolPortalDtos.CareerReadinessLearnerView(
                        context.learnerUserId(),
                        context.learnerName(),
                        context.careerGoal(),
                        context.alignedToCareerPath(),
                        context.apsPoints(),
                        context.readinessGap(),
                        context.alternativePathway()
                )).toList(),
                topBreakdown(learners.stream().map(LearnerContext::topCareerInterest).toList(), 6),
                topBreakdown(learners.stream().map(LearnerContext::readinessGap).toList(), 6),
                topBreakdown(learners.stream().map(LearnerContext::alternativePathway).toList(), 6)
        );
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.BursaryReadinessResponse bursaryReadiness(UUID schoolId, UUID viewerUserId, String roleName) {
        List<LearnerContext> learners = learnerContexts(schoolId, viewerUserId, roleName);
        List<SchoolPortalDtos.BursaryReadinessItem> matches = learners.stream()
                .flatMap(context -> context.bursaryMatches().stream().map(match -> new SchoolPortalDtos.BursaryReadinessItem(
                        context.learnerUserId(),
                        context.learnerName(),
                        match.title(),
                        match.provider(),
                        match.deadline(),
                        match.missingRequirements(),
                        buildChecklist(context, match)
                )))
                .sorted(Comparator.comparing(item -> item.deadline() == null ? LocalDate.MAX : item.deadline()))
                .toList();
        List<SchoolPortalDtos.BursaryReadinessItem> deadlineAlerts = matches.stream()
                .filter(item -> item.deadline() != null && !item.deadline().isAfter(LocalDate.now().plusDays(30)))
                .toList();
        return new SchoolPortalDtos.BursaryReadinessResponse(
                matches,
                deadlineAlerts,
                topBreakdown(matches.stream().map(SchoolPortalDtos.BursaryReadinessItem::missingRequirements).toList(), 8)
        );
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.InterventionReportItem> interventions(UUID schoolId, UUID viewerUserId, String roleName) {
        Set<UUID> allowedLearners = learnerContexts(schoolId, viewerUserId, roleName).stream()
                .map(LearnerContext::learnerUserId)
                .collect(Collectors.toSet());
        return schoolInterventionRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .filter(item -> allowedLearners.contains(item.getLearnerUserId()))
                .sorted(Comparator.comparing(SchoolIntervention::getUpdatedAt).reversed())
                .map(this::toInterventionReportItem)
                .toList();
    }

    @Transactional
    public SchoolPortalDtos.InterventionReportItem createIntervention(UUID schoolId, UUID actorUserId, String roleName, SchoolPortalDtos.InterventionRequest request) {
        ensureLearnerVisible(schoolId, actorUserId, roleName, request.learnerUserId());
        SchoolIntervention intervention = new SchoolIntervention();
        intervention.setSchoolId(schoolId);
        intervention.setLearnerUserId(request.learnerUserId());
        intervention.setAssignedByUserId(actorUserId);
        intervention.setSupportType(request.supportType().trim());
        intervention.setPriority(request.priority().trim().toUpperCase(Locale.ROOT));
        intervention.setStatus(normalizeStatus(request.status()));
        intervention.setNotes(request.notes().trim());
        intervention.setFollowUpDate(request.followUpDate());
        SchoolIntervention saved = schoolInterventionRepository.save(intervention);
        writeAudit(actorUserId, "SCHOOL_INTERVENTION_CREATED", saved.getId(), Map.of(
                "schoolId", schoolId,
                "learnerUserId", request.learnerUserId(),
                "roleName", roleName
        ));
        return toInterventionReportItem(saved);
    }

    @Transactional
    public SchoolPortalDtos.InterventionReportItem updateIntervention(UUID schoolId, UUID actorUserId, String roleName, UUID interventionId, SchoolPortalDtos.InterventionProgressRequest request) {
        SchoolIntervention intervention = schoolInterventionRepository.findById(interventionId)
                .filter(item -> item.getSchoolId().equals(schoolId) && item.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Intervention not found."));
        ensureLearnerVisible(schoolId, actorUserId, roleName, intervention.getLearnerUserId());
        intervention.setStatus(normalizeStatus(request.status()));
        intervention.setNotes(request.notes().trim());
        intervention.setFollowUpDate(request.followUpDate());
        SchoolIntervention saved = schoolInterventionRepository.save(intervention);
        writeAudit(actorUserId, "SCHOOL_INTERVENTION_UPDATED", saved.getId(), Map.of(
                "schoolId", schoolId,
                "learnerUserId", intervention.getLearnerUserId(),
                "status", saved.getStatus()
        ));
        return toInterventionReportItem(saved);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.ReportExportResponse exportReport(UUID schoolId, UUID viewerUserId, String roleName, String reportType, String format) {
        String normalizedFormat = normalize(format) == null ? "csv" : normalize(format);
        String normalizedReport = normalize(reportType) == null ? "whole-school-readiness" : normalize(reportType);
        String payload = switch (normalizedReport) {
            case "grade-readiness" -> gradeReadinessReport(schoolId, viewerUserId, roleName);
            case "subject-gap", "subject-gap-report" -> subjectGapReport(schoolId, viewerUserId, roleName);
            case "career-interest", "career-interest-report" -> careerInterestReport(schoolId, viewerUserId, roleName);
            case "bursary-readiness", "bursary-readiness-report" -> bursaryReadinessReport(schoolId, viewerUserId, roleName);
            case "at-risk-learner", "at-risk-learner-report" -> atRiskLearnerReport(schoolId, viewerUserId, roleName);
            default -> wholeSchoolReadinessReport(schoolId, viewerUserId, roleName);
        };
        String fileName = normalizedReport + "." + ("pdf".equals(normalizedFormat) ? "pdf" : "csv");
        String contentType = "pdf".equals(normalizedFormat) ? "application/pdf" : "text/csv";
        byte[] bytes = "pdf".equals(normalizedFormat)
                ? brandedPdf(schoolId, viewerUserId, roleName, normalizedReport, payload)
                : payload.getBytes(StandardCharsets.UTF_8);
        return new SchoolPortalDtos.ReportExportResponse(fileName, contentType, Base64.getEncoder().encodeToString(bytes));
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.PortalSettingsResponse settings(UUID schoolId) {
        School school = schoolRepository.findById(schoolId).orElseThrow();
        Set<UUID> schoolUsers = schoolUserProfileRepository.findBySchoolIdAndDeletedFalse(schoolId).stream()
                .map(profile -> profile.getUserId())
                .collect(Collectors.toSet());
        List<Map<String, Object>> audits = auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(log -> schoolUsers.contains(log.getActorId()))
                .limit(15)
                .map(log -> Map.<String, Object>of(
                        "action", log.getAction(),
                        "entityType", log.getEntityType(),
                        "entityId", log.getEntityId(),
                        "createdAt", log.getCreatedAt()
                ))
                .toList();
        return new SchoolPortalDtos.PortalSettingsResponse(
                school.getSchoolName(),
                school.getDistrict(),
                school.getProvince(),
                schoolUserProfileRepository.findBySchoolIdAndDeletedFalse(schoolId).stream().map(profile -> profile.getRoleName()).distinct().sorted().toList(),
                audits
        );
    }

    private List<LearnerContext> learnerContexts(UUID schoolId, UUID viewerUserId, String roleName) {
        List<UUID> learnerIds = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, SchoolAccessService.ROLE_SCHOOL_STUDENT).stream()
                .filter(profile -> profile.isActive() && !profile.isDeleted())
                .map(profile -> profile.getUserId())
                .toList();
        Set<UUID> visibleLearners = SchoolAccessService.ROLE_TEACHER.equals(roleName)
                ? new HashSet<>(assignedLearnerIds(schoolId, viewerUserId))
                : new HashSet<>(learnerIds);
        Map<UUID, User> usersById = userRepository.findAllById(learnerIds).stream().collect(Collectors.toMap(User::getId, user -> user));
        Map<UUID, StudentProfile> profilesByUserId = studentProfileRepository.findAll().stream()
                .filter(profile -> visibleLearners.contains(profile.getUserId()))
                .collect(Collectors.toMap(StudentProfile::getUserId, profile -> profile, (left, right) -> left));
        Map<UUID, List<LearnerEnrollment>> enrollmentsByLearner = learnerEnrollmentRepository.findAll().stream()
                .filter(item -> item.getSchoolId().equals(schoolId) && item.isActive() && visibleLearners.contains(item.getLearnerUserId()))
                .collect(Collectors.groupingBy(LearnerEnrollment::getLearnerUserId));
        Map<UUID, SchoolClass> classesById = schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId).stream().collect(Collectors.toMap(SchoolClass::getId, item -> item));
        Map<UUID, SchoolSubject> subjectsById = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream().collect(Collectors.toMap(SchoolSubject::getId, item -> item));
        Map<String, UUID> teacherLookup = teacherAssignmentRepository.findAll().stream()
                .filter(item -> item.getSchoolId().equals(schoolId) && item.isActive())
                .collect(Collectors.toMap(item -> item.getClassId() + "|" + item.getSubjectId(), TeacherAssignment::getTeacherUserId, (left, right) -> left));
        Map<UUID, User> teacherUsers = userRepository.findAllById(new HashSet<>(teacherLookup.values())).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<UUID, List<SchoolIntervention>> interventionsByLearner = schoolInterventionRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .collect(Collectors.groupingBy(SchoolIntervention::getLearnerUserId));
        return visibleLearners.stream()
                .map(learnerId -> buildLearnerContext(
                        schoolId,
                        learnerId,
                        usersById.get(learnerId),
                        profilesByUserId.get(learnerId),
                        enrollmentsByLearner.getOrDefault(learnerId, List.of()),
                        classesById,
                        subjectsById,
                        teacherLookup,
                        teacherUsers,
                        interventionsByLearner.getOrDefault(learnerId, List.of())
                ))
                .filter(Objects::nonNull)
                .toList();
    }

    private LearnerContext buildLearnerContext(
            UUID schoolId,
            UUID learnerUserId,
            User user,
            StudentProfile profile,
            List<LearnerEnrollment> enrollments,
            Map<UUID, SchoolClass> classesById,
            Map<UUID, SchoolSubject> subjectsById,
            Map<String, UUID> teacherLookup,
            Map<UUID, User> teacherUsers,
            List<SchoolIntervention> interventions
    ) {
        if (user == null) {
            return null;
        }
        List<StudentSubjectAchievementDto> achievements = readSubjectAchievements(profile == null ? "[]" : profile.getSubjectAchievementsJson());
        Map<String, BigDecimal> feedbackMarks = latestMarksBySubject(learnerUserId, schoolId, subjectsById);
        List<SchoolPortalDtos.SubjectMarkView> subjects = achievements.stream()
                .map(achievement -> {
                    BigDecimal markPercent = feedbackMarks.getOrDefault(achievement.subjectName(), achievementToPercent(achievement.achievementLevel()));
                    boolean risk = markPercent.compareTo(new BigDecimal("55")) < 0 || (achievement.achievementLevel() != null && achievement.achievementLevel() <= 4);
                    return new SchoolPortalDtos.SubjectMarkView(achievement.subjectName(), achievement.achievementLevel(), markPercent, risk);
                })
                .sorted(Comparator.comparing(SchoolPortalDtos.SubjectMarkView::subjectName))
                .toList();
        long aps = calculateAps(achievements);
        String grade = enrollments.stream().map(LearnerEnrollment::getClassId).map(classesById::get).filter(Objects::nonNull).map(SchoolClass::getGrade).findFirst().orElse(profile == null ? null : profile.getSelectedGrade());
        String className = enrollments.stream().map(LearnerEnrollment::getClassId).map(classesById::get).filter(Objects::nonNull).map(SchoolClass::getClassName).findFirst().orElse(null);
        String teacherName = enrollments.stream()
                .map(item -> teacherLookup.get(item.getClassId() + "|" + item.getSubjectId()))
                .filter(Objects::nonNull)
                .map(teacherUsers::get)
                .filter(Objects::nonNull)
                .map(this::fullName)
                .filter(name -> !name.isBlank())
                .findFirst()
                .orElse("Unassigned");
        String careerGoal = profile == null ? null : normalize(profile.getCareerGoals());
        List<SchoolPortalDtos.MatchedCourseView> courseEligibility = matchedCourses(profile, aps);
        List<SchoolPortalDtos.MatchedBursaryView> bursaries = matchedBursaries(profile, subjects);
        String popiaStatus = consentRecordRepository.findByUserIdOrderByAcceptedAtDesc(learnerUserId).isEmpty() ? "Consent missing" : "Consent captured";
        BigDecimal average = subjects.isEmpty()
                ? BigDecimal.ZERO
                : subjects.stream().map(SchoolPortalDtos.SubjectMarkView::markPercent).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(subjects.size()), 2, RoundingMode.HALF_UP);
        boolean profileComplete = profile != null && profile.isProfileCompleted();
        boolean needsIntervention = average.compareTo(new BigDecimal("55")) < 0 || !profileComplete || interventions.stream().anyMatch(item -> !"CLOSED".equalsIgnoreCase(item.getStatus()));
        boolean bursaryEligible = bursaries.stream().anyMatch(SchoolPortalDtos.MatchedBursaryView::eligible);
        boolean careerMapped = careerGoal != null || savedCareerRepository.countByStudentId(profile == null ? nullSafeProfileId() : profile.getId()) > 0;
        boolean aligned = careerGoal != null && aps >= 24 && average.compareTo(new BigDecimal("60")) >= 0;
        String readinessGap = aligned ? "On track for selected pathway" : (!profileComplete ? "Incomplete academic or profile evidence" : aps < 24 ? "APS below degree-ready threshold" : "Subject performance needs improvement");
        String alternativePathway = aligned ? "Degree pathway" : aps >= 20 ? "Higher certificate or diploma pathway" : "TVET or bridging pathway";
        List<SchoolPortalDtos.TimelineItem> timeline = buildTimeline(learnerUserId, schoolId, interventions);
        return new LearnerContext(
                learnerUserId,
                profile == null ? null : profile.getId(),
                fullName(user),
                user.getEmail(),
                grade,
                className,
                teacherName,
                user.getStatus().name().equals("ACTIVE"),
                profileComplete,
                aps,
                careerGoal,
                profile == null ? null : normalize(profile.getQualificationLevel()),
                profile == null ? null : normalize(profile.getInterests()),
                profile == null ? null : normalize(profile.getSkills()),
                needsIntervention,
                bursaryEligible,
                careerMapped,
                subjects,
                courseEligibility,
                bursaries,
                interventions,
                timeline,
                popiaStatus,
                average,
                aligned,
                readinessGap,
                alternativePathway,
                topCareerInterest(profile, careerGoal),
                subjects.stream().filter(SchoolPortalDtos.SubjectMarkView::risk).map(SchoolPortalDtos.SubjectMarkView::subjectName).toList()
        );
    }

    private UUID nullSafeProfileId() {
        return new UUID(0L, 0L);
    }

    private List<UUID> assignedLearnerIds(UUID schoolId, UUID teacherUserId) {
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId);
        Set<String> assignmentKeys = assignments.stream().map(item -> item.getClassId() + "|" + item.getSubjectId()).collect(Collectors.toSet());
        return learnerEnrollmentRepository.findAll().stream()
                .filter(item -> item.getSchoolId().equals(schoolId) && item.isActive())
                .filter(item -> assignmentKeys.contains(item.getClassId() + "|" + item.getSubjectId()))
                .map(LearnerEnrollment::getLearnerUserId)
                .distinct()
                .toList();
    }

    private void ensureLearnerVisible(UUID schoolId, UUID viewerUserId, String roleName, UUID learnerUserId) {
        boolean visible = learnerContexts(schoolId, viewerUserId, roleName).stream().anyMatch(item -> item.learnerUserId().equals(learnerUserId));
        if (!visible) {
            throw new IllegalArgumentException("Learner is outside your access scope.");
        }
    }

    private List<SchoolPortalDtos.PerformanceBandItem> averageBy(
            List<LearnerContext> learners,
            java.util.function.Function<LearnerContext, String> keyMapper,
            java.util.function.Function<LearnerContext, BigDecimal> valueMapper
    ) {
        return learners.stream()
                .filter(item -> keyMapper.apply(item) != null)
                .collect(Collectors.groupingBy(keyMapper))
                .entrySet()
                .stream()
                .map(entry -> {
                    BigDecimal avg = entry.getValue().stream().map(valueMapper).reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);
                    return new SchoolPortalDtos.PerformanceBandItem(entry.getKey(), avg, tone(avg));
                })
                .sorted(Comparator.comparing(SchoolPortalDtos.PerformanceBandItem::label))
                .toList();
    }

    private List<SchoolPortalDtos.PerformanceBandItem> averageBySubjects(List<LearnerContext> learners) {
        Map<String, List<BigDecimal>> values = new HashMap<>();
        for (LearnerContext learner : learners) {
            for (SchoolPortalDtos.SubjectMarkView subject : learner.subjects()) {
                values.computeIfAbsent(subject.subjectName(), ignored -> new ArrayList<>()).add(subject.markPercent());
            }
        }
        return values.entrySet().stream()
                .map(entry -> {
                    BigDecimal avg = entry.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);
                    return new SchoolPortalDtos.PerformanceBandItem(entry.getKey(), avg, tone(avg));
                })
                .sorted(Comparator.comparing(SchoolPortalDtos.PerformanceBandItem::value))
                .toList();
    }

    private SchoolPortalDtos.LearnerListItem toLearnerListItem(LearnerContext context) {
        return new SchoolPortalDtos.LearnerListItem(
                context.learnerUserId(),
                context.studentProfileId(),
                context.learnerName(),
                context.email(),
                context.grade(),
                context.className(),
                context.teacherName(),
                context.active(),
                context.profileComplete(),
                context.apsPoints(),
                context.careerGoal(),
                context.needsIntervention(),
                context.bursaryEligible(),
                context.popiaStatus()
        );
    }

    private SchoolPortalDtos.InterventionSummaryView toInterventionSummary(SchoolIntervention item) {
        return new SchoolPortalDtos.InterventionSummaryView(item.getId(), item.getStatus(), item.getPriority(), item.getSupportType(), item.getNotes(), item.getFollowUpDate(), item.getUpdatedAt());
    }

    private SchoolPortalDtos.InterventionReportItem toInterventionReportItem(SchoolIntervention item) {
        User learner = userRepository.findById(item.getLearnerUserId()).orElse(null);
        User actor = userRepository.findById(item.getAssignedByUserId()).orElse(null);
        return new SchoolPortalDtos.InterventionReportItem(
                item.getId(),
                item.getLearnerUserId(),
                learner == null ? "Learner" : fullName(learner),
                actor == null ? "Staff" : fullName(actor),
                item.getSupportType(),
                item.getPriority(),
                item.getStatus(),
                item.getNotes(),
                item.getFollowUpDate(),
                item.getUpdatedAt()
        );
    }

    private List<SchoolPortalDtos.MatchedCourseView> matchedCourses(StudentProfile profile, long aps) {
        String qualification = profile == null ? null : normalize(profile.getQualificationLevel());
        return courseRepository.findAll().stream()
                .filter(course -> qualification == null || contains(course.getLevel(), qualification))
                .sorted(Comparator.comparing(Course::getName))
                .limit(5)
                .map(course -> new SchoolPortalDtos.MatchedCourseView(
                        course.getName(),
                        course.getLevel(),
                        aps >= 24 || !"degree".equalsIgnoreCase(normalize(course.getLevel())),
                        aps >= 24 ? "APS is within a safe matching band." : "APS likely needs improvement for degree-heavy pathways."
                ))
                .toList();
    }

    private List<SchoolPortalDtos.MatchedBursaryView> matchedBursaries(StudentProfile profile, List<SchoolPortalDtos.SubjectMarkView> subjects) {
        String interests = profile == null ? null : normalize(profile.getInterests());
        String careerGoal = profile == null ? null : normalize(profile.getCareerGoals());
        String qualification = profile == null ? null : normalize(profile.getQualificationLevel());
        boolean hasTranscript = profile != null && normalize(profile.getTranscriptFileUrl()) != null;
        return bursaryRepository.findAll().stream()
                .filter(item -> normalize(item.getStatus()) == null || !"deleted".equals(normalize(item.getStatus())))
                .filter(item -> matchesBursary(item, interests, careerGoal, qualification))
                .sorted(Comparator.comparing(Bursary::getApplicationEndDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .map(item -> new SchoolPortalDtos.MatchedBursaryView(
                        item.getTitle(),
                        item.getProvider(),
                        item.getApplicationEndDate(),
                        hasTranscript && qualification != null,
                        hasTranscript ? "" : "Missing transcript or formal qualification data"
                ))
                .toList();
    }

    private boolean matchesBursary(Bursary bursary, String interests, String careerGoal, String qualification) {
        String haystack = String.join(" ",
                safe(bursary.getTitle()),
                safe(bursary.getFieldOfStudy()),
                safe(bursary.getEligibility()),
                safe(bursary.getDescription()),
                safe(bursary.getQualificationLevel())
        ).toLowerCase(Locale.ROOT);
        return contains(haystack, interests) || contains(haystack, careerGoal) || contains(haystack, qualification);
    }

    private List<SchoolPortalDtos.TimelineItem> buildTimeline(UUID learnerUserId, UUID schoolId, List<SchoolIntervention> interventions) {
        List<SchoolPortalDtos.TimelineItem> items = new ArrayList<>();
        List<TaskSubmission> submissions = taskSubmissionRepository.findByLearnerUserId(learnerUserId);
        for (TaskSubmission submission : submissions) {
            SchoolTask task = schoolTaskRepository.findById(submission.getTaskId()).orElse(null);
            if (task != null && task.getSchoolId().equals(schoolId)) {
                items.add(new SchoolPortalDtos.TimelineItem("Task submitted", task.getTitle(), submission.getCreatedAt(), "submission"));
                submissionFeedbackRepository.findBySubmissionId(submission.getId()).ifPresent(feedback ->
                        items.add(new SchoolPortalDtos.TimelineItem("Marks updated", "Score: " + feedback.getMarksAwarded(), feedback.getUpdatedAt(), "feedback")));
            }
        }
        for (SchoolIntervention intervention : interventions) {
            items.add(new SchoolPortalDtos.TimelineItem("Intervention updated", intervention.getStatus() + " - " + intervention.getSupportType(), intervention.getUpdatedAt(), "intervention"));
        }
        return items.stream().sorted(Comparator.comparing(SchoolPortalDtos.TimelineItem::occurredAt).reversed()).limit(12).toList();
    }

    private Map<String, BigDecimal> latestMarksBySubject(UUID learnerUserId, UUID schoolId, Map<UUID, SchoolSubject> subjectsById) {
        Map<String, BigDecimal> values = new HashMap<>();
        for (TaskSubmission submission : taskSubmissionRepository.findByLearnerUserId(learnerUserId)) {
            SchoolTask task = schoolTaskRepository.findById(submission.getTaskId()).orElse(null);
            if (task == null || !task.getSchoolId().equals(schoolId)) {
                continue;
            }
            SubmissionFeedback feedback = submissionFeedbackRepository.findBySubmissionId(submission.getId()).orElse(null);
            if (feedback == null || feedback.getMarksAwarded() == null) {
                continue;
            }
            SchoolSubject subject = subjectsById.get(task.getSubjectId());
            if (subject == null) {
                continue;
            }
            values.put(subject.getSubjectName(), feedback.getMarksAwarded());
        }
        return values;
    }

    private List<StudentSubjectAchievementDto> readSubjectAchievements(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, SUBJECTS_TYPE);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private long calculateAps(List<StudentSubjectAchievementDto> achievements) {
        return achievements.stream()
                .filter(item -> item.achievementLevel() != null)
                .filter(item -> !safe(item.subjectName()).equalsIgnoreCase("Life Orientation"))
                .map(StudentSubjectAchievementDto::achievementLevel)
                .sorted(Comparator.reverseOrder())
                .limit(6)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private BigDecimal achievementToPercent(Integer level) {
        if (level == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(level * 100.0 / 7.0).setScale(2, RoundingMode.HALF_UP);
    }

    private String topCareerInterest(StudentProfile profile, String careerGoal) {
        if (careerGoal != null) {
            return careerGoal;
        }
        if (profile == null || profile.getInterests() == null) {
            return "Undeclared";
        }
        return profile.getInterests().split(",")[0].trim();
    }

    private String buildChecklist(LearnerContext context, SchoolPortalDtos.MatchedBursaryView match) {
        List<String> items = new ArrayList<>();
        if (!context.profileComplete()) items.add("Complete learner profile");
        if (normalize(context.careerGoal()) == null) items.add("Confirm career goal");
        if (!"Consent captured".equals(context.popiaStatus())) items.add("Capture POPIA consent");
        if (match.deadline() != null) items.add("Submit before " + match.deadline());
        return String.join("; ", items);
    }

    private String wholeSchoolReadinessReport(UUID schoolId, UUID viewerUserId, String roleName) {
        List<LearnerContext> learners = learnerContexts(schoolId, viewerUserId, roleName);
        StringBuilder builder = new StringBuilder("learner,grade,class,aps,profile_complete,career_goal,bursary_eligible,needs_intervention\n");
        for (LearnerContext learner : learners) {
            builder.append(csv(learner.learnerName())).append(',')
                    .append(csv(learner.grade())).append(',')
                    .append(csv(learner.className())).append(',')
                    .append(learner.apsPoints()).append(',')
                    .append(learner.profileComplete()).append(',')
                    .append(csv(learner.careerGoal())).append(',')
                    .append(learner.bursaryEligible()).append(',')
                    .append(learner.needsIntervention()).append('\n');
        }
        return builder.toString();
    }

    private String gradeReadinessReport(UUID schoolId, UUID viewerUserId, String roleName) {
        StringBuilder builder = new StringBuilder("grade,average_mark,learners,intervention_count\n");
        for (SchoolPortalDtos.PerformanceBandItem item : averageBy(learnerContexts(schoolId, viewerUserId, roleName), LearnerContext::grade, LearnerContext::averageMarkPercent)) {
            long learnerCount = learnerContexts(schoolId, viewerUserId, roleName).stream().filter(context -> Objects.equals(context.grade(), item.label())).count();
            long interventions = learnerContexts(schoolId, viewerUserId, roleName).stream().filter(context -> Objects.equals(context.grade(), item.label()) && context.needsIntervention()).count();
            builder.append(csv(item.label())).append(',').append(item.value()).append(',').append(learnerCount).append(',').append(interventions).append('\n');
        }
        return builder.toString();
    }

    private String subjectGapReport(UUID schoolId, UUID viewerUserId, String roleName) {
        StringBuilder builder = new StringBuilder("subject,average_mark,tone\n");
        for (SchoolPortalDtos.PerformanceBandItem item : averageBySubjects(learnerContexts(schoolId, viewerUserId, roleName))) {
            builder.append(csv(item.label())).append(',').append(item.value()).append(',').append(item.tone()).append('\n');
        }
        return builder.toString();
    }

    private String careerInterestReport(UUID schoolId, UUID viewerUserId, String roleName) {
        StringBuilder builder = new StringBuilder("career_interest,count\n");
        for (SchoolPortalDtos.TopBreakdownItem item : topBreakdown(learnerContexts(schoolId, viewerUserId, roleName).stream().map(LearnerContext::topCareerInterest).toList(), 20)) {
            builder.append(csv(item.label())).append(',').append(item.value()).append('\n');
        }
        return builder.toString();
    }

    private String bursaryReadinessReport(UUID schoolId, UUID viewerUserId, String roleName) {
        StringBuilder builder = new StringBuilder("learner,bursary,provider,deadline,eligible,missing_requirements\n");
        for (SchoolPortalDtos.BursaryReadinessItem item : bursaryReadiness(schoolId, viewerUserId, roleName).matches()) {
            builder.append(csv(item.learnerName())).append(',').append(csv(item.bursaryTitle())).append(',').append(csv(item.provider())).append(',')
                    .append(item.deadline() == null ? "" : item.deadline()).append(',').append(!item.missingRequirements().isBlank()).append(',')
                    .append(csv(item.missingRequirements())).append('\n');
        }
        return builder.toString();
    }

    private String atRiskLearnerReport(UUID schoolId, UUID viewerUserId, String roleName) {
        StringBuilder builder = new StringBuilder("learner,grade,class,aps,readiness_gap,teacher\n");
        for (LearnerContext item : learnerContexts(schoolId, viewerUserId, roleName).stream().filter(LearnerContext::needsIntervention).toList()) {
            builder.append(csv(item.learnerName())).append(',').append(csv(item.grade())).append(',').append(csv(item.className())).append(',')
                    .append(item.apsPoints()).append(',').append(csv(item.readinessGap())).append(',').append(csv(item.teacherName())).append('\n');
        }
        return builder.toString();
    }

    private List<SchoolPortalDtos.TopBreakdownItem> topBreakdown(Collection<String> values, int limit) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(entry -> new SchoolPortalDtos.TopBreakdownItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private SchoolPortalDtos.PortalMetric metric(String label, long value, String trendLabel, String tone) {
        return new SchoolPortalDtos.PortalMetric(label, value, trendLabel, tone);
    }

    private long reportUploadCount(UUID schoolId) {
        return schoolTaskRepository.findAll().stream().filter(item -> item.getSchoolId().equals(schoolId)).count()
                + taskSubmissionRepository.findAll().stream()
                .filter(item -> schoolTaskRepository.findById(item.getTaskId()).map(task -> task.getSchoolId().equals(schoolId)).orElse(false))
                .count();
    }

    private String tone(BigDecimal value) {
        if (value.compareTo(new BigDecimal("70")) >= 0) return "positive";
        if (value.compareTo(new BigDecimal("55")) >= 0) return "warning";
        return "critical";
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean contains(String value, String expected) {
        if (expected == null) return true;
        return value != null && value.toLowerCase(Locale.ROOT).contains(expected);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String fullName(User user) {
        return (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String normalizeStatus(String value) {
        String normalized = value == null ? "OPEN" : value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "OPEN" : normalized;
    }

    private void writeAudit(UUID actorId, String action, UUID entityId, Map<String, Object> details) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setAction(action);
        log.setEntityType("SCHOOL_PORTAL");
        log.setEntityId(entityId);
        log.setDetails(objectMapper.valueToTree(details));
        auditLogRepository.save(log);
    }

    private byte[] brandedPdf(UUID schoolId, UUID viewerUserId, String roleName, String reportType, String csvPayload) {
        School school = schoolRepository.findById(schoolId).orElseThrow();
        List<LearnerContext> learners = learnerContexts(schoolId, viewerUserId, roleName);
        long atRisk = learners.stream().filter(LearnerContext::needsIntervention).count();
        long bursaryReady = learners.stream().filter(LearnerContext::bursaryEligible).count();
        long completeProfiles = learners.stream().filter(LearnerContext::profileComplete).count();
        long activeInterventions = schoolInterventionRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .filter(item -> !"CLOSED".equalsIgnoreCase(item.getStatus()))
                .count();

        List<String> lines = new ArrayList<>();
        lines.add("EduRite");
        lines.add("School & Teacher Portal Report");
        lines.add(titleCase(reportType.replace('-', ' ')));
        lines.add("School: " + school.getSchoolName());
        lines.add("Generated: " + LocalDate.now());
        lines.add("Role scope: " + roleName.replace("ROLE_", ""));
        lines.add("");
        lines.add("Summary Cards");
        lines.add("Total learners: " + learners.size());
        lines.add("Complete profiles: " + completeProfiles);
        lines.add("Learners needing intervention: " + atRisk);
        lines.add("Learners eligible for bursaries: " + bursaryReady);
        lines.add("Open interventions: " + activeInterventions);
        lines.add("");
        lines.add("Top Career Interests");
        for (SchoolPortalDtos.TopBreakdownItem item : topBreakdown(learners.stream().map(LearnerContext::topCareerInterest).toList(), 5)) {
            lines.add(item.label() + ": " + item.value());
        }
        lines.add("");
        lines.add("Top Subject Risk Areas");
        for (SchoolPortalDtos.TopBreakdownItem item : topBreakdown(learners.stream().flatMap(context -> context.riskSubjects().stream()).toList(), 5)) {
            lines.add(item.label() + ": " + item.value());
        }
        lines.add("");
        lines.add("Intervention Summary");
        for (SchoolPortalDtos.InterventionReportItem item : interventions(schoolId, viewerUserId, roleName).stream().limit(8).toList()) {
            lines.add(item.learnerName() + " | " + item.supportType() + " | " + item.status());
        }
        lines.add("");
        lines.add("Learner Readiness Data");
        for (String line : csvPayload.split("\n")) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        lines.add("");
        lines.add("EduRite confidential school readiness report");
        return renderSimpleReportPdf(lines);
    }

    private byte[] renderSimpleReportPdf(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("0.12 0.36 1 rg 40 780 180 26 re f\n");
        content.append("0 0 0 rg\n");
        content.append("BT /F1 18 Tf 48 787 Td (EduRite) Tj ET\n");
        content.append("0.95 0.97 1 rg 40 720 515 46 re f\n");
        content.append("0 0 0 rg\n");
        content.append("BT /F1 11 Tf 48 748 Td 14 TL\n");
        int linesWritten = 0;
        for (String line : lines) {
            if (linesWritten >= 44) break;
            content.append("(").append(escapePdf(line)).append(") Tj T*\n");
            linesWritten++;
        }
        content.append("ET\n");
        content.append("0.45 0.45 0.45 rg\n");
        content.append("40 28 515 1 re f\n");
        content.append("0 0 0 rg\n");
        content.append("BT /F1 9 Tf 40 16 Td (Generated by EduRite School Portal) Tj ET\n");

        byte[] contentBytes = content.toString().getBytes(StandardCharsets.UTF_8);
        List<Integer> offsets = new ArrayList<>();
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        offsets.add(pdf.length());
        pdf.append("1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
        offsets.add(pdf.length());
        pdf.append("2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n");
        offsets.add(pdf.length());
        pdf.append("3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources<< /Font<< /F1 4 0 R >> >> /Contents 5 0 R >>endobj\n");
        offsets.add(pdf.length());
        pdf.append("4 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n");
        offsets.add(pdf.length());
        pdf.append("5 0 obj<< /Length ").append(contentBytes.length).append(" >>stream\n")
                .append(content).append("\nendstream endobj\n");
        int xrefOffset = pdf.length();
        pdf.append("xref\n0 6\n0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        pdf.append("trailer<< /Root 1 0 R /Size 6 >>\nstartxref\n").append(xrefOffset).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapePdf(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String titleCase(String value) {
        String[] parts = value.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isBlank()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(parts[i].substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(parts[i].substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private record LearnerContext(
            UUID learnerUserId,
            UUID studentProfileId,
            String learnerName,
            String email,
            String grade,
            String className,
            String teacherName,
            boolean active,
            boolean profileComplete,
            long apsPoints,
            String careerGoal,
            String qualificationLevel,
            String interests,
            String skills,
            boolean needsIntervention,
            boolean bursaryEligible,
            boolean careerMapped,
            List<SchoolPortalDtos.SubjectMarkView> subjects,
            List<SchoolPortalDtos.MatchedCourseView> courseEligibility,
            List<SchoolPortalDtos.MatchedBursaryView> bursaryMatches,
            List<SchoolIntervention> interventions,
            List<SchoolPortalDtos.TimelineItem> timeline,
            String popiaStatus,
            BigDecimal averageMarkPercent,
            boolean alignedToCareerPath,
            String readinessGap,
            String alternativePathway,
            String topCareerInterest,
            List<String> riskSubjects
    ) {
    }
}
