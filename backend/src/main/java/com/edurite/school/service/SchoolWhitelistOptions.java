package com.edurite.school.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SchoolWhitelistOptions {

    public static final List<String> SUPPORTED_PROVINCES = List.of(
            "Eastern Cape",
            "Free State",
            "Gauteng",
            "KwaZulu-Natal",
            "Limpopo",
            "Mpumalanga",
            "North West",
            "Northern Cape",
            "Western Cape"
    );

    public static final List<String> SUPPORTED_SCHOOL_TYPES = List.of(
            "Primary School",
            "Secondary School",
            "Combined School",
            "Special School",
            "Independent School",
            "Other"
    );

    private SchoolWhitelistOptions() {
    }

    public static Optional<String> canonicalProvince(String value) {
        return canonical(value, SUPPORTED_PROVINCES);
    }

    public static Optional<String> canonicalSchoolType(String value) {
        return canonical(value, SUPPORTED_SCHOOL_TYPES);
    }

    private static Optional<String> canonical(String value, List<String> supportedValues) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return supportedValues.stream()
                .filter(item -> item.toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }
}
