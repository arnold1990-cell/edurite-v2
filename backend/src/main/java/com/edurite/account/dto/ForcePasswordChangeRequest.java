package com.edurite.account.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForcePasswordChangeRequest(
        @NotBlank(message = "currentPassword is required")
        @Size(min = 8, max = 100, message = "currentPassword must be between 8 and 100 characters")
        String currentPassword,
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters")
        String newPassword,
        @JsonAlias("confirmPassword")
        @NotBlank(message = "confirmNewPassword is required")
        @Size(min = 8, max = 100, message = "confirmNewPassword must be between 8 and 100 characters")
        String confirmNewPassword
) {
}
