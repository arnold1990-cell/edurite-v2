package com.edurite.learning.service;

import com.edurite.learning.dto.LearningResourceDto;
import com.edurite.learning.entity.LearningCategory;
import com.edurite.learning.entity.LearningOutcomeMapping;
import com.edurite.learning.entity.LearningResource;
import com.edurite.learning.repository.LearningCategoryRepository;
import com.edurite.learning.repository.LearningOutcomeMappingRepository;
import com.edurite.learning.repository.LearningResourceRepository;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningCentreService {

    private static final Logger log = LoggerFactory.getLogger(LearningCentreService.class);
    private static final Duration EXTERNAL_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final LearningCategoryRepository learningCategoryRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final LearningOutcomeMappingRepository learningOutcomeMappingRepository;
    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final PsychometricService psychometricService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String youtubeApiKey;
    private final Map<String, CacheEntry> responseCache = new ConcurrentHashMap<>();

    public LearningCentreService(
            LearningCategoryRepository learningCategoryRepository,
            LearningResourceRepository learningResourceRepository,
            LearningOutcomeMappingRepository learningOutcomeMappingRepository,
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository,
            PsychometricService psychometricService,
            ObjectMapper objectMapper,
            @Value("${YOUTUBE_API_KEY:}") String youtubeApiKey
    ) {
        this.learningCategoryRepository = learningCategoryRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.learningOutcomeMappingRepository = learningOutcomeMappingRepository;
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
        this.psychometricService = psychometricService;
        this.objectMapper = objectMapper;
        this.youtubeApiKey = youtubeApiKey == null ? "" : youtubeApiKey.trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(EXTERNAL_TIMEOUT).build();
    }

    @Transactional(readOnly = true)
    public List<LearningResourceDto> listCatalogue() {
        return toDtoList(
                learningResourceRepository.findByActiveTrueOrderByCreatedAtDesc(),
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public List<LearningResourceDto> recommendedForStudent(Principal principal, List<String> requestedOutcomes) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
        List<String> outcomeKeys = new ArrayList<>();
        if (requestedOutcomes != null && !requestedOutcomes.isEmpty()) {
            requestedOutcomes.stream().filter(item -> item != null && !item.isBlank()).map(String::trim).forEach(outcomeKeys::add);
        } else if (profile != null) {
            outcomeKeys.addAll(psychometricService.findGrowthAreasByStudentProfileId(profile.getId()));
        }

        if (outcomeKeys.isEmpty()) {
            return listCatalogue();
        }

        List<LearningOutcomeMapping> mappings = learningOutcomeMappingRepository.findByOutcomeKeyInOrderByPriorityAsc(outcomeKeys);
        LinkedHashSet<UUID> resourceIds = mappings.stream().map(LearningOutcomeMapping::getResourceId).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (resourceIds.isEmpty()) {
            return List.of();
        }
        List<LearningResource> resources = learningResourceRepository.findByIdInAndActiveTrue(resourceIds);
        return toDtoList(resources, mappings);
    }

    public List<LearningResourceDto> searchOpenLibrary(String query) {
        String cleanQuery = normalizeQuery(query, "study materials");
        String encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);
        String url = "https://openlibrary.org/search.json?q=" + encoded;
        return fetchCached("openlibrary:" + cleanQuery, () -> parseOpenLibrary(fetchJson(url)));
    }

    public List<LearningResourceDto> searchGoogleBooks(String query) {
        String cleanQuery = normalizeQuery(query, "textbooks");
        String encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + encoded;
        return fetchCached("googlebooks:" + cleanQuery, () -> parseGoogleBooks(fetchJson(url)));
    }

    public List<LearningResourceDto> fetchQuizzes(int amount) {
        int safeAmount = Math.clamp(amount, 1, 25);
        String url = "https://opentdb.com/api.php?amount=" + safeAmount + "&type=multiple";
        return fetchCached("opentdb:" + safeAmount, () -> parseTrivia(fetchJson(url)));
    }

    public List<LearningResourceDto> searchYouTubeVideos(String query) {
        if (youtubeApiKey.isBlank()) {
            throw new LearningIntegrationException("Video lessons are temporarily unavailable.");
        }
        String cleanQuery = normalizeQuery(query, "coding tutorials");
        String encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);
        String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=12&q=" + encoded + "&key=" + youtubeApiKey;
        return fetchCached("youtube:" + cleanQuery, () -> parseYouTube(fetchJson(url)));
    }

    private List<LearningResourceDto> fetchCached(String key, CacheSupplier supplier) {
        CacheEntry cached = responseCache.get(key);
        if (cached != null && !cached.expired()) {
            return cached.value();
        }
        List<LearningResourceDto> data = supplier.get();
        responseCache.put(key, new CacheEntry(data, Instant.now().plus(CACHE_TTL)));
        return data;
    }

    private JsonNode fetchJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(EXTERNAL_TIMEOUT)
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Learning API request failed: status={}, url={}", response.statusCode(), url);
                throw new LearningIntegrationException("Learning resources are temporarily unavailable.");
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Learning API request exception: url={}, message={}", url, ex.getMessage(), ex);
            throw new LearningIntegrationException("Learning resources are temporarily unavailable.");
        } catch (IOException ex) {
            log.error("Learning API request exception: url={}, message={}", url, ex.getMessage(), ex);
            throw new LearningIntegrationException("Learning resources are temporarily unavailable.");
        } catch (RuntimeException ex) {
            log.error("Learning API parse exception: url={}, message={}", url, ex.getMessage(), ex);
            throw new LearningIntegrationException("Learning resources are temporarily unavailable.");
        }
    }

    private List<LearningResourceDto> parseOpenLibrary(JsonNode root) {
        List<LearningResourceDto> out = new ArrayList<>();
        for (JsonNode doc : root.path("docs")) {
            if (out.size() >= 12) break;
            String key = doc.path("key").asText("");
            String title = doc.path("title").asText("");
            if (title.isBlank()) continue;
            String author = doc.path("author_name").isArray() && !doc.path("author_name").isEmpty()
                    ? doc.path("author_name").get(0).asText("Open Library")
                    : "Open Library";
            String year = doc.path("first_publish_year").asText("");
            String coverId = doc.path("cover_i").asText("");
            String thumb = coverId.isBlank() ? null : "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
            String external = key.isBlank() ? "https://openlibrary.org" : "https://openlibrary.org" + key;
            out.add(new LearningResourceDto(
                    key.isBlank() ? "openlib-" + out.size() : key,
                    title,
                    year.isBlank() ? "Book and study resource from Open Library." : "Published in " + year + ".",
                    "Open Library",
                    "Books",
                    "Study Materials",
                    "All Grades",
                    "Intermediate",
                    "Book",
                    "45m",
                    thumb,
                    external,
                    0,
                    author,
                    List.of("Read overview", "Review key topics", "Take notes")
            ));
        }
        return out;
    }

    private List<LearningResourceDto> parseGoogleBooks(JsonNode root) {
        List<LearningResourceDto> out = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            if (out.size() >= 12) break;
            JsonNode info = item.path("volumeInfo");
            String id = item.path("id").asText("");
            String title = info.path("title").asText("");
            if (title.isBlank()) continue;
            String desc = info.path("description").asText("Textbook and study guide.");
            String instructor = info.path("authors").isArray() && !info.path("authors").isEmpty()
                    ? info.path("authors").get(0).asText("Google Books")
                    : "Google Books";
            String thumb = info.path("imageLinks").path("thumbnail").asText(null);
            String external = info.path("infoLink").asText("https://books.google.com");
            out.add(new LearningResourceDto(
                    id.isBlank() ? "gbook-" + out.size() : id,
                    title,
                    desc,
                    "Google Books",
                    "Study Materials",
                    "Textbooks",
                    "All Grades",
                    "Intermediate",
                    "Study Guide",
                    "50m",
                    thumb,
                    external,
                    0,
                    instructor,
                    List.of("Read chapter summary", "Highlight formulas", "Practice questions")
            ));
        }
        return out;
    }

    private List<LearningResourceDto> parseTrivia(JsonNode root) {
        List<LearningResourceDto> out = new ArrayList<>();
        for (JsonNode item : root.path("results")) {
            if (out.size() >= 20) break;
            String id = "quiz-" + out.size();
            String question = decodeHtml(item.path("question").asText("Quiz practice question"));
            String category = decodeHtml(item.path("category").asText("Quizzes"));
            String difficulty = decodeHtml(item.path("difficulty").asText("medium"));
            out.add(new LearningResourceDto(
                    id,
                    question,
                    "Multiple-choice revision practice.",
                    "Open Trivia DB",
                    "Quizzes",
                    category,
                    "All Grades",
                    toLevel(difficulty),
                    "Quiz",
                    "10m",
                    null,
                    "https://opentdb.com/",
                    0,
                    "Open Trivia DB",
                    List.of("Attempt question", "Check answer", "Review explanation")
            ));
        }
        return out;
    }

    private List<LearningResourceDto> parseYouTube(JsonNode root) {
        List<LearningResourceDto> out = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            if (out.size() >= 12) break;
            String videoId = item.path("id").path("videoId").asText("");
            if (videoId.isBlank()) continue;
            JsonNode snippet = item.path("snippet");
            String title = snippet.path("title").asText("Video lesson");
            String description = snippet.path("description").asText("Video lesson from YouTube.");
            String channel = snippet.path("channelTitle").asText("YouTube");
            String thumb = snippet.path("thumbnails").path("medium").path("url").asText(null);
            out.add(new LearningResourceDto(
                    "yt-" + videoId,
                    title,
                    description,
                    "YouTube",
                    "Video Lessons",
                    "Coding Tutorials",
                    "All Grades",
                    "Intermediate",
                    "Video Tutorial",
                    "20m",
                    thumb,
                    "https://www.youtube.com/watch?v=" + videoId,
                    0,
                    channel,
                    List.of("Watch lesson", "Summarize key points", "Apply in practice")
            ));
        }
        return out;
    }

    private List<LearningResourceDto> toDtoList(List<LearningResource> resources, List<LearningOutcomeMapping> mappings) {
        Map<UUID, String> categories = learningCategoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .collect(java.util.stream.Collectors.toMap(LearningCategory::getId, LearningCategory::getName));
        Map<UUID, List<String>> mappedOutcomes = new LinkedHashMap<>();
        for (LearningOutcomeMapping mapping : mappings) {
            mappedOutcomes.computeIfAbsent(mapping.getResourceId(), ignored -> new ArrayList<>()).add(mapping.getOutcomeKey());
        }
        return resources.stream()
                .map(resource -> {
                    List<String> outcomes = mappedOutcomes.getOrDefault(resource.getId(), List.of());
                    String category = categories.getOrDefault(resource.getCategoryId(), "Study Materials");
                    return new LearningResourceDto(
                            resource.getId().toString(),
                            resource.getTitle(),
                            resource.getSummary(),
                            "EduRite",
                            category,
                            category,
                            "All Grades",
                            toLevel(resource.getDifficulty()),
                            normalizeResourceType(resource.getResourceType()),
                            (resource.getEstimatedMinutes() == null ? 60 : resource.getEstimatedMinutes()) + "m",
                            null,
                            resource.getUrl(),
                            0,
                            "EduRite Learning Team",
                            outcomes.isEmpty() ? List.of("Overview", "Practice", "Recap") : outcomes
                    );
                })
                .toList();
    }

    private String normalizeQuery(String query, String fallback) {
        if (query == null || query.isBlank()) return fallback;
        return query.trim();
    }

    private String normalizeResourceType(String type) {
        if (type == null || type.isBlank()) return "Study Guide";
        return switch (type.trim().toLowerCase()) {
            case "video", "video tutorial", "tutorial" -> "Video Tutorial";
            case "past paper", "paper" -> "Past Paper";
            case "worksheet" -> "Worksheet";
            case "pdf", "notes" -> "PDF Notes";
            default -> "Course";
        };
    }

    private String toLevel(String value) {
        if (value == null) return "Intermediate";
        String clean = value.trim().toLowerCase();
        if (clean.contains("hard") || clean.contains("advanced")) return "Advanced";
        if (clean.contains("easy") || clean.contains("beginner")) return "Beginner";
        return "Intermediate";
    }

    private String decodeHtml(String value) {
        return value
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private record CacheEntry(List<LearningResourceDto> value, Instant expiresAt) {
        private boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @FunctionalInterface
    private interface CacheSupplier {
        List<LearningResourceDto> get();
    }

    public static class LearningIntegrationException extends RuntimeException {
        public LearningIntegrationException(String message) {
            super(message);
        }
    }
}

