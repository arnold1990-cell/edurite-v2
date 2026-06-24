package com.edurite.company.dto;

import jakarta.validation.constraints.Size;

public record CompanyProfileUpdateRequest(
        @Size(max = 120) String industry,
        @Size(max = 30) String mobileNumber,
        @Size(max = 150) String contactPersonName,
        @Size(max = 255) String address,
        @Size(max = 255) String website,
        String description
) {
}

