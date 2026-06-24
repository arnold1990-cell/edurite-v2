package com.edurite.student.dto;

import java.util.List;
import java.util.Map;

public record StudentPreferencesDto(
        List<String> preferredIndustries,
        List<String> preferredLocations,
        Map<String, Object> notificationPreferences,
        Map<String, Object> extra
) {
}

