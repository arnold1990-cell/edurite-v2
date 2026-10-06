package com.edurite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email String email,
        @NotBlank @Size(max = 30) String phoneNumber,
        @NotBlank @Size(min = 8, max = 100) String password,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.AssertTrue Boolean popiaConsentAccepted
) {
}

