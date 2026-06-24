package com.edurite.course.dto;

import java.util.UUID;

public record CourseDto(
        UUID id,
        String name,
        String institutionName,
        String duration,
        String level,
        Integer matchScore
) {
}

