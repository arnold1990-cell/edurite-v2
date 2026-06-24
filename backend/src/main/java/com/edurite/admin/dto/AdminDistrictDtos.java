package com.edurite.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminDistrictDtos {
    private AdminDistrictDtos() {
    }

    public record AdminDistrictMetricDto(
            String label,
            String value,
            String helperText
    ) {}

    public record AdminDistrictItemDto(
            UUID id,
            String districtCode,
            String districtName,
            String directorName,
            String adminName,
            String adminEmail,
            String phoneNumber,
            String address,
            String status,
            boolean active,
            long schoolCount,
            long pendingRegistrations,
            boolean hasAssignedAdmin,
            String warningMessage,
            String username,
            String temporaryPassword,
            OffsetDateTime createdAt
    ) {}

    public record AdminDistrictManagementResponse(
            List<AdminDistrictMetricDto> metrics,
            List<AdminDistrictItemDto> items
    ) {}

    public record AdminCreateDistrictRequest(
            @NotBlank String districtName,
            @NotBlank String districtCode,
            @NotBlank String directorName,
            @NotBlank String adminName,
            @Email @NotBlank String adminEmail,
            @NotBlank String phoneNumber,
            @NotBlank String physicalAddress,
            @NotBlank String status
    ) {}

    public record AdminUpdateDistrictRequest(
            @NotBlank String directorName,
            @NotBlank String adminName,
            @Email @NotBlank String adminEmail,
            @NotBlank String phoneNumber,
            @NotBlank String status
    ) {}
}
