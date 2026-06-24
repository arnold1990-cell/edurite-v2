package com.edurite.student.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentSavedProfileDto(
        UUID id,
        String name,
        StudentSavedProfilePayload profile,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

