package com.edurite.bursary.dto;

import java.time.LocalDate;
import java.util.List;

public record BursaryResultDto(
        String externalId,
        String title,
        String provider,
        String description,
        String qualificationLevel,
        String region,
        String eligibility,
        LocalDate deadline,
        String applicationLink,
        String sourceType,
        int relevanceScore,
        List<String> sourceUrls,
        boolean officialSource,
        boolean incomplete,
        String dataFreshnessNote
) {}

