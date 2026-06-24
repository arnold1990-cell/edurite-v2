package com.edurite.cv.dto;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class StudentCvDtos {
    private StudentCvDtos() {
    }

    public record StudentCvUpsertRequest(
            @Size(max = 5000) String personalSummary,
            @Size(max = 8000) String education,
            @Size(max = 5000) String skills,
            @Size(max = 8000) String experience,
            @Size(max = 8000) String projects,
            @Size(max = 5000) String certifications,
            @Size(max = 5000) String references,
            @Size(max = 3000) String careerObjective
    ) {
    }

    public record StudentCvResponse(
            UUID id,
            String personalSummary,
            String education,
            String skills,
            String experience,
            String projects,
            String certifications,
            String references,
            String careerObjective,
            int readinessPercentage,
            OffsetDateTime updatedAt
    ) {
    }

    public record StudentCvSuggestionResponse(
            String summarySuggestion,
            String skillsImprovement,
            String coverLetterDraft,
            String jobReadinessTips
    ) {
    }
}

