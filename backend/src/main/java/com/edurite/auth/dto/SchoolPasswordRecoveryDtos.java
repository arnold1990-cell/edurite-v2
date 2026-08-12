package com.edurite.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SchoolPasswordRecoveryDtos {
    private SchoolPasswordRecoveryDtos() {
    }

    public record ForgotPasswordRequest(
            @NotBlank @Size(max = 120) String emis
    ) {}

    public record VerifyOtpRequest(
            @NotBlank @Size(max = 120) String emis,
            @NotBlank @Size(min = 6, max = 6) String otp
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String resetToken,
            @NotBlank String newPassword,
            @NotBlank String confirmPassword
    ) {}

    public record RecoveryResponse(
            String message,
            String maskedMobileNumber,
            String resetToken
    ) {}
}
