package com.edurite.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StudentRegisterRequest(
        @Size(max = 200, message = "fullName must be at most 200 characters")
        String fullName,
        @Size(max = 100, message = "firstName must be at most 100 characters")
        String firstName,
        @Size(max = 100, message = "lastName must be at most 100 characters")
        String lastName,
        @Size(max = 255, message = "interests must be at most 255 characters")
        String interests,
        @Size(max = 255, message = "location must be at most 255 characters")
        String location,
        @NotBlank(message = "phone is required")
        @Size(max = 50, message = "phone must be at most 50 characters")
        String phone,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateOfBirth,
        @Size(max = 50, message = "gender must be at most 50 characters")
        String gender,
        @Size(max = 100, message = "qualificationLevel must be at most 100 characters")
        String qualificationLevel,
        @NotNull(message = "POPIA consent is required")
        @AssertTrue(message = "POPIA consent is required")
        Boolean popiaConsentAccepted,
        @Size(max = 40, message = "consentVersion must be at most 40 characters")
        String consentVersion,
        @Email(message = "email must be a valid email address")
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password
) {
}

