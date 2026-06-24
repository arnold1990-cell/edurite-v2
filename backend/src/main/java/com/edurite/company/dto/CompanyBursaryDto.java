package com.edurite.company.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CompanyBursaryDto(
        UUID id,
        String bursaryName,
        String description,
        String fieldOfStudy,
        String academicLevel,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        BigDecimal fundingAmount,
        String benefits,
        String requiredSubjects,
        String minimumGrade,
        String demographics,
        String location,
        String eligibility,
        String status,
        long applicantCount,
        long views,
        double applicationCompletionRate,
        long profileViews
) {
}

