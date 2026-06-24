package com.edurite.admin.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminBursaryDto(
        UUID id,
        String title,
        UUID companyId,
        String companyName,
        String status,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        long applicantCount,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
}

