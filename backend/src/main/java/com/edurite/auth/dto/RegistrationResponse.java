package com.edurite.auth.dto;

public record RegistrationResponse(
        String message,
        String email,
        boolean verificationRequired,
        String accessToken,
        String refreshToken,
        String tokenType,
        Long accessTokenExpiresIn,
        String role,
        String primaryRole,
        String approvalStatus,
        Boolean mustChangePassword,
        AuthResponse.UserSummary user
) {
    public RegistrationResponse(String message, String email, boolean verificationRequired) {
        this(message, email, verificationRequired, null, null, null, null, null, null, null, null, null);
    }
}
