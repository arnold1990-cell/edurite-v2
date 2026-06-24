package com.edurite.bursary.service;

import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchRequest;
import com.edurite.bursary.dto.BursarySearchResponse;
import com.edurite.bursary.source.ProviderBursarySource;
import com.edurite.bursary.source.WebFallbackBursarySource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BursaryAggregationService {

    private final ProviderBursarySource providerBursarySource;
    private final WebFallbackBursarySource webFallbackBursarySource;

    public BursaryAggregationService(ProviderBursarySource providerBursarySource, WebFallbackBursarySource webFallbackBursarySource) {
        this.providerBursarySource = providerBursarySource;
        this.webFallbackBursarySource = webFallbackBursarySource;
    }

    public BursarySearchResponse search(BursarySearchRequest request) {
        List<BursaryResultDto> providerItems = providerBursarySource.fetch(request);
        List<BursaryResultDto> merged = new ArrayList<>(providerItems);

        if (providerItems.size() < request.size()) {
            merged.addAll(webFallbackBursarySource.fetch(request));
        }

        Map<String, BursaryResultDto> deduped = new LinkedHashMap<>();
        for (BursaryResultDto item : merged) {
            deduped.merge(dedupeKey(item), item, this::preferGroundedRecord);
        }

        List<BursaryResultDto> ordered = deduped.values().stream()
                .sorted(Comparator.comparing(BursaryResultDto::officialSource).reversed()
                        .thenComparing(BursaryResultDto::incomplete)
                        .thenComparing(Comparator.comparingInt(BursaryResultDto::relevanceScore).reversed()))
                .toList();

        int from = Math.min(request.page() * request.size(), ordered.size());
        int to = Math.min(from + request.size(), ordered.size());
        return new BursarySearchResponse(ordered.subList(from, to), request.page(), request.size(), ordered.size());
    }

    private BursaryResultDto preferGroundedRecord(BursaryResultDto left, BursaryResultDto right) {
        if (left.officialSource() != right.officialSource()) {
            return left.officialSource() ? left : right;
        }
        if (left.incomplete() != right.incomplete()) {
            return left.incomplete() ? right : left;
        }
        return left.relevanceScore() >= right.relevanceScore() ? left : right;
    }

    private String dedupeKey(BursaryResultDto item) {
        return normalize(item.title()) + "|" + normalize(item.provider()) + "|" + normalize(item.applicationLink());
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}

