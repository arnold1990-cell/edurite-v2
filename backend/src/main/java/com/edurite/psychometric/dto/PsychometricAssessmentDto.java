package com.edurite.psychometric.dto;

import java.util.UUID;

public record PsychometricAssessmentDto(
        UUID id,
        String code,
        String name,
        String description,
        String version,
        boolean publicAvailable,
        int questionCount
) {
}

