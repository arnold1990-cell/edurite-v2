package com.edurite.psychometric.dto;

import java.util.UUID;

public record PsychometricQuestionDto(
        UUID id,
        String questionKey,
        String prompt,
        String dimensionKey,
        int minScore,
        int maxScore,
        int displayOrder
) {
}

