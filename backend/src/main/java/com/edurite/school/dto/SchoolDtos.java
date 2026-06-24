package com.edurite.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class SchoolDtos {
    private SchoolDtos() {
    }

    public record SchoolProfileRequest(
            @NotBlank @Size(max = 255) String schoolName,
            @Size(max = 120) String country,
            @Size(max = 120) String city,
            @Size(max = 180) String contactPerson,
            @Email @Size(max = 255) String contactEmail,
            @Size(max = 5000) String notes
    ) {
    }

    public record SchoolProfileResponse(
            UUID id,
            String schoolName,
            String country,
            String city,
            String contactPerson,
            String contactEmail,
            String notes
    ) {
    }

    public record LinkStudentRequest(@NotNull UUID studentId) {
    }

    public record SchoolStudentResponse(UUID studentId, String name, String qualificationLevel, int profileCompleteness) {
    }

    public record SchoolSummaryResponse(
            UUID schoolId,
            long linkedStudents,
            long psychometricCompleted,
            long completeProfiles,
            long tutorSessions,
            long trackedApplications,
            List<SchoolStudentResponse> students
    ) {
    }
}

