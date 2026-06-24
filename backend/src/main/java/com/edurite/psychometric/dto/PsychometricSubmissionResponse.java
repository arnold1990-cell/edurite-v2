package com.edurite.psychometric.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PsychometricSubmissionResponse(
        UUID id,
        String submissionMode,
        Map<String, Double> scores,
        List<String> strengthAreas,
        List<String> growthAreas,
        String interpretation,
        String createdAt
) {
}

