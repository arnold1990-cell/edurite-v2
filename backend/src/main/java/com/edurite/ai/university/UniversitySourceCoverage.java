package com.edurite.ai.university;

import java.time.OffsetDateTime;

public record UniversitySourceCoverage(
        int configuredUniversityCount,
        int activeUniversityCount,
        long storedPageCount,
        long successfulCrawlCount,
        long failedCrawlCount,
        OffsetDateTime lastCrawlTime
) {
}

