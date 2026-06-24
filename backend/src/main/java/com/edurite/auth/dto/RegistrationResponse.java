package com.edurite.auth.dto;

public record RegistrationResponse(String message, String email, boolean verificationRequired) {
}

