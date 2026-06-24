package com.edurite.opportunity.dto;

public record UnifiedOpportunityResponse(
        String id,
        String title,
        OpportunityType type,
        String field,
        String industry,
        String qualification,
        String location,
        String demand,
        boolean saved,
        boolean recommended
) {
}

