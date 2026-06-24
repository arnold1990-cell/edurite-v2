package com.edurite.bursary.dto;

public record BursarySearchRequest(
        String query,
        String qualificationLevel,
        String region,
        String eligibility,
        int page,
        int size
) {}

