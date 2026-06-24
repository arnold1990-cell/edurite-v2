package com.edurite.scholarship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ScholarshipApplicationDtos {
    private ScholarshipApplicationDtos() {
    }

    public record ScholarshipApplicationRequest(
            UUID bursaryId,
            @NotBlank @Size(max = 255) String scholarshipTitle,
            @Size(max = 255) String provider,
            LocalDate applicationDeadline,
            @Size(max = 40) String status,
            @Size(max = 8000) String checklist,
            @Size(max = 8000) String requiredDocuments,
            @Size(max = 3000) String reminderNotes,
            @Size(max = 8000) String motivationLetterDraft,
            Boolean saved,
            @Size(max = 5000) String notes
    ) {
    }

    public record ScholarshipApplicationResponse(
            UUID id,
            UUID bursaryId,
            String scholarshipTitle,
            String provider,
            LocalDate applicationDeadline,
            String status,
            String checklist,
            String requiredDocuments,
            String reminderNotes,
            String motivationLetterDraft,
            boolean saved,
            String notes,
            boolean deadlineSoon,
            OffsetDateTime updatedAt
    ) {
    }

    public record MotivationLetterResponse(String motivationLetterDraft) {
    }
}

