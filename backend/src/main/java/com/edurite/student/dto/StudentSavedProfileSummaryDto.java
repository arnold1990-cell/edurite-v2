package com.edurite.student.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentSavedProfileSummaryDto(
        UUID id,
        String name,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

