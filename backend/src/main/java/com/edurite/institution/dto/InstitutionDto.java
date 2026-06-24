package com.edurite.institution.dto;

import java.util.UUID;

public record InstitutionDto(
        UUID id,
        String name,
        String location,
        String city,
        String province,
        String country,
        String website,
        String logoUrl,
        String category,
        boolean featured,
        boolean active
) {
}

