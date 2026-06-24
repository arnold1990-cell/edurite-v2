package com.edurite.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompanyBursaryUpsertRequest(
        @NotBlank String bursaryName,
        @NotBlank String description,
        @NotBlank String fieldOfStudy,
        @NotBlank String academicLevel,
        @NotNull LocalDate applicationStartDate,
        @NotNull LocalDate applicationEndDate,
        @NotNull BigDecimal fundingAmount,
        String benefits,
        List<String> requiredSubjects,
        String minimumGrade,
        List<String> demographics,
        String location,
        List<String> eligibilityFilters
) {
}

