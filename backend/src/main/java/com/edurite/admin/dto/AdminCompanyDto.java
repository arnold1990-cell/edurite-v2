package com.edurite.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminCompanyDto(
        UUID id,
        UUID userId,
        String companyName,
        String registrationNumber,
        String officialEmail,
        String industry,
        String status,
        String reviewNotes,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
}

