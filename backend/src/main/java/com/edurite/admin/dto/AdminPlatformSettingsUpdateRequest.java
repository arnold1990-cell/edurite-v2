package com.edurite.admin.dto;

public record AdminPlatformSettingsUpdateRequest(
        Boolean companySelfRegistrationEnabled,
        Boolean manualCompanyApprovalRequired,
        Boolean bursaryPostingEnabled,
        Boolean studentRegistrationEnabled,
        Boolean bursaryModerationRequired,
        Boolean aiGuidanceEnabled,
        Boolean maintenanceModeEnabled,
        String supportEmail,
        String platformContactInfo,
        Integer maxCsvBulkUploadRows
) {
}

