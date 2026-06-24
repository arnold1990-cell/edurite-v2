package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class UniversityPageRetrievalService {

    private final CrawledUniversityPageRepository repository;

    public UniversityPageRetrievalService(CrawledUniversityPageRepository repository) {
        this.repository = repository;
    }

    public List<UniversityPageSummary> retrieveTopRelevantPages(StudentProfile profile,
                                                                UniversitySourcesAnalysisRequest request,
                                                                int limit) {
        return repository.findByActiveTrueAndCrawlStatus(CrawlStatus.SUCCESS).stream()
                .map(page -> toSummary(page, relevanceScore(page, profile, request)))
                .sorted(Comparator.comparingInt(UniversityPageSummary::relevanceScore).reversed()
                        .thenComparing(UniversityPageSummary::universityName))
                .limit(limit)
                .toList();
    }

    private UniversityPageSummary toSummary(CrawledUniversityPage page, int relevanceScore) {
        return new UniversityPageSummary(
                page.getSourceUrl(),
                page.getUniversityName(),
                page.getPageTitle(),
                page.getPageType(),
                page.getQualificationLevel(),
                page.getExtractedKeywords(),
                page.getSummaryExcerpt(),
                relevanceScore
        );
    }

    private int relevanceScore(CrawledUniversityPage page, StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        String haystack = (safe(page.getPageTitle()) + " " + safe(page.getCleanedContent()) + " "
                + String.join(" ", page.getExtractedKeywords())).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : buildTokens(profile, request)) {
            if (haystack.contains(token)) {
                score += 3;
            }
        }
        if ("PROGRAMME_DETAIL".equalsIgnoreCase(page.getPageType())) {
            score += 4;
        }
        if ("QUALIFICATION_LIST".equalsIgnoreCase(page.getPageType())) {
            score += 2;
        }
        if (safe(request.qualificationLevel()).equalsIgnoreCase(safe(page.getQualificationLevel()))) {
            score += 3;
        }
        return score;
    }

    private List<String> buildTokens(StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        List<String> tokens = new ArrayList<>();
        addSplit(tokens, request.targetProgram());
        addSplit(tokens, request.careerInterest());
        addSplit(tokens, request.qualificationLevel());
        addSplit(tokens, profile.getInterests());
        addSplit(tokens, profile.getSkills());
        addSplit(tokens, profile.getQualificationLevel());
        return tokens;
    }

    private void addSplit(List<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String token : value.toLowerCase(Locale.ROOT).split("[,\\s/]+")) {
            if (token.length() >= 3) {
                target.add(token);
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

