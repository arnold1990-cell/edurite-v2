package com.edurite.jobs.service;

import com.edurite.jobs.dto.JobOpportunityDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdzunaJobService {

    private static final Logger log = LoggerFactory.getLogger(AdzunaJobService.class);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int MAX_DESCRIPTION_LENGTH = 220;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String appId;
    private final String appKey;
    private final String country;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public AdzunaJobService(
            ObjectMapper objectMapper,
            @Value("${adzuna.app-id:}") String appId,
            @Value("${adzuna.app-key:}") String appKey,
            @Value("${adzuna.country:za}") String country
    ) {
        this.objectMapper = objectMapper;
        this.appId = appId == null ? "" : appId.trim();
        this.appKey = appKey == null ? "" : appKey.trim();
        this.country = country == null || country.isBlank() ? "za" : country.trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
    }

    public List<JobOpportunityDto> searchJobs(String query, String location, String category, String experience) {
        if (appId.isBlank() || appKey.isBlank()) {
            throw new JobSearchException("Job opportunities are temporarily unavailable. Please try again.");
        }

        String safeQuery = normalize(query, "software developer");
        String safeLocation = normalize(location, "Cape Town");
        String safeCategory = normalize(category, "");
        String safeExperience = normalize(experience, "");
        String cacheKey = String.join("|", safeQuery, safeLocation, safeCategory, safeExperience);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.expired()) {
            return cached.data();
        }

        String url = buildUrl(safeQuery, safeLocation);
        JsonNode root = fetchJson(url);
        List<JobOpportunityDto> mapped = mapResults(root, safeCategory, safeExperience);
        cache.put(cacheKey, new CacheEntry(mapped, Instant.now().plus(CACHE_TTL)));
        return mapped;
    }

    private String buildUrl(String query, String location) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        return "https://api.adzuna.com/v1/api/jobs/" + country + "/search/1"
                + "?app_id=" + URLEncoder.encode(appId, StandardCharsets.UTF_8)
                + "&app_key=" + URLEncoder.encode(appKey, StandardCharsets.UTF_8)
                + "&what=" + encodedQuery
                + "&where=" + encodedLocation;
    }

    private JsonNode fetchJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(HTTP_TIMEOUT)
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Adzuna search failed with status={} for url={}", response.statusCode(), url);
                throw new JobSearchException("Job opportunities are temporarily unavailable. Please try again.");
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Adzuna request interrupted: {}", ex.getMessage(), ex);
            throw new JobSearchException("Job opportunities are temporarily unavailable. Please try again.");
        } catch (IOException | RuntimeException ex) {
            log.error("Adzuna request failed: {}", ex.getMessage(), ex);
            throw new JobSearchException("Job opportunities are temporarily unavailable. Please try again.");
        }
    }

    private List<JobOpportunityDto> mapResults(JsonNode root, String categoryFilter, String experienceFilter) {
        List<JobOpportunityDto> out = new ArrayList<>();
        for (JsonNode result : root.path("results")) {
            String title = result.path("title").asText("");
            if (title.isBlank()) continue;
            String category = result.path("category").path("label").asText("");
            String description = result.path("description").asText("");
            if (!categoryFilter.isBlank() && !category.toLowerCase().contains(categoryFilter.toLowerCase())) {
                continue;
            }
            if (!experienceFilter.isBlank()) {
                String haystack = (title + " " + description).toLowerCase();
                if (!haystack.contains(experienceFilter.toLowerCase())) continue;
            }
            out.add(new JobOpportunityDto(
                    result.path("id").asText("adzuna-" + out.size()),
                    title,
                    result.path("company").path("display_name").asText("Unknown company"),
                    result.path("location").path("display_name").asText("Unknown location"),
                    trimDescription(description),
                    nullableDouble(result.path("salary_min")),
                    nullableDouble(result.path("salary_max")),
                    result.path("contract_type").asText("N/A"),
                    category.isBlank() ? "General" : category,
                    result.path("redirect_url").asText(""),
                    result.path("created").asText(""),
                    "Adzuna"
            ));
            if (out.size() >= 20) break;
        }
        return out;
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }

    private String trimDescription(String value) {
        if (value == null) return "";
        if (value.length() <= MAX_DESCRIPTION_LENGTH) return value;
        return value.substring(0, MAX_DESCRIPTION_LENGTH) + "...";
    }

    private Double nullableDouble(JsonNode node) {
        return node != null && node.isNumber() ? node.asDouble() : null;
    }

    private record CacheEntry(List<JobOpportunityDto> data, Instant expiresAt) {
        private boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public static class JobSearchException extends RuntimeException {
        public JobSearchException(String message) {
            super(message);
        }
    }
}

