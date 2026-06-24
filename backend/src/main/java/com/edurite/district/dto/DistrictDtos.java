package com.edurite.district.dto;

import com.edurite.school.portal.dto.SchoolPortalDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DistrictDtos {
    private DistrictDtos() {
    }

    public record MetricCardDto(String label, String value, String helperText, String tone) {}
    public record TrendPointDto(String label, BigDecimal value, String tone) {}
    public record InsightItemDto(String title, String detail, String severity) {}
    public record DistributionItemDto(String label, long value, String tone) {}

    public record DistrictDashboardResponse(
            String districtName,
            String districtCode,
            String province,
            String licensingStatus,
            String summaryHeadline,
            List<MetricCardDto> metrics,
            List<TrendPointDto> performanceTrends,
            List<DistributionItemDto> learnerRiskDistribution,
            List<DistributionItemDto> reportUploadProgress,
            List<InsightItemDto> schoolRanking,
            List<InsightItemDto> schoolsNeedingIntervention,
            List<InsightItemDto> aiHighlights,
            List<SupportRequestItemDto> recentSupportRequests,
            List<AnnouncementItemDto> recentAnnouncements
    ) {}

    public record DistrictSchoolSummaryDto(
            UUID schoolId,
            String schoolName,
            String registrationNumber,
            String district,
            String province,
            long learnerCount,
            long teacherCount,
            long activeClasses,
            String reportUploadStatus,
            BigDecimal averageApsScore,
            String performanceSummary,
            String apsReadiness,
            String riskLevel,
            String complianceStatus
    ) {}

    public record DistrictSchoolsResponse(
            List<MetricCardDto> metrics,
            List<DistrictSchoolSummaryDto> items,
            long total
    ) {}

    public record DistrictSchoolDetailResponse(
            UUID schoolId,
            String schoolName,
            String registrationNumber,
            String district,
            String province,
            String contactEmail,
            String contactPhone,
            String address,
            List<MetricCardDto> metrics,
            List<TrendPointDto> subjectPerformance,
            List<DistributionItemDto> apsBandDistribution,
            List<DistributionItemDto> reportUploads,
            List<InsightItemDto> teacherActivity,
            List<InsightItemDto> aiRecommendations,
            String exportReportType
    ) {}

    public record DistrictSchoolAnalyticsResponse(
            UUID schoolId,
            String schoolName,
            List<TrendPointDto> performanceTrends,
            List<TrendPointDto> subjectPerformance,
            List<DistributionItemDto> gradePerformanceComparison,
            List<DistributionItemDto> careerPathwayDistribution,
            List<InsightItemDto> urgentInterventions
    ) {}

    public record DistrictAnalyticsResponse(
            List<TrendPointDto> districtPerformanceTrends,
            List<InsightItemDto> schoolRankingComparison,
            List<TrendPointDto> subjectPerformanceBySchool,
            List<DistributionItemDto> apsBandDistribution,
            List<DistributionItemDto> careerPathwayDistribution,
            List<DistributionItemDto> learnerRiskDistribution,
            List<DistributionItemDto> reportUploadCompletionProgress,
            List<DistributionItemDto> gradeClassPerformanceComparison,
            List<InsightItemDto> urgentInterventions
    ) {}

    public record DistrictAiInsightsResponse(
            boolean dataAvailable,
            String emptyStateMessage,
            List<InsightItemDto> schoolsAtRisk,
            List<InsightItemDto> learnersAtRiskBySchool,
            List<InsightItemDto> weakSubjectsAcrossDistrict,
            List<InsightItemDto> apsReadinessWarnings,
            List<InsightItemDto> careerPathwayGaps,
            List<InsightItemDto> bursaryFundingIndicators,
            List<InsightItemDto> teacherActivityAlerts,
            List<InsightItemDto> recommendedDistrictInterventions
    ) {}

    public record ReportItemDto(String key, String title, String description, boolean pdfSupported, boolean excelSupported) {}
    public record ReportExportRequest(@NotBlank String type, @NotBlank String format) {}

    public record AnnouncementCreateRequest(
            @NotBlank String audience,
            @NotBlank String title,
            @NotBlank String message,
            String deliveryScope,
            UUID schoolId
    ) {}

    public record AnnouncementItemDto(
            UUID id,
            String audience,
            String title,
            String message,
            String deliveryScope,
            UUID schoolId,
            String schoolName,
            String status,
            OffsetDateTime sentAt,
            OffsetDateTime createdAt
    ) {}

    public record DistrictInterventionCreateRequest(
            @NotBlank String title,
            @NotBlank String category,
            @NotBlank String priority,
            @NotBlank String notes,
            String targetScope,
            UUID schoolId,
            LocalDate followUpDate
    ) {}

    public record DistrictInterventionItemDto(
            UUID id,
            String title,
            String category,
            String priority,
            String status,
            String targetScope,
            UUID schoolId,
            String schoolName,
            String notes,
            LocalDate followUpDate,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record DistrictInterventionsResponse(
            String headline,
            List<MetricCardDto> metrics,
            List<InsightItemDto> interventionTypes,
            List<DistrictInterventionItemDto> items
    ) {}

    public record SupportRequestItemDto(
            UUID id,
            UUID schoolId,
            String schoolName,
            String category,
            String title,
            String message,
            String status,
            String priority,
            OffsetDateTime createdAt
    ) {}

    public record DistrictSettingsResponse(
            String districtName,
            String districtCode,
            String province,
            String contactEmail,
            String contactPhone,
            String licensingStatus,
            List<String> activeRoles,
            List<SchoolPortalDtos.PortalSettingsResponse> linkedSchoolSettings,
            List<SupportRequestItemDto> supportRequests,
            List<AnnouncementItemDto> announcements,
            List<AuditItemDto> recentAuditLogs
    ) {}

    public record AuditItemDto(String action, String entityType, UUID entityId, OffsetDateTime createdAt) {}

    public record SchoolRegistrationRequestItemDto(
            UUID requestId,
            UUID userId,
            UUID schoolId,
            String schoolName,
            String emisNumber,
            String province,
            String district,
            String circuit,
            String schoolType,
            String principalName,
            String principalEmail,
            String schoolEmail,
            String phoneNumber,
            String physicalAddress,
            String status,
            String rejectionReason,
            OffsetDateTime submittedAt,
            OffsetDateTime approvedAt,
            OffsetDateTime rejectedAt
    ) {}

    public record SchoolRegistrationRequestsResponse(
            List<MetricCardDto> metrics,
            List<SchoolRegistrationRequestItemDto> items,
            long total
    ) {}

    public record SchoolRegistrationDecisionRequest(
            @NotBlank String decision,
            String rejectionReason
    ) {}
}
