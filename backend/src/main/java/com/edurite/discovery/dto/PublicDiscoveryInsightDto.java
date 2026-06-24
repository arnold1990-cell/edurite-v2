package com.edurite.discovery.dto;

import java.util.List;

public record PublicDiscoveryInsightDto(
        String summary,
        boolean aiUsed,
        List<String> highlights,
        int resultCount
) {
}

