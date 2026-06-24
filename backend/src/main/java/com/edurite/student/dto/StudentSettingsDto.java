package com.edurite.student.dto;

public record StudentSettingsDto(
        boolean inAppNotificationsEnabled,
        boolean emailNotificationsEnabled,
        boolean smsNotificationsEnabled
) {}

