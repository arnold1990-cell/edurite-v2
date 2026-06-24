package com.edurite.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminRecentUserDto(
        UUID id,
        String fullName,
        String email,
        List<String> roles,
        OffsetDateTime createdAt
) {
}

