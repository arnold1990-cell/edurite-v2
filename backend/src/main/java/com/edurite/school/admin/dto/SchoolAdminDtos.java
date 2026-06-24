package com.edurite.school.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SchoolAdminDtos {
    private SchoolAdminDtos() {
    }

    public record MetricCardDto(String label, String value, String helperText, String tone) {}
    public record TrendPointDto(String label, BigDecimal value, String tone) {}
    public record InsightItemDto(String title, String detail, String severity) {}
    public record DistributionItemDto(String label, long value, String tone) {}

    public record SchoolAdminDashboardResponse(
            String schoolName,
            String role,
            String systemStatus,
            String subscriptionStatus,
            String summaryHeadline,
            List<MetricCardDto> metrics,
            List<InsightItemDto> topCareerInterests,
            List<InsightItemDto> topSubjectRiskAreas,
            List<InsightItemDto> teacherActivitySummary,
            List<TrendPointDto> schoolPerformanceTrends,
            List<TrendPointDto> subjectPerformance,
            List<DistributionItemDto> apsBandDistribution,
            List<DistributionItemDto> gradePerformanceComparison,
            List<DistributionItemDto> reportUploadProgress,
            List<InsightItemDto> districtReportingSummary
    ) {}

    public record LearnerRequirementDto(String label, String value, String tone) {}

    public record LearnerAdminProfileResponse(
            UUID learnerUserId,
            String learnerName,
            String email,
            String grade,
            String className,
            String teacherName,
            boolean profileComplete,
            long apsPoints,
            String careerGoal,
            String readinessLevel,
            String reportUploadStatus,
            String consentStatus,
            String qualificationLevel,
            String interests,
            String skills,
            List<SchoolAdminDtos.LearnerRequirementDto> requiredSubjects,
            List<SchoolAdminDtos.LearnerRequirementDto> requiredMarks,
            List<SchoolPortalCourseDto> coursesQualifiedNow,
            List<SchoolPortalCourseDto> coursesCloseToQualifyingFor,
            List<InsightItemDto> alternativePathways,
            List<SchoolPortalBursaryDto> bursaryMatches,
            List<SchoolAdminDtos.LearnerRequirementDto> missingRequirements,
            List<SchoolAdminDtos.LearnerRequirementDto> bursaryDeadlines,
            List<SchoolAdminInterventionDto> interventionPlan,
            List<SchoolAdminNoteDto> teacherNotes,
            String followUpDate,
            List<SchoolAdminTimelineDto> activityTimeline
    ) {}

    public record SchoolPortalCourseDto(String name, String level, boolean eligible, String reason) {}
    public record SchoolPortalBursaryDto(String title, String provider, String deadline, boolean eligible, String missingRequirements) {}
    public record SchoolAdminInterventionDto(UUID interventionId, String status, String priority, String supportType, String notes, String followUpDate, String updatedAt) {}
    public record SchoolAdminNoteDto(String title, String detail, String author, String createdAt) {}
    public record SchoolAdminTimelineDto(String title, String detail, String occurredAt, String type) {}

    public record SchoolAdminCareerReadinessResponse(
            String headline,
            List<MetricCardDto> metrics,
            List<InsightItemDto> topCareerInterests,
            List<InsightItemDto> commonReadinessGaps,
            List<InsightItemDto> alignmentWarnings,
            List<InsightItemDto> alternativePathwayRecommendations
    ) {}

    public record SchoolAdminCourseMatchDto(
            UUID learnerUserId,
            String learnerName,
            String careerGoal,
            long apsPoints,
            List<SchoolPortalCourseDto> qualifiedCourses,
            List<SchoolPortalCourseDto> closeCourses
    ) {}

    public record SchoolAdminCoursesResponse(
            String headline,
            List<MetricCardDto> metrics,
            List<InsightItemDto> institutionOptions,
            List<InsightItemDto> mostMatchedCourses,
            List<InsightItemDto> qualificationGaps,
            List<SchoolAdminCourseMatchDto> learnerMatches
    ) {}

    public record SchoolAdminBursaryReadinessResponse(
            String headline,
            List<MetricCardDto> metrics,
            List<InsightItemDto> fundingInterestByCareerField,
            List<InsightItemDto> missingRequirements,
            List<InsightItemDto> deadlineAlerts,
            List<SchoolPortalBursaryDto> savedBursaries
    ) {}

    public record SchoolAdminInterventionsResponse(
            String headline,
            List<MetricCardDto> metrics,
            List<InsightItemDto> interventionTypes,
            List<SchoolAdminInterventionReportDto> items
    ) {}

    public record SchoolAdminInterventionReportDto(
            UUID interventionId,
            UUID learnerUserId,
            String learnerName,
            String assignedBy,
            String supportType,
            String priority,
            String status,
            String notes,
            String followUpDate,
            String updatedAt
    ) {}

    public record SchoolAdminAnalyticsResponse(
            List<TrendPointDto> schoolPerformanceTrends,
            List<TrendPointDto> subjectPerformance,
            List<DistributionItemDto> apsBandDistribution,
            List<DistributionItemDto> gradePerformanceComparison,
            List<InsightItemDto> learnerImprovementRecommendations,
            List<InsightItemDto> careerReadinessOverview,
            List<InsightItemDto> districtReadyReportingSummary
    ) {}

    public record SchoolAdminAiInsightsResponse(
            boolean dataAvailable,
            String emptyStateMessage,
            List<InsightItemDto> learnersAtRisk,
            List<InsightItemDto> subjectWeaknessTrends,
            List<InsightItemDto> apsReadinessWarnings,
            List<InsightItemDto> careerPathwayGaps,
            List<InsightItemDto> bursaryNeedIndicators,
            List<InsightItemDto> teacherActivityAlerts,
            List<InsightItemDto> recommendedInterventions
    ) {}

    public record LearnerAdminItemDto(
            UUID learnerUserId,
            String learnerName,
            String username,
            String email,
            String generatedPassword,
            String grade,
            String className,
            String teacherName,
            long apsPoints,
            boolean profileComplete,
            boolean needsIntervention,
            boolean bursaryEligible,
            String readinessStatus,
            long bursaryMatchCount,
            String parentGuardianName,
            String parentGuardianPhone,
            String parentGuardianEmail,
            String consentStatus,
            String reportUploadStatus,
            String careerGoal,
            String lastActiveAt
    ) {}

    public record LearnerAdminListResponse(List<LearnerAdminItemDto> items, long total) {}

    public record LearnerCreateRequest(
            String email,
            String username,
            String password,
            @NotBlank String firstName,
            @NotBlank String lastName,
            String grade,
            String className,
            String careerGoal,
            String parentGuardianName,
            String parentGuardianPhone,
            @Email String parentGuardianEmail,
            Boolean popiaConsentAccepted,
            String consentVersion
    ) {}

    public record LearnerCredentialItemDto(String learnerName, String username, String password, String grade, String className) {}
    public record LearnerCredentialsExportDto(String fileName, String contentType, String base64Content, List<LearnerCredentialItemDto> items) {}

    public record TeacherAdminItemDto(
            UUID teacherUserId,
            String fullName,
            String email,
            String phoneNumber,
            String employeeNumber,
            String profilePhotoUrl,
            String status,
            boolean active,
            long assignedClasses,
            long assignedSubjects,
            long learnerCount,
            long createdAssignments,
            long createdAssessments,
            long uploadedNotes,
            long resourcesUploaded,
            long learnerSubmissions,
            BigDecimal submissionRate,
            BigDecimal averageLearnerPerformance,
            BigDecimal apsImpact,
            long interventionCount,
            long careerGuidanceSessions,
            long learnersSupported,
            long careerMappedLearners,
            long careerReadyLearners,
            long bursaryReadyLearners,
            long learnersAtRisk,
            long learnersMeetingApsRequirements,
            long activeInterventions,
            BigDecimal engagementScore
    ) {}

    public record TeacherAdminListResponse(List<TeacherAdminItemDto> items, long total, long pendingApprovals) {}

    public record TeacherAdminDashboardResponse(
            List<MetricCardDto> metrics,
            List<InsightItemDto> workloadAlerts,
            List<InsightItemDto> topContributors,
            List<InsightItemDto> approvalAlerts
    ) {}

    public record TeacherActivityItemDto(UUID teacherUserId, String teacherName, String title, String detail, String category, OffsetDateTime occurredAt) {}
    public record TeacherActivityResponse(List<TeacherActivityItemDto> today, List<TeacherActivityItemDto> thisWeek, List<TeacherActivityItemDto> thisMonth) {}

    public record TeacherAiInsightResponse(boolean dataAvailable, String emptyStateMessage, List<InsightItemDto> items) {}

    public record TeacherWorkloadItemDto(
            UUID teacherUserId,
            String teacherName,
            long classesAssigned,
            long subjectsAssigned,
            long learnersAssigned,
            long assessmentsAssigned,
            long assignmentsAssigned,
            String workloadBand
    ) {}
    public record TeacherWorkloadResponse(List<TeacherWorkloadItemDto> items, List<InsightItemDto> alerts) {}

    public record TeacherResourceCategoryDto(String category, long uploadCount, long downloadCount) {}
    public record TeacherResourceItemDto(UUID teacherUserId, String teacherName, long uploadCount, long downloadCount, List<TeacherResourceCategoryDto> categories) {}
    public record TeacherResourceResponse(List<TeacherResourceItemDto> teachers, List<InsightItemDto> mostUsedResources, List<InsightItemDto> topContributors) {}

    public record TeacherTrainingItemDto(
            UUID teacherUserId,
            String teacherName,
            long trainingCompleted,
            long eduriteCertifications,
            long careerGuidanceTraining,
            long aiToolUsage,
            BigDecimal cpdHours,
            LocalDate lastTrainingDate,
            String certificationStatus,
            String developmentProgress
    ) {}
    public record TeacherTrainingResponse(List<TeacherTrainingItemDto> items, List<InsightItemDto> recommendations) {}

    public record TeacherApprovalHistoryDto(String action, OffsetDateTime createdAt) {}
    public record TeacherDocumentDto(String title, String status, String url) {}
    public record TeacherProfileSectionDto(String label, String value) {}

    public record TeacherDetailResponse(
            TeacherAdminItemDto summary,
            List<String> classes,
            List<String> subjects,
            List<TeacherProfileSectionDto> profile,
            List<TeacherProfileSectionDto> approvalDetails,
            List<TeacherProfileSectionDto> performance,
            List<TeacherProfileSectionDto> readinessImpact,
            List<TeacherActivityItemDto> activityTimeline,
            List<TeacherDocumentDto> documents,
            List<InsightItemDto> notes,
            List<SchoolAdminInterventionReportDto> interventions,
            List<TeacherApprovalHistoryDto> approvalHistory
    ) {}

    public record ClassAdminItemDto(
            UUID classId,
            String className,
            String grade,
            int academicYear,
            String term,
            String classTeacher,
            long learnerCount,
            long subjectCount,
            long averageAps,
            BigDecimal careerReadinessPercent,
            BigDecimal bursaryReadinessPercent,
            long interventionCount,
            boolean active,
            BigDecimal attendanceRate,
            BigDecimal assignmentCompletionRate,
            BigDecimal reportUploadCompletion
    ) {}

    public record ClassAdminListResponse(
            List<MetricCardDto> metrics,
            List<ClassAdminItemDto> items,
            List<InsightItemDto> topPerformingClasses,
            List<InsightItemDto> mostImprovedClasses,
            List<InsightItemDto> atRiskClasses,
            List<InsightItemDto> highestCareerReadinessClasses
    ) {}

    public record ClassAnalyticsResponse(
            UUID classId,
            String classLabel,
            List<MetricCardDto> metrics,
            List<TrendPointDto> apsDistribution,
            List<TrendPointDto> subjectPerformanceTrends,
            List<TrendPointDto> readinessTrends,
            List<TrendPointDto> interventionTrends
    ) {}

    public record ClassCareerReadinessResponse(
            UUID classId,
            String classLabel,
            BigDecimal careerReadinessScore,
            List<InsightItemDto> careerInterests,
            List<InsightItemDto> careerAlignment,
            List<InsightItemDto> careerGaps
    ) {}

    public record ClassBursaryReadinessResponse(
            UUID classId,
            String classLabel,
            List<MetricCardDto> metrics,
            List<InsightItemDto> upcomingDeadlines,
            List<InsightItemDto> applicationReadiness
    ) {}

    public record ClassAiInsightsResponse(boolean dataAvailable, String emptyStateMessage, List<InsightItemDto> items) {}

    public record ClassProfileResponse(
            ClassAdminItemDto summary,
            List<InsightItemDto> academics,
            List<InsightItemDto> careerReadiness,
            List<InsightItemDto> bursaries,
            List<InsightItemDto> interventions,
            List<InsightItemDto> aiInsights,
            List<SchoolAdminTimelineDto> activityTimeline,
            List<LearnerAdminItemDto> learners,
            List<InsightItemDto> subjectTeachers
    ) {}

    public record ReportItemDto(String key, String title, String description, boolean pdfSupported, boolean excelSupported) {}
    public record ReportExportRequest(@NotBlank String type, @NotBlank String format) {}

    public record AnnouncementCreateRequest(@NotBlank String audience, @NotBlank String title, @NotBlank String message) {}
    public record AnnouncementItemDto(UUID id, String audience, String title, String message, String status, OffsetDateTime sentAt, OffsetDateTime createdAt) {}

    public record SupportRequestCreateRequest(@NotBlank String category, @NotBlank String title, @NotBlank String message, String priority) {}
    public record SupportRequestItemDto(UUID id, String category, String title, String message, String status, String priority, OffsetDateTime createdAt) {}

    public record SchoolSettingsResponse(
            String schoolName,
            String registrationNumber,
            String district,
            String province,
            String contactEmail,
            String contactPhone,
            String address,
            List<String> activeRoles,
            List<AuditItemDto> recentAuditLogs
    ) {}

    public record AuditItemDto(String action, String entityType, UUID entityId, OffsetDateTime createdAt) {}
}
