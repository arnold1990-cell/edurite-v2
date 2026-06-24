package com.edurite.admin.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminRecentBursaryDto(
        UUID id,
        String title,
        UUID companyId,
        String status,
        LocalDate applicationEndDate,
        OffsetDateTime createdAt
) {
}

