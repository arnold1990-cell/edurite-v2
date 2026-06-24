package com.edurite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SchoolRegisterRequest(
        @NotBlank String schoolName,
        @NotBlank String emisNumber,
        @NotNull UUID districtId,
        @NotNull UUID circuitId,
        @NotBlank String schoolType,
        @NotBlank String principalName,
        @Email @NotBlank String principalEmail,
        @Email @NotBlank String schoolEmail,
        @NotBlank @Size(max = 30) String phoneNumber,
        @NotBlank String physicalAddress,
        @NotBlank String password,
        @NotBlank String confirmPassword
) {
}
