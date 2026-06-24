package com.edurite.ai.dto;

import com.edurite.bursary.dto.BursaryResultDto;
import java.util.List;
import java.util.Map;

public record AiDashboardSummaryResponse(
        Map<String, Object> dashboard,
        List<String> dashboardInsights,
        List<BursaryResultDto> bursarySuggestions,
        List<CareerAdviceResponse.RecommendedCareer> recommendedCareers
) {
}

