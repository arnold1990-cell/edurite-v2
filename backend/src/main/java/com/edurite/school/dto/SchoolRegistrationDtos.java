package com.edurite.school.dto;

import com.edurite.school.portal.entity.SchoolStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class SchoolRegistrationDtos {
    private SchoolRegistrationDtos() {
    }

    public record SchoolRegistrationStatusResponse(
            UUID requestId,
            String schoolName,
            String emisNumber,
            String province,
            String district,
            String circuit,
            String schoolType,
            String principalName,
            String principalEmail,
            String schoolEmail,
            String phoneNumber,
            String physicalAddress,
            SchoolStatus status,
            String rejectionReason,
            OffsetDateTime submittedAt,
            OffsetDateTime approvedAt,
            OffsetDateTime rejectedAt
    ) {}
}
