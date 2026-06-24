package com.edurite.district.dto;

import com.edurite.curriculum.dto.CurriculumDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DistrictEducationDtos {
    private DistrictEducationDtos() {
    }

    public record RoleDashboardResponse(
            String title,
            String subtitle,
            List<DistrictDtos.MetricCardDto> metrics
    ) {}

    public record CircuitSchoolRowDto(
            UUID schoolId,
            String schoolName,
            String principal,
            long learners,
            long teachers,
            int passRate,
            int atpCompliance,
            int attendance,
            String riskStatus
    ) {}

    public record CircuitSchoolsResponse(
            List<DistrictDtos.MetricCardDto> metrics,
            List<CircuitSchoolRowDto> items
    ) {}

    public record CircuitCurriculumRowDto(
            UUID schoolId,
            String schoolName,
            String subject,
            String grade,
            String term,
            Integer expectedWeek,
            Integer actualWeek,
            String topicExpected,
            String topicCompleted,
            int weeksBehind,
            String status,
            String riskTone
    ) {}

    public record CircuitCurriculumResponse(List<CircuitCurriculumRowDto> items) {}

    public record SchoolVisitDto(
            UUID id,
            UUID schoolId,
            String schoolName,
            LocalDate visitDate,
            String purpose,
            String status,
            String notes,
            String outcome,
            OffsetDateTime createdAt
    ) {}

    public record SchoolVisitUpsertRequest(
            @NotNull UUID schoolId,
            @NotNull LocalDate visitDate,
            @NotBlank String purpose,
            String notes,
            String status,
            String outcome
    ) {}

    public record SupportRequestDto(
            UUID id,
            UUID schoolId,
            String schoolName,
            String requestType,
            String subject,
            String grade,
            String description,
            String status,
            UUID assignedTo,
            String assignedToName,
            OffsetDateTime createdAt
    ) {}

    public record SupportRequestUpdateRequest(
            String status,
            UUID assignedTo
    ) {}

    public record DistrictInterventionDto(
            UUID id,
            String title,
            String description,
            String interventionType,
            String priority,
            String status,
            UUID schoolId,
            String schoolName,
            UUID teacherId,
            String teacherName,
            String subject,
            String grade,
            UUID assignedTo,
            String assignedToName,
            LocalDate dueDate,
            String supportPlan,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record DistrictInterventionUpsertRequest(
            @NotBlank String title,
            String description,
            String interventionType,
            String priority,
            String status,
            UUID schoolId,
            UUID teacherId,
            String subject,
            String grade,
            UUID assignedTo,
            LocalDate dueDate,
            String notes
    ) {}

    public record AiSupportPlanResponse(
            UUID interventionId,
            String supportPlan
    ) {}

    public record AdvisorTeacherRowDto(
            UUID teacherUserId,
            String teacherName,
            UUID schoolId,
            String schoolName,
            String subject,
            String grade,
            long classes,
            Integer atpWeek,
            Integer expectedWeek,
            String status,
            int averageMark
    ) {}

    public record AdvisorTeachersResponse(
            List<DistrictDtos.MetricCardDto> metrics,
            List<AdvisorTeacherRowDto> items
    ) {}

    public record TeacherProfileResponse(
            UUID teacherUserId,
            String teacherName,
            String schoolName,
            List<String> assignedSubjects,
            List<String> assignedClasses,
            CurriculumDtos.TeacherCoverageItemDto currentCoverage,
            List<CurriculumDtos.TeacherCoverageItemDto> topicsBehind,
            int attendanceSubmissionRate,
            int averageMark,
            List<DistrictInterventionDto> interventions
    ) {}

    public record CommonAssessmentDto(
            UUID id,
            String title,
            String subject,
            String grade,
            String term,
            LocalDate date,
            Integer totalMarks,
            LocalDate dueDate,
            String badge,
            CurriculumDtos.CurriculumAssetDto asset
    ) {}

    public record CommonAssessmentCreateRequest(
            @NotBlank String title,
            @NotBlank String subject,
            @NotBlank String grade,
            @NotBlank String term,
            LocalDate date,
            Integer totalMarks,
            LocalDate dueDate,
            @NotNull CurriculumDtos.CurriculumAssetUpsertRequest asset
    ) {}
}
