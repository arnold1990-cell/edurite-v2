package com.edurite.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @NotBlank(message = "phoneNumber is required")
        @Size(max = 30, message = "phoneNumber must be at most 30 characters")
        String phoneNumber,
        @NotBlank(message = "code is required")
        @Size(min = 4, max = 10, message = "code must be between 4 and 10 characters")
        String code
) {
}

