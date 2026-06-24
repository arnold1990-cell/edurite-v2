package com.edurite.account.dto;

// Validation annotations to ensure correct input
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * This DTO is used when a user wants to delete their account.
 *
 * It receives data from the frontend (API request).
 *
 * Using a "record" makes it:
 * - Immutable (cannot be changed after creation)
 * - Cleaner (no getters/setters needed)
 */
public record DeleteAccountRequest(

        /**
         * This is a confirmation text entered by the user.
         *
         * Example:
         * The system may require the user to type:
         * "DELETE" or "CONFIRM"
         *
         * This prevents accidental account deletion.
         */
        @NotBlank(message = "confirmationText is required")
        String confirmationText,

        /**
         * Optional reason for deleting the account.
         *
         * Example:
         * "I no longer need the service"
         *
         * Limited to 255 characters to prevent overly long input.
         */
        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason
) {
}
