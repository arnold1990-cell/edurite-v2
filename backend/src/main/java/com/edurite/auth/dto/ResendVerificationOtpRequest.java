package com.edurite.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationOtpRequest(
        @NotBlank(message = "phoneNumber is required")
        @Size(max = 30, message = "phoneNumber must be at most 30 characters")
        String phoneNumber
) {
}

