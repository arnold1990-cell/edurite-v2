package com.edurite.student.dto;

public record StudentSubjectAchievementDto(
        String subjectName,
        Integer achievementLevel,
        Integer markPercentage,
        String source,
        boolean verified,
        String documentId,
        String updatedAt,
        String curriculum,
        String gradingScale,
        String academicYear,
        String examination
) {
    public StudentSubjectAchievementDto(String subjectName, Integer achievementLevel, Integer markPercentage, String source, boolean verified, String documentId, String updatedAt) {
        this(subjectName, achievementLevel, markPercentage, source, verified, documentId, updatedAt, null, null, null, null);
    }
    public StudentSubjectAchievementDto(String subjectName, Integer achievementLevel) {
        this(subjectName, achievementLevel, null, "MANUAL", false, null, null);
    }
}

