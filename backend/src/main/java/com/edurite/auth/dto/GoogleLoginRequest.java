package com.edurite.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "idToken is required")
        String idToken,
        String role
) {
    public String resolvedRole() {
        if (role == null || role.isBlank()) {
            return "STUDENT";
        }

        return role.trim().toUpperCase();
    }
}

