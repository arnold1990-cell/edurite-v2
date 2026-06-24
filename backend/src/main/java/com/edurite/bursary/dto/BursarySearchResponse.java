package com.edurite.bursary.dto;

import java.util.List;

public record BursarySearchResponse(
        List<BursaryResultDto> items,
        int page,
        int size,
        long total
) {}

