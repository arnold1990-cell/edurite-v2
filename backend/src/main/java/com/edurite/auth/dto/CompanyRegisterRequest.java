package com.edurite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompanyRegisterRequest(
        @NotBlank @Size(max = 200) String companyName,
        @Size(max = 120) String registrationNumber,
        @Size(max = 120) String industry,
        @Email String officialEmail,
        @Email String email,
        @NotBlank @Size(max = 30) String mobileNumber,
        @Size(max = 150) String contactPersonName,
        @Size(max = 255) String address,
        @Size(max = 255) String website,
        @Size(max = 1000) String description,
        @NotNull(message = "POPIA consent is required") @AssertTrue(message = "POPIA consent is required") Boolean popiaConsentAccepted,
        @Size(max = 40) String consentVersion,
        @NotBlank @Size(min = 8, max = 255) String password
) {}

