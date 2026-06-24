package com.edurite.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record ForgotPasswordOtpRequest(
        @Size(max = 255, message = "accountIdentifier must be at most 255 characters")
        String accountIdentifier,
        @Size(max = 30, message = "phoneNumber must be at most 30 characters")
        String phoneNumber
) {
    @AssertTrue(message = "phoneNumber or accountIdentifier is required")
    public boolean hasIdentifier() {
        return !isBlank(accountIdentifier) || !isBlank(phoneNumber);
    }

    public String resolvedIdentifier() {
        if (!isBlank(accountIdentifier)) {
            return accountIdentifier.trim();
        }
        return phoneNumber == null ? null : phoneNumber.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

