package com.edurite.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @JsonAlias({"username", "accountIdentifier"})
        @Size(max = 255, message = "email or username must be at most 255 characters")
        String email,
        @Size(max = 255, message = "school name must be at most 255 characters")
        String schoolName,
        @Size(max = 120, message = "EMIS number must be at most 120 characters")
        String emisNumber,
        @NotBlank(message = "password is required")
        String password
) {
    public LoginRequest(String email, String password) {
        this(email, null, null, password);
    }

    public String resolvedIdentifier() {
        return email == null ? null : email.trim();
    }

    public String resolvedSchoolName() {
        return schoolName == null ? null : schoolName.trim();
    }

    public String resolvedEmisNumber() {
        return emisNumber == null ? null : emisNumber.trim();
    }
}

