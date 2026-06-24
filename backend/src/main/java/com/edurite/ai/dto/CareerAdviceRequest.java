package com.edurite.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record CareerAdviceRequest(
        @NotBlank(message = "qualificationLevel is required")
        String qualificationLevel,
        @NotBlank(message = "interests is required")
        String interests,
        @NotBlank(message = "skills is required")
        String skills,
        @NotBlank(message = "location is required")
        String location
) {
}

