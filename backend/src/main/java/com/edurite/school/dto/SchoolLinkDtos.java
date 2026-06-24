package com.edurite.school.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class SchoolLinkDtos {
    private SchoolLinkDtos() {
    }

    public record PublicSchoolDto(UUID id, String name, String schoolCode) {}

    public record StudentSchoolRequest(@NotNull UUID schoolId) {}

    public record StudentSchoolStatusDto(
            String status,
            PublicSchoolDto school,
            String generatedUsername,
            String learnerGrade,
            String learnerClassName,
            String message,
            OffsetDateTime requestedAt,
            OffsetDateTime approvedAt,
            OffsetDateTime rejectedAt
    ) {}

    public record SchoolJoinRequestItemDto(
            UUID requestId,
            UUID studentId,
            String learnerFullName,
            String learnerEmail,
            String learnerPhone,
            UUID schoolId,
            String schoolName,
            String schoolCode,
            OffsetDateTime requestedAt,
            String status,
            String generatedUsername
    ) {}
}
