package com.edurite.student.dto;

import java.time.LocalDate;
import java.util.List;

public record StudentSavedProfilePayload(
        String firstName,
        String lastName,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String location,
        String bio,
        String qualificationLevel,
        String selectedGrade,
        List<StudentSubjectAchievementDto> subjectAchievements,
        List<String> qualifications,
        List<String> experience,
        List<String> skills,
        List<String> interests,
        String careerGoals
) {
}

