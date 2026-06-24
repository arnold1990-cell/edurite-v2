package com.edurite.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevPasswordResetRequest(
        @NotBlank(message = "email is required")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters")
        String newPassword
) {
}

