package com.edurite.student.dto;

public record StudentSubjectAchievementDto(
        String subjectName,
        Integer achievementLevel,
        Integer markPercentage,
        String source,
        boolean verified,
        String documentId,
        String updatedAt
) {
    public StudentSubjectAchievementDto(String subjectName, Integer achievementLevel) {
        this(subjectName, achievementLevel, null, "MANUAL", false, null, null);
    }
}

