package com.edurite.curriculum.dto;

import com.edurite.district.dto.DistrictDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class CurriculumDtos {
    private CurriculumDtos() {
    }

    public record FilePayload(
            String fileName,
            String contentType,
            String base64Content
    ) {}

    public record CurriculumAssetUpsertRequest(
            @NotBlank String repositoryType,
            @NotBlank String title,
            @NotBlank String subject,
            @NotBlank String grade,
            String curriculumPhase,
            Integer academicYear,
            String province,
            String versionNumber,
            String description,
            String term,
            Integer weekNumber,
            @Valid FilePayload pdf,
            @Valid FilePayload docx,
            @Valid FilePayload excel
    ) {}

    public record CurriculumAssetDto(
            UUID id,
            String repositoryType,
            String contentSource,
            String source,
            String visibility,
            String status,
            String extractionStatus,
            String extractionError,
            String badge,
            String title,
            String subject,
            String grade,
            String curriculumPhase,
            Integer academicYear,
            String province,
            String versionNumber,
            String description,
            String term,
            Integer weekNumber,
            String uploadedBy,
            OffsetDateTime uploadDate,
            OffsetDateTime extractedAt,
            boolean archived,
            boolean active,
            boolean deleted,
            boolean pdfAvailable,
            boolean docxAvailable,
            boolean excelAvailable,
            String scopeLabel,
            Boolean assignmentMatched,
            String assignmentMatchReason,
            String lessonPlanStatus,
            boolean generatedByAi
    ) {}

    public record CurriculumResourceQuery(
            String type,
            String subject,
            String grade,
            String phase,
            String term,
            Integer week,
            Integer academicYear
    ) {}

    public record CurriculumAssetDownloadResponse(
            String fileName,
            String contentType,
            String base64Content
    ) {}

    public record CurriculumWeekPlanDto(
            UUID id,
            String term,
            Integer weekNumber,
            String subject,
            String grade,
            String topic,
            String subtopic,
            String learningOutcomes,
            String assessmentActivities,
            String expectedCompletion
    ) {}

    public record CurriculumCalendarItemDto(
            UUID id,
            UUID curriculumResourceId,
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            String province,
            String term,
            Integer weekNumber,
            LocalDate startDate,
            LocalDate endDate,
            String topic,
            String subtopic,
            String learningObjectives,
            String resources,
            String assessmentTask,
            String lessonFocus,
            String notes,
            String status,
            String sourceTitle,
            String extractionStatus,
            boolean publishable,
            String tone
    ) {}

    public record CurriculumCalendarStatsDto(
            long atpsProcessed,
            long calendarItemsGenerated,
            long publishedTopics,
            long draftTopics,
            long teacherRemindersCreated,
            long extractionErrors
    ) {}

    public record DistrictCurriculumCalendarResponse(
            CurriculumCalendarStatsDto stats,
            List<CurriculumCalendarItemDto> items,
            List<CurriculumAssetDto> extractionErrors
    ) {}

    public record CurriculumCalendarItemUpsertRequest(
            UUID curriculumResourceId,
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            @NotBlank String term,
            @NotNull Integer weekNumber,
            LocalDate startDate,
            LocalDate endDate,
            @NotBlank String topic,
            String subtopic,
            String learningObjectives,
            String resources,
            String assessmentTask,
            String lessonFocus,
            String notes
    ) {}

    public record CurriculumHeatMapItemDto(
            String schoolName,
            String subject,
            String status,
            String tone,
            int compliancePercent
    ) {}

    public record CurriculumComplianceSchoolDto(
            UUID schoolId,
            String schoolName,
            int compliancePercent,
            String status,
            long teachersBehind,
            long subjectsBehind
    ) {}

    public record CurriculumRiskAlertDto(
            UUID id,
            UUID schoolId,
            String schoolName,
            UUID teacherUserId,
            String teacherName,
            String subject,
            String grade,
            String title,
            String detail,
            String severity,
            OffsetDateTime createdAt
    ) {}

    public record CurriculumComplianceResponse(
            List<DistrictDtos.MetricCardDto> metrics,
            List<CurriculumComplianceSchoolDto> schools,
            List<CurriculumHeatMapItemDto> heatMap,
            List<DistrictDtos.InsightItemDto> subjectsBehindSchedule,
            List<DistrictDtos.InsightItemDto> teachersBehindSchedule,
            List<CurriculumRiskAlertDto> riskAlerts
    ) {}

    public record TeacherReminderDto(
            String reminderType,
            String title,
            String message,
            String tone,
            OffsetDateTime reminderDate,
            String status
    ) {}

    public record TeacherCoverageItemDto(
            UUID weekPlanId,
            UUID curriculumResourceId,
            UUID atpCalendarItemId,
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            String term,
            Integer weekNumber,
            LocalDate startDate,
            LocalDate endDate,
            String topic,
            String subtopic,
            String learningObjectives,
            String resources,
            String assessmentTask,
            String lessonFocus,
            String notes,
            String sourceTitle,
            String status,
            int progressPercent,
            String completionLabel
        ) {}

    public record TeacherCurriculumWidgetResponse(
            TeacherCoverageItemDto currentTopic,
            TeacherCoverageItemDto thisWeeksCoverage,
            List<TeacherCoverageItemDto> topicsBehindSchedule,
            List<TeacherCoverageItemDto> visibleTopics,
            List<TeacherCoverageItemDto> upcomingTopics,
            List<TeacherReminderDto> reminders,
            List<CurriculumAssetDto> districtResources,
            List<CurriculumAssetDto> officialSyllabuses,
            List<CurriculumAssetDto> lessonPlans,
            long totalPublishedAtpCalendarItems,
            long currentTeacherAtpMatches,
            TeacherCoverageItemDto currentWeekAtpItem
    ) {}

    public record TeacherProgressUpdateRequest(
            @NotBlank String status,
            @Min(0) @Max(100) Integer completionPercent,
            String notes
    ) {}

    public record TeacherLessonPlanGenerateRequest(
            @NotNull UUID weekPlanId
    ) {}

    public record TeacherLessonPlanCreateRequest(
            Boolean regenerate
    ) {}

    public record LessonPlanStageDto(
            String stage,
            String duration,
            String teacherActivities,
            String learnerActivities
    ) {}

    public record TeacherLessonPlanDayDto(
            String day,
            String topicContent,
            String objectives,
            String sourceOfMatter,
            String media,
            String lessonActivities,
            String evaluation
    ) {}

    public record LessonPlanGenerationRequest(
            UUID sourceAtpCalendarItemId,
            UUID sourceCurriculumAssetId,
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            String term,
            Integer weekNumber,
            String topic,
            LocalDate lessonDate,
            Integer lessonDurationMinutes,
            UUID classId,
            UUID teacherUserId,
            String language,
            String availableResources,
            String additionalInstructions,
            Boolean regenerate
    ) {}

    public record LessonPlanDraftSaveRequest(
            String title,
            String schoolName,
            String teacherName,
            String className,
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            String term,
            Integer weekNumber,
            String topic,
            LocalDate lessonDate,
            Integer lessonDurationMinutes,
            String language,
            String sourceAtpTitle,
            String curriculumReferences,
            String priorKnowledge,
            String availableResources,
            String additionalInstructions,
            String learningObjectives,
            String introduction,
            String teacherActivities,
            String learnerActivities,
            String differentiation,
            String assessment,
            String homework,
            String reflection,
            List<LessonPlanStageDto> stages
    ) {}

    public record TeacherLessonPlanResponse(
            UUID lessonPlanAssetId,
            UUID sourceCalendarItemId,
            UUID sourceCurriculumAssetId,
            boolean alreadyExisted,
            boolean pdfAvailable,
            boolean docxAvailable,
            String lessonPlanStatus,
            boolean generatedByAi,
            String title,
            String schoolName,
            String teacherName,
            String className,
            String weekEnding,
            String subtopic,
            String sourceAtpTitle,
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            String term,
            Integer weekNumber,
            String topic,
            LocalDate lessonDate,
            Integer lessonDurationMinutes,
            String language,
            String curriculumReferences,
            String priorKnowledge,
            String availableResources,
            String additionalInstructions,
            String learningObjectives,
            String introduction,
            String activities,
            String learnerActivities,
            String resources,
            String assessment,
            String homework,
            String differentiation,
            String reflection,
            List<LessonPlanStageDto> stages,
            List<TeacherLessonPlanDayDto> days,
            OffsetDateTime aiGeneratedAt,
            String aiProvider,
            String aiModel
    ) {}

    public record SchoolCurriculumCalendarResponse(
            TeacherCoverageItemDto thisWeeksCoverage,
            List<TeacherCoverageItemDto> upcomingTopics,
            List<TeacherCoverageItemDto> visibleTopics,
            List<TeacherCoverageItemDto> subjectsBehindSchedule,
            List<TeacherReminderDto> reminders,
            List<CurriculumAssetDto> districtResources,
            long totalPublishedAtpCalendarItems,
            TeacherCoverageItemDto currentWeekAtpItem,
            long currentSchoolAtpMatches,
            long itemsVisibleToSchool,
            long itemsMatchingCurrentWeek,
            long itemsWithNullDates,
            long districtWideItems,
            long remindersQueuedForSchool
    ) {}

    public record CurriculumPublishRepairResponse(
            long publishedItems,
            long weekPlansSynced,
            long schoolRemindersQueued,
            long teacherRemindersQueued,
            String message
    ) {}
}
