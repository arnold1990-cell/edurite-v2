package com.edurite.account.dto;

// Validation annotations to ensure incoming data is correct
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * This is a DTO (Data Transfer Object).
 *
 * It is used to receive data from the client (frontend / API request)
 * when a user wants to change their password using an OTP (code).
 *
 * A "record" in Java is a simple way to create an immutable data class.
 * - No setters
 * - Fields are final
 * - Automatically creates constructor + getters
 */
public record ChangePasswordWithOtpRequest(

        /**
         * The user's current password.
         *
         * Must not be empty.
         * Must be between 8 and 100 characters.
         */
        @NotBlank(message = "currentPassword is required")
        @Size(min = 8, max = 100, message = "currentPassword must be between 8 and 100 characters")
        String currentPassword,

        /**
         * The OTP (One-Time Password) or verification code.
         *
         * Must not be empty.
         * Must be between 4 and 10 characters.
         */
        @NotBlank(message = "code is required")
        @Size(min = 4, max = 10, message = "code must be between 4 and 10 characters")
        String code,

        /**
         * The new password the user wants to set.
         *
         * Must not be empty.
         * Must be between 8 and 100 characters.
         */
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters")
        String newPassword
) {
}
