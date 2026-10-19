package com.edurite.ai.university;

import java.util.List;
import java.util.Set;

public record UniversitySourcePageResult(
        String sourceUrl,
        String pageTitle,
        UniversityPageType pageType,
        String cleanedText,
        Set<String> extractedKeywords,
        List<String> headings,
        boolean success,
        String failureReason,
        UniversityCrawlFailureType failureType,
        java.util.Map<String, String> officialLinks
) {

    public UniversitySourcePageResult(String sourceUrl, String pageTitle, UniversityPageType pageType,
            String cleanedText, Set<String> keywords, List<String> headings, boolean success,
            String reason, UniversityCrawlFailureType failureType) {
        this(sourceUrl, pageTitle, pageType, cleanedText, keywords, headings, success, reason, failureType, java.util.Map.of());
    }

    public UniversitySourcePageResult(
            String sourceUrl,
            String pageTitle,
            UniversityPageType pageType,
            String cleanedText,
            Set<String> extractedKeywords,
            boolean success,
            String failureReason,
            UniversityCrawlFailureType failureType
    ) {
        this(sourceUrl, pageTitle, pageType, cleanedText, extractedKeywords, List.of(), success, failureReason, failureType);
    }
}

