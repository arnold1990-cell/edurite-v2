package com.edurite.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String fullName,
        String email,
        List<String> roles,
        String primaryRole,
        String status,
        boolean active,
        String companyApprovalStatus,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
}

