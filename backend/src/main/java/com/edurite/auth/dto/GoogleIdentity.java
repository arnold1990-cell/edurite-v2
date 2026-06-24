package com.edurite.auth.dto;

public record GoogleIdentity(
        String email,
        String firstName,
        String lastName,
        String fullName
) {
}

