package com.edurite.ai.university;

public record UniversityCrawlSummary(
        int universitiesProcessed,
        int seedUrlsProcessed,
        int pagesDiscovered,
        int pagesSaved,
        int failures,
        long durationMs
) {
}

