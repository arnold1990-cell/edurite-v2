package com.edurite.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "idToken is required")
        String idToken,
        String role,
        Boolean popiaConsentAccepted
) {
    public GoogleLoginRequest(String idToken, String role) { this(idToken, role, false); }

    public String resolvedRole() {
        if (role == null || role.isBlank()) {
            return "STUDENT";
        }

        return role.trim().toUpperCase();
    }
}

