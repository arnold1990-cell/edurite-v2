package com.edurite.company.dto;

import com.edurite.company.entity.CompanyApprovalStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyProfileDto(
        UUID id,
        String companyName,
        String registrationNumber,
        String industry,
        String officialEmail,
        String mobileNumber,
        String contactPersonName,
        String address,
        String website,
        String description,
        CompanyApprovalStatus status,
        boolean emailVerified,
        boolean mobileVerified,
        OffsetDateTime reviewedAt,
        UUID reviewedBy,
        String reviewNotes
) {
}

