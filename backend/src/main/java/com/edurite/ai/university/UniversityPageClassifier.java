package com.edurite.ai.university;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UniversityPageClassifier {

    private static final List<String> PROGRAMME_DETAIL_HINTS = List.of(
            "programme overview", "program overview", "curriculum", "module list", "entry requirements",
            "career opportunities", "duration", "nqf level"
    );

    private static final List<String> FILTERED_PROGRAMME_LIST_HINTS = List.of(
            "filter programmes", "search programmes", "browse programmes", "programme finder", "program finder"
    );

    private static final List<String> QUALIFICATION_LIST_HINTS = List.of(
            "all qualifications", "undergraduate qualifications", "qualifications",
            "list of qualifications", "available qualifications"
    );

    private static final List<String> FEES_FUNDING_HINTS = List.of(
            "tuition fees", "financial aid", "funding", "bursary", "cost of study", "study fees"
    );

    private static final List<String> ADMISSIONS_HINTS = List.of(
            "apply for admission", "admission requirements", "how to apply", "admissions", "application dates"
    );


    private static final List<String> DEPRIORITIZED_HINTS = List.of(
            "news", "event", "staff", "directory", "privacy", "cookie", "legal", "terms", "vacancy"
    );

    private static final List<String> HIGH_VALUE_HINTS = List.of(
            "programme", "program", "course", "study", "faculty", "admission", "entry requirement", "bursary", "financial aid"
    );
    private static final List<String> ACADEMIC_KEYWORDS = List.of(
            "computer science", "information systems", "accounting", "economics", "engineering", "software",
            "technology", "mathematics", "faculty", "college", "campus", "diploma", "degree", "bachelor"
    );

    public UniversityPageType classify(String title, String text) {
        String content = normalize(title) + "\n" + normalize(text);
        // We score every page type and pick the strongest one.
        // This keeps specific pages ahead of broad admissions wording.
        int programmeDetailScore = score(content, PROGRAMME_DETAIL_HINTS, 1);
        int filteredProgrammeScore = score(content, FILTERED_PROGRAMME_LIST_HINTS, 1);
        int qualificationScore = score(content, QUALIFICATION_LIST_HINTS, 1)
                + score(content, List.of("all qualifications", "list of qualifications", "available qualifications"), 1);
        int feesFundingScore = score(content, FEES_FUNDING_HINTS, 1);
        int admissionsScore = score(content, ADMISSIONS_HINTS, 1);

        if (programmeDetailScore > 0) {
            return UniversityPageType.PROGRAMME_DETAIL;
        }
        if (filteredProgrammeScore > 0) {
            return UniversityPageType.FILTERED_PROGRAMME_LIST;
        }
        if (qualificationScore > 0 && qualificationScore >= admissionsScore) {
            return UniversityPageType.QUALIFICATION_LIST;
        }
        if (feesFundingScore > 0) {
            return UniversityPageType.FEES_FUNDING;
        }
        if (admissionsScore > 0) {
            return UniversityPageType.ADMISSIONS_OVERVIEW;
        }
        return UniversityPageType.UNKNOWN;
    }



    public UniversityPageType classify(String url, String title, String text) {
        return classify(title + "\n" + safe(url), text);
    }

    public boolean shouldSkipPage(String url, String title, String text) {
        String content = normalize(url) + " " + normalize(title) + " " + normalize(text);
        boolean hasHighValue = containsAny(content, HIGH_VALUE_HINTS);
        boolean looksDeprioritized = containsAny(content, DEPRIORITIZED_HINTS);
        return looksDeprioritized && !hasHighValue;
    }

    public boolean shouldDeprioritizeLink(String url, String anchorText) {
        String content = normalize(url) + " " + normalize(anchorText);
        boolean hasHighValue = containsAny(content, HIGH_VALUE_HINTS);
        return !hasHighValue && containsAny(content, DEPRIORITIZED_HINTS);
    }

    public Set<String> extractKeywords(String title, String text) {
        String content = normalize(title) + "\n" + normalize(text);
        Set<String> extracted = new LinkedHashSet<>();
        for (String keyword : ACADEMIC_KEYWORDS) {
            if (content.contains(keyword)) {
                extracted.add(keyword);
            }
        }
        return extracted;
    }

    private boolean containsAny(String content, List<String> terms) {
        return terms.stream().anyMatch(content::contains);
    }

    private int score(String content, List<String> terms, int perMatch) {
        int result = 0;
        for (String term : terms) {
            if (content.contains(term)) {
                result += perMatch;
            }
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

