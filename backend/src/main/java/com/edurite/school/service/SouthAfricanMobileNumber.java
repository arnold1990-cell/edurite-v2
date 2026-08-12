package com.edurite.school.service;

import com.edurite.common.exception.ResourceConflictException;

public final class SouthAfricanMobileNumber {

    private SouthAfricanMobileNumber() {
    }

    public static String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResourceConflictException("Enter a valid South African mobile number.");
        }
        return normalized;
    }

    public static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String compact = value.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");
        if (compact.startsWith("+27")) {
            compact = compact.substring(3);
        } else if (compact.startsWith("0027")) {
            compact = compact.substring(4);
        } else if (compact.startsWith("27")) {
            compact = compact.substring(2);
        }
        if (compact.startsWith("0")) {
            compact = compact.substring(1);
        }
        if (!compact.matches("[6-8][0-9]{8}")) {
            return null;
        }
        return "+27" + compact;
    }

    public static String mask(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.length() < 6) {
            return "+27******";
        }
        return "+27******" + normalized.substring(normalized.length() - 3);
    }
}
