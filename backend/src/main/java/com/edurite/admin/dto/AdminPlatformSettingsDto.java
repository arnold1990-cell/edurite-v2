package com.edurite.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminPlatformSettingsDto(
        UUID id,
        boolean companySelfRegistrationEnabled,
        boolean manualCompanyApprovalRequired,
        boolean bursaryPostingEnabled,
        boolean studentRegistrationEnabled,
        boolean bursaryModerationRequired,
        boolean aiGuidanceEnabled,
        boolean maintenanceModeEnabled,
        String supportEmail,
        String platformContactInfo,
        int maxCsvBulkUploadRows,
        OffsetDateTime updatedAt
) {
}

