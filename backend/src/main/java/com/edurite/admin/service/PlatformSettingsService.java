package com.edurite.admin.service;

import com.edurite.admin.dto.AdminPlatformSettingsDto;
import com.edurite.admin.dto.AdminPlatformSettingsUpdateRequest;
import com.edurite.admin.entity.PlatformSetting;
import com.edurite.admin.repository.PlatformSettingRepository;
import com.edurite.common.exception.ResourceConflictException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformSettingsService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_CSV_ROWS_MIN = 1;
    private static final int MAX_CSV_ROWS_MAX = 10000;

    private final PlatformSettingRepository platformSettingRepository;

    public PlatformSettingsService(PlatformSettingRepository platformSettingRepository) {
        this.platformSettingRepository = platformSettingRepository;
    }

    @Transactional
    public PlatformSetting getCurrentSettingsEntity() {
        return platformSettingRepository.findTopByOrderByCreatedAtAsc()
                .orElseGet(this::createDefaults);
    }

    @Transactional(readOnly = true)
    public AdminPlatformSettingsDto getCurrentSettings() {
        return toDto(getCurrentSettingsEntity());
    }

    @Transactional
    public AdminPlatformSettingsDto updateSettings(AdminPlatformSettingsUpdateRequest request) {
        PlatformSetting settings = getCurrentSettingsEntity();
        if (request.companySelfRegistrationEnabled() != null) {
            settings.setCompanySelfRegistrationEnabled(request.companySelfRegistrationEnabled());
        }
        if (request.manualCompanyApprovalRequired() != null) {
            settings.setManualCompanyApprovalRequired(request.manualCompanyApprovalRequired());
        }
        if (request.bursaryPostingEnabled() != null) {
            settings.setBursaryPostingEnabled(request.bursaryPostingEnabled());
        }
        if (request.studentRegistrationEnabled() != null) {
            settings.setStudentRegistrationEnabled(request.studentRegistrationEnabled());
        }
        if (request.bursaryModerationRequired() != null) {
            settings.setBursaryModerationRequired(request.bursaryModerationRequired());
        }
        if (request.aiGuidanceEnabled() != null) {
            settings.setAiGuidanceEnabled(request.aiGuidanceEnabled());
        }
        if (request.maintenanceModeEnabled() != null) {
            settings.setMaintenanceModeEnabled(request.maintenanceModeEnabled());
        }
        if (request.supportEmail() != null) {
            String email = request.supportEmail().trim();
            if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
                throw new ResourceConflictException("Support email is not valid");
            }
            settings.setSupportEmail(email.isEmpty() ? null : email);
        }
        if (request.platformContactInfo() != null) {
            String contact = request.platformContactInfo().trim();
            settings.setPlatformContactInfo(contact.isEmpty() ? null : contact);
        }
        if (request.maxCsvBulkUploadRows() != null) {
            int limit = request.maxCsvBulkUploadRows();
            if (limit < MAX_CSV_ROWS_MIN || limit > MAX_CSV_ROWS_MAX) {
                throw new ResourceConflictException("maxCsvBulkUploadRows must be between %d and %d".formatted(MAX_CSV_ROWS_MIN, MAX_CSV_ROWS_MAX));
            }
            settings.setMaxCsvBulkUploadRows(limit);
        }
        return toDto(platformSettingRepository.save(settings));
    }

    private PlatformSetting createDefaults() {
        PlatformSetting defaults = new PlatformSetting();
        return platformSettingRepository.save(defaults);
    }

    private AdminPlatformSettingsDto toDto(PlatformSetting settings) {
        return new AdminPlatformSettingsDto(
                settings.getId(),
                settings.isCompanySelfRegistrationEnabled(),
                settings.isManualCompanyApprovalRequired(),
                settings.isBursaryPostingEnabled(),
                settings.isStudentRegistrationEnabled(),
                settings.isBursaryModerationRequired(),
                settings.isAiGuidanceEnabled(),
                settings.isMaintenanceModeEnabled(),
                settings.getSupportEmail(),
                settings.getPlatformContactInfo(),
                settings.getMaxCsvBulkUploadRows(),
                settings.getUpdatedAt()
        );
    }
}

