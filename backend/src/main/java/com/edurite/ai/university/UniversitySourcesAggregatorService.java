package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourcesAggregatorService {

    private static final int MAX_COMBINED_CONTEXT_CHARS = 20_000;

    public String buildCombinedContext(List<UniversitySourcePageResult> pages, StudentProfile profile,
                                      UniversitySourcesAnalysisRequest request) {
        List<UniversitySourcePageResult> successful = pages.stream()
                .filter(UniversitySourcePageResult::success)
                .sorted(Comparator.comparingInt(p -> -relevanceScore(p, profile, request)))
                .toList();

        StringBuilder builder = new StringBuilder();
        for (UniversitySourcePageResult page : successful) {
            String block = "Source URL: " + page.sourceUrl() + "\n"
                    + "Title: " + page.pageTitle() + "\n"
                    + "Type: " + page.pageType() + "\n"
                    + "Keywords: " + String.join(", ", page.extractedKeywords()) + "\n"
                    + page.cleanedText();
            if (builder.length() + block.length() + 2 > MAX_COMBINED_CONTEXT_CHARS) {
                break;
            }
            builder.append(block).append("\n\n");
        }
        return builder.toString().trim();
    }

    private int relevanceScore(UniversitySourcePageResult page, StudentProfile profile,
                               UniversitySourcesAnalysisRequest request) {
        int score = 0;
        String haystack = (page.pageTitle() + " " + page.cleanedText() + " " + String.join(" ", page.extractedKeywords()))
                .toLowerCase(Locale.ROOT);
        for (String token : buildTokens(profile, request)) {
            if (!token.isBlank() && haystack.contains(token)) {
                score += 3;
            }
        }
        if (page.pageType() == UniversityPageType.PROGRAMME_DETAIL) {
            score += 4;
        }
        if (page.pageType() == UniversityPageType.QUALIFICATION_LIST) {
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
}

