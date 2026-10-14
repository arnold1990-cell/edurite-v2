package com.edurite.student.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record StudentProfileUpsertRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
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
        String careerGoals,
        @Size(max = 200) String schoolName,
        List<StudentSubjectAchievementDto> expectedSubjectAchievements
) {
    public StudentProfileUpsertRequest(String firstName, String lastName, String phone, LocalDate dateOfBirth,
            String gender, String location, String bio, String qualificationLevel, String selectedGrade,
            List<StudentSubjectAchievementDto> subjectAchievements, List<String> qualifications,
            List<String> experience, List<String> skills, List<String> interests, String careerGoals, String schoolName) {
        this(firstName, lastName, phone, dateOfBirth, gender, location, bio, qualificationLevel, selectedGrade,
                subjectAchievements, qualifications, experience, skills, interests, careerGoals, schoolName, null);
    }
}

