package com.edurite.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminSchoolDtos {
    private AdminSchoolDtos() {
    }

    public record AdminSchoolMetricDto(String label, String value, String helperText) {}

    public record AdminSchoolItemDto(
            UUID id,
            String schoolName,
            String emisNumber,
            String schoolCode,
            UUID districtId,
            String districtName,
            String province,
            String circuit,
            String schoolType,
            String principalName,
            String schoolEmail,
            String contactNumber,
            String physicalAddress,
            String status,
            boolean hasSchoolAdmin,
            String username,
            String temporaryPassword,
            OffsetDateTime createdAt
    ) {}

    public record AdminSchoolManagementResponse(
            List<AdminSchoolMetricDto> metrics,
            List<AdminSchoolItemDto> items
    ) {}

    public record AdminWhitelistSchoolRequest(
            @NotBlank String schoolName,
            @NotBlank String emisNumber,
            String schoolCode,
            String province,
            @NotNull UUID districtId,
            UUID registeredSchoolId,
            String entrySource,
            String circuit,
            @Email @NotBlank String schoolEmail,
            @NotBlank String contactNumber,
            @NotBlank String principalName,
            @NotBlank String physicalAddress,
            @NotBlank String schoolType,
            @NotBlank String status
    ) {}

    public record AdminCredentialResponse(
            UUID id,
            String username,
            String temporaryPassword
    ) {}
}
