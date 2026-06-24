package com.edurite.learning.dto;

import java.util.List;

public record LearningResourceDto(
        String id,
        String title,
        String description,
        String provider,
        String category,
        String subject,
        String grade,
        String level,
        String resourceType,
        String duration,
        String thumbnailUrl,
        String externalUrl,
        Integer progress,
        String instructor,
        List<String> lessons
) {
}

