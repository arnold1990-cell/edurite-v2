package com.edurite.company.dto;

import java.util.List;
import java.util.UUID;

public record CompanyStudentSearchResultDto(
        UUID studentId,
        String firstName,
        String lastName,
        String location,
        String qualificationLevel,
        List<String> skills,
        List<String> interests,
        int matchScore,
        boolean bookmarked,
        boolean shortlisted
) {
}

