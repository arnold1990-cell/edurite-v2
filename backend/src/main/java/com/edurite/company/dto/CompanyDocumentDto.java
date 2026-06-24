package com.edurite.company.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyDocumentDto(
        UUID id,
        String documentType,
        String objectKey,
        String verificationStatus,
        String fileName,
        OffsetDateTime createdAt
) {
}

