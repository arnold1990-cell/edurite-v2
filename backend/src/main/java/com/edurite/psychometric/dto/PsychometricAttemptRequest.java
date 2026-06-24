package com.edurite.psychometric.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PsychometricAttemptRequest(
        @NotEmpty(message = "At least one answer is required")
        @Valid
        List<AnswerItem> answers
) {
    public record AnswerItem(
            @NotNull(message = "questionId is required")
            UUID questionId,
            @NotNull(message = "score is required")
            @Min(value = 1, message = "score must be at least 1")
            @Max(value = 5, message = "score must be at most 5")
            Integer score
    ) {
    }
}

