package com.edurite.progress.dto;

import java.util.List;

public final class ProgressScoreDtos {
    private ProgressScoreDtos() {
    }

    public record ProgressScoreCard(
            String key,
            String label,
            int percentage,
            String color,
            String recommendation
    ) {
    }

    public record ProgressScoreResponse(
            int overallPercentage,
            String overallColor,
            List<ProgressScoreCard> cards,
            List<String> recommendations
    ) {
    }
}

