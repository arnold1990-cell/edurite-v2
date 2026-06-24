package com.edurite.discovery.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchRequest;
import com.edurite.bursary.service.BursaryAggregationService;
import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import com.edurite.course.entity.Course;
import com.edurite.course.repository.CourseRepository;
import com.edurite.discovery.dto.PublicDiscoveryInsightDto;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PublicDiscoveryInsightService {

    private final CareerRepository careerRepository;
    private final CourseRepository courseRepository;
    private final BursaryAggregationService bursaryAggregationService;
    private final AiProviderOrchestratorService aiProviderOrchestratorService;

    public PublicDiscoveryInsightService(
            CareerRepository careerRepository,
            CourseRepository courseRepository,
            BursaryAggregationService bursaryAggregationService,
            AiProviderOrchestratorService aiProviderOrchestratorService
    ) {
        this.careerRepository = careerRepository;
        this.courseRepository = courseRepository;
        this.bursaryAggregationService = bursaryAggregationService;
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
    }

    public PublicDiscoveryInsightDto careersInsight(
            String q,
            String industry,
            String qualificationLevel,
            String location,
            int top
    ) {
        int limit = clampTop(top);
        List<Career> careers = careerRepository.search(
                        safe(q),
                        safe(industry),
                        safe(qualificationLevel),
                        safe(location),
                        "",
                        "",
                        PageRequest.of(0, limit))
                .getContent();

        List<String> highlights = careers.stream()
                .map(Career::getTitle)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        String fallback = careersFallbackSummary(q, careers.size());
        return toInsight(promptForCareers(q, industry, qualificationLevel, location, highlights), fallback, highlights, careers.size());
    }

    public PublicDiscoveryInsightDto coursesInsight(
            String q,
            String level,
            String location,
            int top
    ) {
        int limit = clampTop(top);
        List<Course> courses = courseRepository.search(
                        safe(q),
                        safe(level),
                        safe(location),
                        PageRequest.of(0, limit))
                .getContent();

        List<String> highlights = courses.stream()
                .map(Course::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        String fallback = coursesFallbackSummary(q, courses.size());
        return toInsight(promptForCourses(q, level, location, highlights), fallback, highlights, courses.size());
    }

    public PublicDiscoveryInsightDto bursariesInsight(
            String q,
            String qualification,
            String region,
            String eligibility,
            int top
    ) {
        int limit = clampTop(top);
        List<BursaryResultDto> bursaries = bursaryAggregationService.search(new BursarySearchRequest(
                safe(q),
                safe(qualification),
                safe(region),
                safe(eligibility),
                0,
                limit
        )).items();

        List<String> highlights = bursaries.stream()
                .map(item -> {
                    String provider = item.provider() == null ? "" : item.provider().trim();
                    return provider.isBlank() ? item.title() : item.title() + " (" + provider + ")";
                })
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        String fallback = bursariesFallbackSummary(q, bursaries.size());
        return toInsight(promptForBursaries(q, qualification, region, eligibility, highlights), fallback, highlights, bursaries.size());
    }

    private PublicDiscoveryInsightDto toInsight(String prompt, String fallbackSummary, List<String> highlights, int resultCount) {
        try {
            String summary = aiProviderOrchestratorService.generateContent(prompt);
            return new PublicDiscoveryInsightDto(cleanSummary(summary, fallbackSummary), true, highlights, resultCount);
        } catch (RuntimeException ignored) {
            return new PublicDiscoveryInsightDto(fallbackSummary, false, highlights, resultCount);
        }
    }

    private String promptForCareers(
            String q,
            String industry,
            String qualificationLevel,
            String location,
            List<String> highlights
    ) {
        return """
                You are assisting with public career discovery on EduRite.
                Write a concise, practical summary in 1-2 sentences.
                Keep it professional and plain text.
                Mention market fit and how the user should refine filters next.

                Query: %s
                Industry: %s
                Qualification level: %s
                Location: %s
                Top results: %s
                """.formatted(safe(q), safe(industry), safe(qualificationLevel), safe(location), joinHighlights(highlights));
    }

    private String promptForCourses(String q, String level, String location, List<String> highlights) {
        return """
                You are assisting with public course discovery on EduRite.
                Write a concise, practical summary in 1-2 sentences.
                Keep it professional and plain text.
                Mention alignment between courses and career goals.

                Query: %s
                Level: %s
                Institution/location: %s
                Top results: %s
                """.formatted(safe(q), safe(level), safe(location), joinHighlights(highlights));
    }

    private String promptForBursaries(
            String q,
            String qualification,
            String region,
            String eligibility,
            List<String> highlights
    ) {
        return """
                You are assisting with public bursary discovery on EduRite.
                Write a concise, practical summary in 1-2 sentences.
                Keep it professional and plain text.
                Mention competition/readiness and which filters to improve.

                Query: %s
                Qualification: %s
                Region: %s
                Eligibility: %s
                Top results: %s
                """.formatted(safe(q), safe(qualification), safe(region), safe(eligibility), joinHighlights(highlights));
    }

    private String careersFallbackSummary(String query, int resultCount) {
        if (resultCount <= 0) {
            return "No live career matches were found. Broaden your search terms or remove one filter.";
        }
        if (safe(query).isBlank()) {
            return "Showing live career roles currently available on EduRite. Use industry and location filters to refine your shortlist.";
        }
        return "Showing live career roles that match your search. Refine location and industry filters to narrow to the best-fit paths.";
    }

    private String coursesFallbackSummary(String query, int resultCount) {
        if (resultCount <= 0) {
            return "No live course matches were found. Try a broader keyword or a different level.";
        }
        if (safe(query).isBlank()) {
            return "Showing live courses currently available on EduRite. Use level and institution/location filters to focus your options.";
        }
        return "Showing live course matches for your search. Narrow by level and institution/location to improve fit.";
    }

    private String bursariesFallbackSummary(String query, int resultCount) {
        if (resultCount <= 0) {
            return "No live bursary matches were found. Broaden your filters to discover more funding opportunities.";
        }
        if (safe(query).isBlank()) {
            return "Showing live bursary opportunities ranked by source quality and relevance.";
        }
        return "Showing live bursary matches for your criteria. Refine qualification, region, and eligibility to improve relevance.";
    }

    private int clampTop(int top) {
        return Math.min(8, Math.max(3, top));
    }

    private String joinHighlights(List<String> highlights) {
        if (highlights == null || highlights.isEmpty()) {
            return "none";
        }
        return highlights.stream().limit(5).collect(java.util.stream.Collectors.joining(", "));
    }

    private String cleanSummary(String summary, String fallbackSummary) {
        if (summary == null) {
            return fallbackSummary;
        }
        String cleaned = summary.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        if (cleaned.isBlank()) {
            return fallbackSummary;
        }
        return cleaned.length() <= 320 ? cleaned : cleaned.substring(0, 320).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

