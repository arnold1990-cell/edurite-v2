package com.edurite.universityapplication.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class UniversityApplicationDtos {
    private UniversityApplicationDtos() {
    }

    public record UniversityApplicationRequest(
            @NotBlank @Size(max = 255) String universityName,
            @NotBlank @Size(max = 255) String programmeName,
            @Size(max = 120) String country,
            @Min(2020) @Max(2100) Integer intakeYear,
            LocalDate applicationDeadline,
            @Size(max = 40) String applicationStatus,
            @Size(max = 5000) String notes,
            @Size(max = 8000) String documentReferences
    ) {
    }

    public record UniversityApplicationResponse(
            UUID id,
            String universityName,
            String programmeName,
            String country,
            Integer intakeYear,
            LocalDate applicationDeadline,
            String applicationStatus,
            String notes,
            String documentReferences,
            boolean deadlineSoon,
            OffsetDateTime updatedAt
    ) {
    }
}

