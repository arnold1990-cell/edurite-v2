package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UniversityGuidanceResultEnricher {

    public UniversitySourcesAnalysisResponse enrich(UniversitySourcesAnalysisResponse response,
                                                    UniversitySourcesAnalysisRequest request,
                                                    StudentProfile profile,
                                                    List<String> requestedUrls,
                                                    List<UniversitySourcePageResult> fetchedPages) {
        List<UniversitySourcePageResult> safePages = fetchedPages == null ? List.of() : fetchedPages;
        List<String> safeRequestedUrls = requestedUrls == null ? List.of() : requestedUrls;
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes = enrichProgrammes(response, request, profile, safePages);
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> careers = enrichCareers(response, request, profile, programmes, safePages);
        List<UniversitySourcesAnalysisResponse.SourceDiagnostic> sourceDiagnostics = buildDiagnostics(safeRequestedUrls, safePages);
        UniversitySourcesAnalysisResponse.SourceCoverage sourceCoverage = buildCoverage(safeRequestedUrls, safePages, programmes);
        String scoreReason = buildScoreReason(response, request, profile, programmes, safePages);
        List<String> scoreSignals = buildScoreSignals(request, profile, programmes, safePages);
        List<String> scoreLimitations = buildScoreLimitations(programmes, safePages);

        return new UniversitySourcesAnalysisResponse(
                response.aiLive(),
                response.fallbackUsed(),
                response.status(),
                response.mode(),
                response.groundingStatus(),
                response.evidenceCoverage(),
                response.warningMessage(),
                response.requestedSources(),
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
                response.summary(),
                response.inferredGuidance(),
                careers,
                programmes,
                response.bursarySuggestions(),
                dedupeStrings(response.recommendedUniversities()),
                dedupeStrings(response.minimumRequirements()),
                dedupeStrings(response.keyRequirements()),
                dedupeStrings(response.skillGaps()),
                dedupeStrings(response.recommendedNextSteps()),
                dedupeStrings(response.warnings()),
                response.suitabilityScore(),
                response.rawModelUsed(),
                scoreReason,
                scoreSignals,
                scoreLimitations,
                sourceDiagnostics,
                sourceCoverage,
                response.planCode(),
                response.premiumUnlocked(),
                response.careerSuggestionLimit(),
                response.careerSuggestionsLimited(),
                response.upgradeMessage(),
                response.available(),
                response.message()
        );
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedProgramme> enrichProgrammes(UniversitySourcesAnalysisResponse response,
                                                                                           UniversitySourcesAnalysisRequest request,
                                                                                           StudentProfile profile,
                                                                                           List<UniversitySourcePageResult> pages) {
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> base = response.recommendedProgrammes() == null ? List.of() : response.recommendedProgrammes();
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> result = new ArrayList<>();
        int index = 0;
        for (UniversitySourcesAnalysisResponse.RecommendedProgramme programme : base) {
            List<String> verifiedFacts = extractVerifiedFacts(programme.name(), programme.university(), pages);
            List<String> missingData = buildMissingProgrammeData(programme, verifiedFacts);
            List<String> nextBestActions = buildProgrammeActions(programme, missingData);
            List<String> inferredInsights = dedupeStrings(java.util.Arrays.asList(programme.notes(), programme.recommendationReason()));
            result.add(new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                    programme.name(),
                    programme.university(),
                    dedupeRequirements(programme.admissionRequirements()),
                    programme.notes(),
                    firstNonBlank(programme.recommendationReason(), buildProgrammeReason(programme, request, profile, verifiedFacts)),
                    firstNonBlank(programme.confidenceLevel(), confidenceLevel(verifiedFacts, missingData)),
                    verifiedFacts,
                    inferredInsights,
                    missingData,
                    resolveSourceStatus(verifiedFacts, missingData, pages),
                    rankingCategory(index++),
                    nextBestActions
            ));
        }
        return result;
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedCareer> enrichCareers(UniversitySourcesAnalysisResponse response,
                                                                                     UniversitySourcesAnalysisRequest request,
                                                                                     StudentProfile profile,
                                                                                     List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                                                                     List<UniversitySourcePageResult> pages) {
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> base = response.recommendedCareers() == null ? List.of() : response.recommendedCareers();
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> result = new ArrayList<>();
        int index = 0;
        for (UniversitySourcesAnalysisResponse.RecommendedCareer career : base) {
            List<String> verifiedFacts = extractCareerFacts(career, programmes, pages);
            List<String> missingData = verifiedFacts.isEmpty()
                    ? List.of("Official pathway requirements are limited in fetched sources.")
                    : List.of();
            result.add(new UniversitySourcesAnalysisResponse.RecommendedCareer(
                    career.name(),
                    career.reason(),
                    dedupeStrings(career.requirements()),
                    dedupeStrings(career.relatedProgrammes()),
                    firstNonBlank(career.recommendationReason(), buildCareerReason(career, request, profile, programmes)),
                    firstNonBlank(career.confidenceLevel(), confidenceLevel(verifiedFacts, missingData)),
                    verifiedFacts,
                    dedupeStrings(java.util.Collections.singletonList(career.reason())),
                    missingData,
                    resolveSourceStatus(verifiedFacts, missingData, pages),
                    rankingCategory(index++),
                    buildCareerActions(career, programmes)
            ));
        }
        return result;
    }

    private List<UniversitySourcesAnalysisResponse.SourceDiagnostic> buildDiagnostics(List<String> requestedUrls,
                                                                                      List<UniversitySourcePageResult> pages) {
        List<UniversitySourcesAnalysisResponse.SourceDiagnostic> diagnostics = new ArrayList<>();
        for (String requestedUrl : requestedUrls) {
            UniversitySourcePageResult page = pages.stream().filter(item -> requestedUrl.equals(item.sourceUrl())).findFirst().orElse(null);
            if (page == null) {
                diagnostics.add(new UniversitySourcesAnalysisResponse.SourceDiagnostic(requestedUrl, "FAILED", "Source was requested but no fetch result was recorded.", inferUniversity(requestedUrl), false));
                continue;
            }
            diagnostics.add(new UniversitySourcesAnalysisResponse.SourceDiagnostic(
                    page.sourceUrl(),
                    diagnosticStatus(page),
                    page.failureReason(),
                    inferUniversity(page.sourceUrl()),
                    page.success() && isProgrammeUsable(page)
            ));
        }
        return diagnostics;
    }

    private UniversitySourcesAnalysisResponse.SourceCoverage buildCoverage(List<String> requestedUrls,
                                                                          List<UniversitySourcePageResult> pages,
                                                                          List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes) {
        int successCount = (int) pages.stream().filter(UniversitySourcePageResult::success).count();
        int partialCount = (int) pages.stream().filter(page -> !page.success() && page.failureReason() != null && !page.failureReason().isBlank()).count();
        List<String> universities = dedupeStrings(programmes.stream().map(UniversitySourcesAnalysisResponse.RecommendedProgramme::university).collect(Collectors.toList()));
        return new UniversitySourcesAnalysisResponse.SourceCoverage(requestedUrls.size(), successCount, Math.max(0, pages.size() - successCount), partialCount, universities);
    }

    private List<String> extractVerifiedFacts(String programmeName, String university, List<UniversitySourcePageResult> pages) {
        Set<String> facts = new LinkedHashSet<>();
        String programmeNeedle = normalize(programmeName);
        String universityNeedle = normalize(university);
        for (UniversitySourcePageResult page : pages) {
            if (!page.success()) {
                continue;
            }
            String combined = normalize(page.pageTitle() + " " + page.cleanedText() + " " + String.join(" ", page.headings()));
            if ((!programmeNeedle.isBlank() && combined.contains(programmeNeedle))
                    || (!universityNeedle.isBlank() && combined.contains(universityNeedle))
                    || isProgrammeUsable(page)) {
                facts.add("Verified from " + inferUniversity(page.sourceUrl()) + ": " + summarizeFact(page));
            }
            if (facts.size() >= 3) {
                break;
            }
        }
        return new ArrayList<>(facts);
    }

    private List<String> extractCareerFacts(UniversitySourcesAnalysisResponse.RecommendedCareer career,
                                            List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                            List<UniversitySourcePageResult> pages) {
        Set<String> facts = new LinkedHashSet<>();
        for (UniversitySourcesAnalysisResponse.RecommendedProgramme programme : programmes) {
            if (programme.name() != null && career.relatedProgrammes() != null && career.relatedProgrammes().stream().anyMatch(item -> item.equalsIgnoreCase(programme.name()))) {
                facts.addAll(programme.verifiedFacts() == null ? List.of() : programme.verifiedFacts());
            }
        }
        if (facts.isEmpty()) {
            for (UniversitySourcePageResult page : pages) {
                if (page.success() && isProgrammeUsable(page)) {
                    facts.add("Verified from official source: " + summarizeFact(page));
                }
                if (facts.size() >= 2) {
                    break;
                }
            }
        }
        return new ArrayList<>(facts);
    }

    private List<String> buildMissingProgrammeData(UniversitySourcesAnalysisResponse.RecommendedProgramme programme,
                                                   List<String> verifiedFacts) {
        List<String> missing = new ArrayList<>();
        String combinedRequirements = String.join(" ", programme.admissionRequirements() == null ? List.of() : programme.admissionRequirements()).toLowerCase(Locale.ROOT);
        String combinedFacts = String.join(" ", verifiedFacts).toLowerCase(Locale.ROOT);
        if (!combinedRequirements.contains("aps") && !combinedFacts.contains("aps")) {
            missing.add("APS not found in fetched sources.");
        }
        if (!combinedRequirements.contains("deadline") && !combinedFacts.contains("deadline")) {
            missing.add("Deadline missing from fetched sources.");
        }
        if (verifiedFacts.isEmpty()) {
            missing.add("Official page unreachable or lacked programme-specific details.");
        }
        return missing;
    }

    private List<String> buildProgrammeActions(UniversitySourcesAnalysisResponse.RecommendedProgramme programme,
                                               List<String> missingData) {
        List<String> actions = new ArrayList<>();
        actions.add("Compare your subjects against the official programme requirements.");
        if (!missingData.isEmpty()) {
            actions.add("Open the official admissions page to verify missing APS, deadline, or subject details.");
        }
        if (programme.university() != null && !programme.university().isBlank()) {
            actions.add("Shortlist " + programme.university() + " for direct application tracking.");
        }
        return dedupeStrings(actions);
    }

    private List<String> buildCareerActions(UniversitySourcesAnalysisResponse.RecommendedCareer career,
                                            List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes) {
        List<String> actions = new ArrayList<>();
        actions.add("Review the linked programmes for " + career.name() + ".");
        if (career.requirements() != null && !career.requirements().isEmpty()) {
            actions.add("Strengthen the top requirement gaps before applying.");
        }
        if (!programmes.isEmpty()) {
            actions.add("Compare universities offering the strongest-fit related programmes.");
        }
        return dedupeStrings(actions);
    }

    private String buildProgrammeReason(UniversitySourcesAnalysisResponse.RecommendedProgramme programme,
                                        UniversitySourcesAnalysisRequest request,
                                        StudentProfile profile,
                                        List<String> verifiedFacts) {
        List<String> reasons = new ArrayList<>();
        if (containsIgnoreCase(programme.name(), request.targetProgram())) {
            reasons.add("matches your target programme");
        }
        if (containsIgnoreCase(programme.name(), request.careerInterest())) {
            reasons.add("aligns with your stated career interest");
        }
        if (containsIgnoreCase(profile.getInterests(), programme.name()) || containsIgnoreCase(profile.getSkills(), programme.name())) {
            reasons.add("connects with your profile interests or skills");
        }
        if (!verifiedFacts.isEmpty()) {
            reasons.add("has supporting official source evidence");
        }
        return reasons.isEmpty() ? "Recommended as a practical next-fit programme based on your profile and available university evidence." :
                "Recommended because it " + String.join(", ", reasons) + ".";
    }

    private String buildCareerReason(UniversitySourcesAnalysisResponse.RecommendedCareer career,
                                     UniversitySourcesAnalysisRequest request,
                                     StudentProfile profile,
                                     List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes) {
        List<String> reasons = new ArrayList<>();
        if (containsIgnoreCase(career.name(), request.careerInterest())) {
            reasons.add("directly reflects your career interest");
        }
        if (containsIgnoreCase(profile.getInterests(), career.name()) || containsIgnoreCase(profile.getSkills(), career.name())) {
            reasons.add("fits your profile interests or skills");
        }
        if (career.relatedProgrammes() != null && !career.relatedProgrammes().isEmpty()) {
            reasons.add("links to available programme pathways");
        }
        if (!programmes.isEmpty()) {
            reasons.add("is supported by multi-university programme options");
        }
        return reasons.isEmpty() ? "Recommended as a realistic pathway given your current profile." : "Recommended because it " + String.join(", ", reasons) + ".";
    }

    private String buildScoreReason(UniversitySourcesAnalysisResponse response,
                                    UniversitySourcesAnalysisRequest request,
                                    StudentProfile profile,
                                    List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                    List<UniversitySourcePageResult> pages) {
        int score = response.suitabilityScore() == null ? 0 : response.suitabilityScore();
        int successCount = (int) pages.stream().filter(UniversitySourcePageResult::success).count();
        return "Suitability score " + score + " was based on the match between your requested pathway, stored profile signals, and "
                + successCount + " successfully fetched official source(s). "
                + (programmes.isEmpty() ? "The score is lower-confidence because few programme matches were available." : "Programme matches and verified facts increased confidence.");
    }

    private List<String> buildScoreSignals(UniversitySourcesAnalysisRequest request,
                                           StudentProfile profile,
                                           List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                           List<UniversitySourcePageResult> pages) {
        List<String> signals = new ArrayList<>();
        if (request.targetProgram() != null && !request.targetProgram().isBlank()) {
            signals.add("Target programme: " + request.targetProgram());
        }
        if (request.careerInterest() != null && !request.careerInterest().isBlank()) {
            signals.add("Career interest: " + request.careerInterest());
        }
        if (profile.getInterests() != null && !profile.getInterests().isBlank()) {
            signals.add("Profile interests: " + profile.getInterests());
        }
        if (profile.getSkills() != null && !profile.getSkills().isBlank()) {
            signals.add("Profile skills: " + profile.getSkills());
        }
        if (!programmes.isEmpty()) {
            signals.add("Programme matches identified: " + programmes.size());
        }
        signals.add("Successful official sources: " + pages.stream().filter(UniversitySourcePageResult::success).count());
        return dedupeStrings(signals);
    }

    private List<String> buildScoreLimitations(List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                               List<UniversitySourcePageResult> pages) {
        List<String> limitations = new ArrayList<>();
        if (pages.stream().anyMatch(page -> !page.success())) {
            limitations.add("Some official pages failed or timed out, so source coverage is incomplete.");
        }
        if (programmes.stream().anyMatch(programme -> programme.missingData() != null && !programme.missingData().isEmpty())) {
            limitations.add("Missing APS, deadline, or subject detail reduced score certainty.");
        }
        if (limitations.isEmpty()) {
            limitations.add("No major data limitations were detected in the fetched sources.");
        }
        return limitations;
    }

    private List<String> dedupeRequirements(List<String> requirements) {
        List<String> safe = dedupeStrings(requirements);
        Set<String> normalized = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String item : safe) {
            String canonical = normalize(item)
                    .replace("not found in fetched sources", "missing requirement data")
                    .replace("verify exact programme requirements from official university programme pages.", "verify on official programme page");
            if (normalized.add(canonical)) {
                result.add(item);
            }
        }
        return result;
    }

    private List<String> dedupeStrings(List<String> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private String rankingCategory(int index) {
        if (index == 0) {
            return "Best match";
        }
        if (index < 3) {
            return "Strong alternative";
        }
        return "Explore option";
    }

    private String confidenceLevel(List<String> verifiedFacts, List<String> missingData) {
        if (verifiedFacts.size() >= 2 && missingData.isEmpty()) {
            return "HIGH";
        }
        if (!verifiedFacts.isEmpty()) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveSourceStatus(List<String> verifiedFacts, List<String> missingData, List<UniversitySourcePageResult> pages) {
        long failures = pages.stream().filter(page -> !page.success()).count();
        if (!verifiedFacts.isEmpty() && failures == 0) {
            return "SUCCESS";
        }
        if (!verifiedFacts.isEmpty()) {
            return "PARTIAL";
        }
        return missingData.isEmpty() ? "FAILED" : "PARTIAL";
    }

    private String diagnosticStatus(UniversitySourcePageResult page) {
        if (page.success() && isProgrammeUsable(page)) {
            return "SUCCESS";
        }
        if (page.success()) {
            return "PARTIAL";
        }
        return page.failureType() != null && page.failureType().name().contains("TIMEOUT") ? "TIMEOUT" : "FAILED";
    }

    private boolean isProgrammeUsable(UniversitySourcePageResult page) {
        return page.pageType() == UniversityPageType.PROGRAMME_DETAIL
                || page.pageType() == UniversityPageType.QUALIFICATION_LIST
                || page.pageType() == UniversityPageType.ADMISSIONS_OVERVIEW;
    }

    private String summarizeFact(UniversitySourcePageResult page) {
        String text = firstNonBlank(String.join(" | ", page.headings()), page.pageTitle(), page.cleanedText());
        text = text == null ? "Official page content was available." : text;
        return text.length() <= 140 ? text : text.substring(0, 140).trim() + "...";
    }

    private String inferUniversity(String url) {
        if (url == null) {
            return "University Source";
        }
        String normalized = url.toLowerCase(Locale.ROOT);
        if (normalized.contains("uct.ac.za")) return "University of Cape Town";
        if (normalized.contains("wits")) return "University of the Witwatersrand";
        if (normalized.contains("up.ac.za")) return "University of Pretoria";
        if (normalized.contains("sun.ac.za")) return "Stellenbosch University";
        if (normalized.contains("uj.ac.za")) return "University of Johannesburg";
        if (normalized.contains("unisa")) return "University of South Africa";
        return "University Source";
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return normalize(haystack).contains(normalize(needle)) && !normalize(needle).isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

