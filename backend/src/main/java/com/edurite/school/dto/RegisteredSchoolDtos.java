package com.edurite.school.dto;

import java.util.List;
import java.util.UUID;

public final class RegisteredSchoolDtos {
    private RegisteredSchoolDtos() {
    }

    public record RegisteredSchoolDto(
            UUID id,
            String schoolName,
            String emisNumber,
            String schoolCode,
            UUID provinceId,
            UUID districtId,
            String province,
            String district,
            String circuit,
            String schoolType,
            String physicalAddress,
            String principalName,
            String schoolEmail,
            String contactNumber,
            String source,
            String registeredStatus
    ) {}

    public record RegisteredSchoolSearchResponse(
            List<RegisteredSchoolDto> items
    ) {}
}
