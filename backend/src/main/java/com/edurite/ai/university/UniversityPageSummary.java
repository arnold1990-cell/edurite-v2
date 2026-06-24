package com.edurite.ai.university;

import java.util.Set;

public record UniversityPageSummary(
        String sourceUrl,
        String universityName,
        String pageTitle,
        String pageType,
        String qualificationLevel,
        Set<String> keywords,
        String summaryExcerpt,
        int relevanceScore
) {
}

