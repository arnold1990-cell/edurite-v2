package com.edurite.psychometric.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PsychometricSubmissionRequest(
        @NotEmpty(message = "answers are required")
        List<AnswerItem> answers
) {
    public record AnswerItem(
            @NotBlank(message = "dimension is required")
            String dimension,
            @Min(value = 1, message = "score must be at least 1")
            @Max(value = 5, message = "score must be at most 5")
            int score
    ) {
    }
}

