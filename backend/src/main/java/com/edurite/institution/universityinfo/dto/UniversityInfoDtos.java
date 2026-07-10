package com.edurite.institution.universityinfo.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class UniversityInfoDtos {
    private UniversityInfoDtos() {
    }

    public record UniversityInfoInstitutionResponse(
            UUID id,
            String slug,
            String name,
            String officialWebsite,
            String officialProgrammesUrl,
            String officialAdmissionsUrl
    ) {
    }

    public record UniversityProgrammeResponse(
            UUID id,
            String name,
            String qualificationType,
            String faculty,
            String department,
            String duration,
            String studyMode,
            String campus,
            String programmeUrl,
            String sourceUrl,
            String sourceLabel,
            OffsetDateTime lastVerifiedAt,
            String retrievalStatus,
            boolean active
    ) {
    }

    public record UniversityAdmissionRequirementResponse(
            UUID id,
            String programmeName,
            String requirementTitle,
            Integer apsMinimum,
            List<String> requiredSubjects,
            List<String> minimumMarks,
            String nscRequirement,
            String languageRequirement,
            String facultySpecificRequirement,
            String internationalRequirement,
            String additionalTests,
            String sourceUrl,
            String sourceLabel,
            OffsetDateTime lastVerifiedAt,
            String retrievalStatus,
            boolean active
    ) {
    }

    public record UniversityRetrievalLogResponse(
            UUID id,
            String sourceUrl,
            String status,
            String message,
            String retrievalType,
            OffsetDateTime retrievedAt
    ) {
    }

    public record UniversityProgrammesViewResponse(
            UniversityInfoInstitutionResponse institution,
            String retrievalStatus,
            String message,
            OffsetDateTime lastUpdatedAt,
            boolean cachedData,
            boolean fallbackOnly,
            List<String> officialDomains,
            List<String> availableFaculties,
            List<String> availableQualificationTypes,
            List<String> availableStudyModes,
            List<UniversityProgrammeResponse> programmes,
            List<UniversityRetrievalLogResponse> retrievalLogs
    ) {
    }

    public record UniversityAdmissionRequirementsViewResponse(
            UniversityInfoInstitutionResponse institution,
            String retrievalStatus,
            String message,
            OffsetDateTime lastUpdatedAt,
            boolean cachedData,
            boolean fallbackOnly,
            List<String> officialDomains,
            List<UniversityAdmissionRequirementResponse> requirements,
            List<UniversityRetrievalLogResponse> retrievalLogs
    ) {
    }

    public record UniversityRefreshResponse(
            String slug,
            String status,
            String message,
            OffsetDateTime refreshedAt
    ) {
    }
}
