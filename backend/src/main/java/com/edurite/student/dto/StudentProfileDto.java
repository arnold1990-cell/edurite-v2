package com.edurite.student.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentProfileDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
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
        String cvFileUrl,
        String transcriptFileUrl,
        boolean profileCompleted,
        int profileCompleteness
) {}

