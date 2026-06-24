package com.edurite.admin.dto;

import java.util.UUID;

public record AdminApplicationsPerBursaryDto(
        UUID bursaryId,
        String bursaryTitle,
        long totalApplications
) {
}

