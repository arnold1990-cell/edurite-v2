package com.edurite.school.portal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SchoolPortalDtos {
    private SchoolPortalDtos() {
    }

    public record SchoolProfileUpsertRequest(
            @NotBlank String schoolName,
            String registrationNumber,
            String district,
            String province,
            String contactEmail,
            String contactPhone,
            String address
    ) {}

    public record SchoolClassRequest(@NotBlank String grade, @NotBlank String className, @NotNull Integer academicYear, String term) {}
    public record SchoolClassUpdateRequest(@NotBlank String grade, @NotBlank String className, @NotNull Integer academicYear, String term, boolean active) {}
    public record SchoolSubjectRequest(
            UUID subjectCatalogueId,
            @NotBlank String subjectName,
            @NotBlank String phase,
            String grade,
            String gradeRange,
            String languageLevel,
            String subjectType,
            Boolean isLanguage,
            Boolean isCompulsory,
            UUID hodUserId,
            Boolean capsAligned,
            Boolean active
    ) {}
    public record SchoolSubjectUpdateRequest(
            UUID subjectCatalogueId,
            @NotBlank String subjectName,
            @NotBlank String phase,
            String grade,
            String gradeRange,
            String languageLevel,
            String subjectType,
            Boolean isLanguage,
            Boolean isCompulsory,
            UUID hodUserId,
            Boolean capsAligned,
            boolean active
    ) {}
    public record TeacherAssignmentRequest(
            @NotNull UUID teacherUserId,
            @NotNull UUID classId,
            @NotNull UUID subjectId,
            String phase,
            String grade,
            Boolean isClassTeacher
    ) {}
    public record TeacherAssignmentBulkRequest(@NotNull List<TeacherAssignmentRequest> assignments) {}
    public record LearnerEnrollmentRequest(@NotNull UUID learnerUserId, @NotNull UUID classId, @NotNull UUID subjectId) {}
    public record SchoolUserCreateRequest(
            @NotBlank String email,
            @NotBlank String password,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String roleName,
            String phoneNumber,
            String status,
            String selectedGrade,
            String careerGoal,
            Boolean popiaConsentAccepted,
            String consentVersion
    ) {
        @AssertTrue(message = "POPIA consent is required for learner accounts")
        public boolean isLearnerConsentValid() {
            if (!"ROLE_SCHOOL_STUDENT".equalsIgnoreCase(roleName) && !"SCHOOL_STUDENT".equalsIgnoreCase(roleName)) {
                return true;
            }
            return Boolean.TRUE.equals(popiaConsentAccepted);
        }
    }
    public record SchoolUserUpdateRequest(@NotBlank String firstName, @NotBlank String lastName, @NotBlank String email) {}
    public record BulkLearnerUploadRow(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String email,
            String password,
            String grade,
            String className,
            String selectedGrade,
            String careerGoal,
            String teacherEmail,
            Boolean popiaConsentAccepted,
            String consentVersion
    ) {}
    public record BulkLearnerUploadResult(int createdCount, int updatedCount, int skippedCount, List<String> messages) {}

    public record LearningNoteRequest(@NotNull UUID classId, @NotNull UUID subjectId, @NotBlank String title, String noteText, String pdfUrl) {}

    public record SchoolTaskRequest(
            UUID teacherUserId,
            @NotNull UUID classId,
            @NotNull UUID subjectId,
            UUID atpTopicId,
            @NotBlank String taskType,
            @NotBlank String title,
            Integer academicYear,
            String phase,
            String grade,
            String instructions,
            String assessmentType,
            Integer weekNumber,
            @NotNull OffsetDateTime dueAt,
            String term,
            @NotNull @DecimalMin("0.0") BigDecimal maxMarks,
            String rubric,
            String resources,
            String cognitiveLevel,
            String assessmentCategory
    ) {}

    public record TaskSubmissionRequest(@NotNull UUID taskId, String submissionText, String fileUrl) {}

    public record MarkSubmissionRequest(@NotNull @DecimalMin("0.0") BigDecimal marksAwarded, String comments, String rubricScoring, boolean released) {}

    public record ProgressSummaryResponse(long totalTasks, long submitted, long missing, long late) {}

    public record SubmissionView(
            UUID submissionId,
            UUID taskId,
            UUID learnerUserId,
            String learnerName,
            String submissionText,
            String fileUrl,
            boolean late,
            String status,
            BigDecimal similarity,
            boolean plagiarismFlag,
            BigDecimal marks,
            String feedback,
            boolean released
    ) {}

    public record StudentTaskView(UUID taskId, String taskType, String title, String instructions, OffsetDateTime dueAt, BigDecimal maxMarks, String term) {}
    public record LearnerSubjectView(
            UUID subjectId,
            String subjectName,
            String phase,
            String grade,
            String teacherName,
            int progress,
            String latestTaskTitle,
            String latestNoteTitle
    ) {}

    public record DashboardResponse(String role, UUID schoolId, long totalClasses, long totalSubjects, long totalTasks, long totalNotes, long totalSubmissions) {}
    public record PortalMetric(String label, long value, String trendLabel, String tone) {}
    public record TopBreakdownItem(String label, long value) {}
    public record DashboardSnapshot(
            String role,
            UUID schoolId,
            List<PortalMetric> metrics,
            List<TopBreakdownItem> topCareerInterests,
            List<TopBreakdownItem> topSubjectRiskAreas
    ) {}

    public record TeacherClassView(UUID classId, String grade, String className, Integer academicYear, String subjectName, long learnerCount) {}
    public record TeacherSubjectView(UUID subjectId, String subjectName, String phase, String grade, long classCount) {}
    public record AtpTopicView(
            UUID id,
            String phase,
            String grade,
            UUID subjectCatalogueId,
            String subjectName,
            Integer academicYear,
            String term,
            Integer weekNumber,
            String topic,
            String subtopic,
            String recommendedActivities,
            String assessmentGuidance,
            String capsReference,
            boolean active
    ) {}
    public record SubjectCatalogueItem(
            UUID id,
            String name,
            String phase,
            String gradeRange,
            String subjectType,
            String languageLevel,
            boolean isLanguage,
            boolean isCompulsory,
            boolean active
    ) {}
    public record SchoolSubjectView(
            UUID id,
            UUID subjectCatalogueId,
            String subjectName,
            String phase,
            String grade,
            String gradeRange,
            String languageLevel,
            String subjectType,
            boolean isLanguage,
            boolean isCompulsory,
            UUID hodUserId,
            boolean capsAligned,
            boolean active,
            long assignedTeacherCount,
            long learnerCount,
            boolean hasLinkedRecords
    ) {}
    public record SubjectSummaryMetric(String label, long value, String helperText) {}
    public record SubjectManagementSummary(List<SubjectSummaryMetric> metrics) {}
    public record TeacherAssignmentView(
            UUID id,
            UUID teacherUserId,
            UUID classId,
            UUID subjectId,
            String phase,
            String grade,
            boolean isClassTeacher,
            boolean active
    ) {}
    public record TeacherAnalyticsResponse(
            long pendingMarking,
            long sbaTasksDue,
            long learnerSubmissions,
            BigDecimal averageClassPerformance,
            BigDecimal attendanceRate,
            long upcomingAssessments
    ) {}
    public record TeacherActivityItem(String type, String title, String detail, OffsetDateTime occurredAt, String priority) {}
    public record TeacherMessageItem(String from, String subject, String body, boolean unread, OffsetDateTime createdAt) {}
    public record TeacherCalendarItem(String title, String category, OffsetDateTime dueAt) {}
    public record TeacherAiPromptRequest(@NotBlank String prompt, String context) {}
    public record TeacherAiResponse(String title, String content, String provider, boolean mock) {}

    public record CollectionResponse<T>(List<T> items) {}
    public record SchoolUserAdminView(UUID userId, String fullName, String email, String roleName, String status, boolean active, String phoneNumber) {}

    public record LearnerListItem(
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
            boolean needsIntervention,
            boolean bursaryEligible,
            String popiaStatus
    ) {}

    public record LearnerListResponse(List<LearnerListItem> items, long total) {}

    public record SubjectMarkView(String subjectName, Integer achievementLevel, BigDecimal markPercent, boolean risk) {}
    public record MatchedCourseView(String name, String level, boolean eligible, String reason) {}
    public record MatchedBursaryView(String title, String provider, LocalDate deadline, boolean eligible, String missingRequirements) {}
    public record TimelineItem(String title, String detail, OffsetDateTime occurredAt, String type) {}
    public record InterventionSummaryView(
            UUID interventionId,
            String status,
            String priority,
            String supportType,
            String notes,
            LocalDate followUpDate,
            OffsetDateTime updatedAt
    ) {}
    public record LearnerProfileResponse(
            UUID learnerUserId,
            UUID studentProfileId,
            String learnerName,
            String email,
            String grade,
            String className,
            String teacherName,
            boolean profileComplete,
            long apsPoints,
            String careerGoal,
            String qualificationLevel,
            String interests,
            String skills,
            String popiaStatus,
            List<SubjectMarkView> subjects,
            List<MatchedCourseView> courseEligibility,
            List<MatchedBursaryView> bursaryMatches,
            List<InterventionSummaryView> interventions,
            List<TimelineItem> activityTimeline
    ) {}

    public record PerformanceBandItem(String label, BigDecimal value, String tone) {}
    public record AcademicInsightsResponse(
            List<PerformanceBandItem> gradePerformance,
            List<PerformanceBandItem> subjectPerformance,
            List<PerformanceBandItem> classPerformance,
            List<LearnerListItem> atRiskLearners,
            List<TopBreakdownItem> subjectsAffectingCareerEligibility
    ) {}

    public record CareerReadinessLearnerView(
            UUID learnerUserId,
            String learnerName,
            String careerGoal,
            boolean aligned,
            long apsPoints,
            String readinessGap,
            String alternativePathway
    ) {}
    public record CareerReadinessResponse(
            List<CareerReadinessLearnerView> learners,
            List<TopBreakdownItem> topCareerInterests,
            List<TopBreakdownItem> readinessGaps,
            List<TopBreakdownItem> alternativePathways
    ) {}

    public record BursaryReadinessItem(
            UUID learnerUserId,
            String learnerName,
            String bursaryTitle,
            String provider,
            LocalDate deadline,
            String missingRequirements,
            String checklist
    ) {}
    public record BursaryReadinessResponse(
            List<BursaryReadinessItem> matches,
            List<BursaryReadinessItem> deadlineAlerts,
            List<TopBreakdownItem> missingRequirements
    ) {}

    public record InterventionRequest(
            @NotNull UUID learnerUserId,
            @NotBlank String supportType,
            @NotBlank String priority,
            @NotBlank String notes,
            LocalDate followUpDate,
            String status
    ) {}
    public record InterventionProgressRequest(
            @NotBlank String status,
            @NotBlank String notes,
            LocalDate followUpDate
    ) {}
    public record InterventionReportItem(
            UUID interventionId,
            UUID learnerUserId,
            String learnerName,
            String assignedBy,
            String supportType,
            String priority,
            String status,
            String notes,
            LocalDate followUpDate,
            OffsetDateTime updatedAt
    ) {}

    public record ReportExportResponse(String fileName, String contentType, String base64Content) {}

    public record PortalSettingsResponse(
            String schoolName,
            String district,
            String province,
            List<String> activeRoles,
            List<Map<String, Object>> recentAuditLogs
    ) {}
}


