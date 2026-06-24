package com.edurite.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminRecentCompanyDto(
        UUID id,
        String companyName,
        String officialEmail,
        String status,
        OffsetDateTime createdAt
) {
}

