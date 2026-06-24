package com.edurite.jobs.dto;

public record JobOpportunityDto(
        String id,
        String title,
        String company,
        String location,
        String description,
        Double salaryMin,
        Double salaryMax,
        String contractType,
        String category,
        String redirectUrl,
        String created,
        String source
) {
}


