package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.MultiUniversityPageFetcherService;
import com.edurite.ai.university.PublicUniversitySourceDiscoveryService;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.ai.university.UniversitySourcesAggregatorService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.ai.exception.AiServiceException;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourcesGuidanceService {

    private static final Logger log = LoggerFactory.getLogger(UniversitySourcesGuidanceService.class);

    private final UniversitySourceRegistryService registryService;
    private final PublicUniversitySourceDiscoveryService discoveryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final UniversitySourcesAggregatorService aggregatorService;
    private final StudentService studentService;
    private final GeminiService geminiService;
    private final UniversityGuidanceResultEnricher resultEnricher;
    private final StudentPlanAccessService studentPlanAccessService;

    public UniversitySourcesGuidanceService(
            UniversitySourceRegistryService registryService,
            PublicUniversitySourceDiscoveryService discoveryService,
            MultiUniversityPageFetcherService pageFetcherService,
            UniversitySourcesAggregatorService aggregatorService,
            StudentService studentService,
            GeminiService geminiService,
            UniversityGuidanceResultEnricher resultEnricher,
            StudentPlanAccessService studentPlanAccessService
    ) {
        this.registryService = registryService;
        this.discoveryService = discoveryService;
        this.pageFetcherService = pageFetcherService;
        this.aggregatorService = aggregatorService;
        this.studentService = studentService;
        this.geminiService = geminiService;
        this.resultEnricher = resultEnricher;
        this.studentPlanAccessService = studentPlanAccessService;
    }

    public UniversitySourcesAnalysisResponse analyse(Principal principal, UniversitySourcesAnalysisRequest request) {
        Instant startedAt = Instant.now();
        StudentProfile profile = studentService.getProfileEntity(principal);
        StudentPlanAccessService.StudentPlanAccess planAccess = studentPlanAccessService.resolveByUserId(profile.getUserId());
        if (request.safeMaxRecommendations() > StudentPlanAccessService.BASIC_CAREER_GUIDANCE_LIMIT && !planAccess.premium()) {
            throw new AiServiceException(
                    HttpStatus.FORBIDDEN,
                    "PREMIUM_SUBSCRIPTION_REQUIRED",
                    "Premium subscription required.",
                    "Premium subscription required."
            );
        }

        int registrySize = registryService.configuredUniversityCount();
        int activeInstitutions = registryService.getActiveUniversities().size();
        int recommendationBudget = request.safeMaxRecommendations();
        int targetSourceLimit = request.usesDefaultSources()
                ? Math.min(Math.max(recommendationBudget * 2, 8), 16)
                : Math.min(Math.max(recommendationBudget * 2, 10), 40);

        log.info("University analysis pipeline starting: registrySize={}, activeInstitutions={}, usesDefaultSources={}, requestedSources={}, targetSourceLimit={}",
                registrySize,
                activeInstitutions,
                request.usesDefaultSources(),
                request.urls() == null ? 0 : request.urls().size(),
                targetSourceLimit);

        List<String> urls = resolveUrls(profile, request, targetSourceLimit);
        List<UniversitySourcePageResult> fetchedPages = fetchPagesSafely(urls, request.usesDefaultSources());
        String combinedContext = buildCombinedContextSafely(fetchedPages, profile, request);
        UniversitySourcesAnalysisResponse baseResponse = geminiService.getUniversitySourcesAdvice(request, profile, urls, fetchedPages, combinedContext);
        UniversitySourcesAnalysisResponse response = enrichSafely(baseResponse, request, profile, urls, fetchedPages);
        response = applyPipelineStatus(response, urls, fetchedPages);
        response = applyPlanGuidance(response, planAccess);

        long successfulPages = fetchedPages.stream().filter(UniversitySourcePageResult::success).count();
        log.info("University analysis completed: registrySize={}, activeInstitutions={}, requestedSources={}, discoveredUrlCount={}, fetchedPages={}, successfulSources={}, failedSources={}, mode={}, durationMs={}",
                registrySize,
                activeInstitutions,
                request.urls() == null ? 0 : request.urls().size(),
                urls.size(),
                fetchedPages.size(),
                successfulPages,
                Math.max(0, fetchedPages.size() - (int) successfulPages),
                response.mode(),
                Duration.between(startedAt, Instant.now()).toMillis());
        return response;
    }

    private UniversitySourcesAnalysisResponse applyPlanGuidance(
            UniversitySourcesAnalysisResponse response,
            StudentPlanAccessService.StudentPlanAccess planAccess
    ) {
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> careers = response.recommendedCareers() == null
                ? List.of()
                : response.recommendedCareers();
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes = response.recommendedProgrammes() == null
                ? List.of()
                : response.recommendedProgrammes();
        List<String> universities = response.recommendedUniversities() == null ? List.of() : response.recommendedUniversities();
        List<String> minimumRequirements = response.minimumRequirements() == null ? List.of() : response.minimumRequirements();
        List<String> keyRequirements = response.keyRequirements() == null ? List.of() : response.keyRequirements();
        List<String> skillGaps = response.skillGaps() == null ? List.of() : response.skillGaps();
        List<String> nextSteps = response.recommendedNextSteps() == null ? List.of() : response.recommendedNextSteps();
        List<String> warnings = new ArrayList<>(response.warnings() == null ? List.of() : response.warnings());

        Integer careerLimit = planAccess.careerSuggestionLimit();
        boolean limited = false;
        if (careerLimit != null) {
            List<UniversitySourcesAnalysisResponse.RecommendedCareer> limitedCareers = careers.stream().limit(careerLimit).toList();
            limited = limitedCareers.size() < careers.size();
            careers = limitedCareers;
            programmes = programmes.stream().limit(careerLimit).toList();
            universities = universities.stream().limit(careerLimit).toList();
            minimumRequirements = minimumRequirements.stream().limit(4).toList();
            keyRequirements = keyRequirements.stream().limit(4).toList();
            skillGaps = skillGaps.stream().limit(4).toList();
            nextSteps = nextSteps.stream().limit(careerLimit).toList();
            if (planAccess.upgradeMessage() != null && warnings.stream().noneMatch(planAccess.upgradeMessage()::equals)) {
                warnings.add(planAccess.upgradeMessage());
            }
        }

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
                universities,
                minimumRequirements,
                keyRequirements,
                skillGaps,
                nextSteps,
                warnings,
                response.suitabilityScore(),
                response.rawModelUsed(),
                response.suitabilityScoreReason(),
                response.suitabilitySignalsUsed(),
                response.suitabilityScoreLimitations(),
                response.sourceDiagnostics(),
                response.sourceCoverage(),
                planAccess.planCode(),
                planAccess.premium(),
                careerLimit,
                limited,
                planAccess.upgradeMessage(),
                response.available(),
                response.message()
        );
    }

    private List<String> resolveUrls(StudentProfile profile,
                                     UniversitySourcesAnalysisRequest request,
                                     int targetSourceLimit) {
        try {
            List<String> urls = request.usesDefaultSources()
                    ? discoveryService.discoverSources(profile, request, targetSourceLimit)
                    : registryService.deduplicate(request.urls()).stream().limit(targetSourceLimit).toList();
            if (urls.isEmpty()) {
                urls = registryService.getFallbackSources(targetSourceLimit);
                log.warn("University source discovery returned zero URLs; using registry fallback sources: requestedByDefaultSources={}, fallbackUrls={}",
                        request.usesDefaultSources(), urls.size());
            }
            log.info("University source discovery completed: requestedByDefaultSources={}, discoveredUrlCount={}, registryFallbackUsed={}",
                    request.usesDefaultSources(), urls.size(), request.usesDefaultSources() && !urls.isEmpty());
            return urls;
        } catch (RuntimeException ex) {
            List<String> fallbackUrls = registryService.getFallbackSources(targetSourceLimit);
            log.error("University source discovery failed: requestedByDefaultSources={}, targetSourceLimit={}, fallbackUrls={}, message={}",
                    request.usesDefaultSources(), targetSourceLimit, fallbackUrls.size(), ex.getMessage(), ex);
            return fallbackUrls;
        }
    }

    private List<UniversitySourcePageResult> fetchPagesSafely(List<String> urls, boolean requestedByDefaultSources) {
        try {
            List<UniversitySourcePageResult> fetchedPages = ensureTerminalResults(urls, pageFetcherService.fetchPages(urls));
            log.info("University page fetch completed: requestedByDefaultSources={}, requestedUrls={}, fetchedPages={}, successfulPages={}, failedPages={}",
                    requestedByDefaultSources,
                    urls.size(),
                    fetchedPages.size(),
                    fetchedPages.stream().filter(UniversitySourcePageResult::success).count(),
                    fetchedPages.stream().filter(page -> !page.success()).count());
            return fetchedPages;
        } catch (RuntimeException ex) {
            log.error("University page fetch failed: requestedByDefaultSources={}, requestedUrls={}, message={}",
                    requestedByDefaultSources, urls.size(), ex.getMessage(), ex);
            return buildFailedFetchResults(urls, "Fetch pipeline failed before individual source results were recorded.");
        }
    }

    private String buildCombinedContextSafely(List<UniversitySourcePageResult> fetchedPages,
                                              StudentProfile profile,
                                              UniversitySourcesAnalysisRequest request) {
        try {
            String combinedContext = aggregatorService.buildCombinedContext(fetchedPages, profile, request);
            log.info("University source aggregation completed: fetchedPages={}, combinedContextLength={}",
                    fetchedPages.size(), combinedContext.length());
            return combinedContext;
        } catch (RuntimeException ex) {
            log.error("University source aggregation failed: fetchedPages={}, message={}",
                    fetchedPages.size(), ex.getMessage(), ex);
            String partialContext = fetchedPages.stream()
                    .filter(UniversitySourcePageResult::success)
                    .map(page -> page.pageTitle() + "\n" + page.cleanedText())
                    .filter(value -> value != null && !value.isBlank())
                    .limit(3)
                    .reduce("", (left, right) -> left + "\n\n" + right)
                    .trim();
            if (!partialContext.isBlank()) {
                log.warn("Using simplified combined context fallback after aggregation failure: fallbackContextLength={}", partialContext.length());
            }
            return partialContext;
        }
    }

    private UniversitySourcesAnalysisResponse enrichSafely(UniversitySourcesAnalysisResponse response,
                                                           UniversitySourcesAnalysisRequest request,
                                                           StudentProfile profile,
                                                           List<String> urls,
                                                           List<UniversitySourcePageResult> fetchedPages) {
        try {
            return resultEnricher.enrich(response, request, profile, urls, fetchedPages);
        } catch (RuntimeException ex) {
            log.error("University guidance enrichment failed: urls={}, fetchedPages={}, message={}",
                    urls.size(), fetchedPages.size(), ex.getMessage(), ex);
            return response;
        }
    }

    private UniversitySourcesAnalysisResponse applyPipelineStatus(UniversitySourcesAnalysisResponse response,
                                                                  List<String> urls,
                                                                  List<UniversitySourcePageResult> fetchedPages) {
        List<String> successfulUrls = fetchedPages.stream()
                .filter(UniversitySourcePageResult::success)
                .map(UniversitySourcePageResult::sourceUrl)
                .toList();
        List<String> failedUrls = fetchedPages.stream()
                .filter(page -> !page.success())
                .map(UniversitySourcePageResult::sourceUrl)
                .toList();
        boolean hasRequestedSources = !urls.isEmpty();
        boolean hasSuccessfulSources = !successfulUrls.isEmpty();
        boolean hasFailures = !failedUrls.isEmpty() || (hasRequestedSources && successfulUrls.size() < urls.size());

        String mode = response.mode();
        String warningMessage = response.warningMessage();
        if (hasSuccessfulSources && hasFailures) {
            mode = "PARTIAL";
            warningMessage = mergeWarning(warningMessage,
                    "Some university sources could not be analysed, so EduRite returned the successful official sources that were available.");
        } else if (hasSuccessfulSources) {
            mode = response.fallbackUsed() ? "FALLBACK" : "LIVE";
        } else if (hasRequestedSources) {
            mode = response.fallbackUsed() ? "PARTIAL" : "UNAVAILABLE";
            warningMessage = mergeWarning(warningMessage,
                    "University sources were requested, but no official pages could be analysed successfully. Returning the best available profile-based guidance.");
        } else {
            mode = "UNAVAILABLE";
        }

        Set<String> requestedSources = new LinkedHashSet<>();
        requestedSources.addAll(response.requestedSources() == null ? List.of() : response.requestedSources());
        requestedSources.addAll(urls);

        List<String> warnings = new ArrayList<>();
        if (response.warnings() != null) {
            warnings.addAll(response.warnings());
        }
        if (hasFailures) {
            warnings.add("Some requested university sources were unavailable or only partially usable, so EduRite continued with the successful sources.");
        }

        if (Boolean.FALSE.equals(response.aiLive())) {
            String unavailableWarning = mergeWarning(response.warningMessage(),
                    "Live AI guidance is unavailable, so no generated recommendations are shown.");
            if (warnings.stream().noneMatch(unavailableWarning::equals)) {
                warnings.add(unavailableWarning);
            }
            List<com.edurite.ai.dto.UniversitySourcesAnalysisResponse.SourceDiagnostic> diagnostics =
                    response.sourceDiagnostics() == null || response.sourceDiagnostics().isEmpty()
                            ? buildSourceDiagnostics(urls, fetchedPages)
                            : response.sourceDiagnostics();
            com.edurite.ai.dto.UniversitySourcesAnalysisResponse.SourceCoverage sourceCoverage = response.sourceCoverage() == null
                    ? buildSourceCoverage(urls, successfulUrls, failedUrls)
                    : response.sourceCoverage();
            return new UniversitySourcesAnalysisResponse(
                    false,
                    false,
                    "ERROR",
                    "UNAVAILABLE",
                    response.groundingStatus(),
                    response.evidenceCoverage(),
                    unavailableWarning,
                    List.copyOf(requestedSources),
                    urls,
                    successfulUrls,
                    failedUrls,
                    successfulUrls.size(),
                    response.summary(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    warnings,
                    0,
                    response.rawModelUsed(),
                    response.suitabilityScoreReason(),
                    response.suitabilitySignalsUsed(),
                    response.suitabilityScoreLimitations(),
                    diagnostics,
                    sourceCoverage,
                    response.planCode(),
                    response.premiumUnlocked(),
                    response.careerSuggestionLimit(),
                    response.careerSuggestionsLimited(),
                    response.upgradeMessage(),
                    false,
                    "AI Guidance is currently not available. Please try again later."
            );
        }

        List<com.edurite.ai.dto.UniversitySourcesAnalysisResponse.SourceDiagnostic> diagnostics =
                response.sourceDiagnostics() == null || response.sourceDiagnostics().isEmpty()
                        ? buildSourceDiagnostics(urls, fetchedPages)
                        : response.sourceDiagnostics();
        com.edurite.ai.dto.UniversitySourcesAnalysisResponse.SourceCoverage sourceCoverage = response.sourceCoverage() == null
                ? buildSourceCoverage(urls, successfulUrls, failedUrls)
                : response.sourceCoverage();

        return new UniversitySourcesAnalysisResponse(
                response.aiLive(),
                response.fallbackUsed(),
                deriveStatus(mode, hasSuccessfulSources, hasRequestedSources),
                mode,
                response.groundingStatus(),
                response.evidenceCoverage(),
                warningMessage,
                List.copyOf(requestedSources),
                urls,
                successfulUrls,
                failedUrls,
                successfulUrls.size(),
                response.summary(),
                response.inferredGuidance(),
                response.recommendedCareers(),
                response.recommendedProgrammes(),
                response.bursarySuggestions(),
                response.recommendedUniversities(),
                response.minimumRequirements(),
                response.keyRequirements(),
                response.skillGaps(),
                response.recommendedNextSteps(),
                warnings,
                response.suitabilityScore(),
                response.rawModelUsed(),
                response.suitabilityScoreReason(),
                response.suitabilitySignalsUsed(),
                response.suitabilityScoreLimitations(),
                diagnostics,
                sourceCoverage,
                response.planCode(),
                response.premiumUnlocked(),
                response.careerSuggestionLimit(),
                response.careerSuggestionsLimited(),
                response.upgradeMessage(),
                true,
                null
        );
    }


    private List<com.edurite.ai.dto.UniversitySourcesAnalysisResponse.SourceDiagnostic> buildSourceDiagnostics(List<String> urls,
                                                                                                                 List<UniversitySourcePageResult> fetchedPages) {
        return urls.stream()
                .map(url -> fetchedPages.stream()
                        .filter(page -> url.equals(page.sourceUrl()))
                        .findFirst()
                        .map(page -> new com.edurite.ai.dto.UniversitySourcesAnalysisResponse.SourceDiagnostic(
                                page.sourceUrl(),
                                page.success() ? "SUCCESS" : "FAILED",
                                page.failureReason(),
                                inferUniversity(url),
                                page.success()))
                        .orElseGet(() -> new com.edurite.ai.dto.UniversitySourcesAnalysisResponse.SourceDiagnostic(
                                url,
                                "FAILED",
                                "Source was requested but no fetch result was recorded; terminal failure was synthesized by the pipeline.",
                                inferUniversity(url),
                                false)))
                .toList();
    }

    private com.edurite.ai.dto.UniversitySourcesAnalysisResponse.SourceCoverage buildSourceCoverage(List<String> urls,
                                                                                                     List<String> successfulUrls,
                                                                                                     List<String> failedUrls) {
        return new com.edurite.ai.dto.UniversitySourcesAnalysisResponse.SourceCoverage(
                urls.size(),
                successfulUrls.size(),
                failedUrls.size(),
                failedUrls.size(),
                List.of()
        );
    }

    private String inferUniversity(String url) {
        if (url == null || url.isBlank()) {
            return "Unknown";
        }
        return registryService.getActiveUniversities().stream()
                .filter(entry -> registryService.isAllowedUrlForUniversity(entry.getUniversityName(), url))
                .map(com.edurite.ai.university.UniversityRegistryProperties.UniversityRegistryEntry::getUniversityName)
                .findFirst()
                .orElse("Unknown");
    }

    private String deriveStatus(String mode, boolean hasSuccessfulSources, boolean hasRequestedSources) {
        if ("PARTIAL".equalsIgnoreCase(mode) || (hasSuccessfulSources && hasRequestedSources)) {
            return "PARTIAL";
        }
        if ("LIVE".equalsIgnoreCase(mode) || "FALLBACK".equalsIgnoreCase(mode)) {
            return "SUCCESS";
        }
        return "ERROR";
    }


    private List<UniversitySourcePageResult> ensureTerminalResults(List<String> urls, List<UniversitySourcePageResult> fetchedPages) {
        List<UniversitySourcePageResult> safeResults = fetchedPages == null ? new ArrayList<>() : new ArrayList<>(fetchedPages);
        Set<String> recorded = new LinkedHashSet<>();
        for (UniversitySourcePageResult page : safeResults) {
            recorded.add(page.sourceUrl());
        }
        for (String requestedUrl : urls) {
            if (recorded.contains(requestedUrl)) {
                continue;
            }
            safeResults.add(new UniversitySourcePageResult(requestedUrl, "", com.edurite.ai.university.UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false,
                    "Source was requested but no fetch result was recorded; terminal failure was synthesized by the pipeline.",
                    com.edurite.ai.university.UniversityCrawlFailureType.FETCH_ERROR));
        }
        return List.copyOf(safeResults);
    }

    private List<UniversitySourcePageResult> buildFailedFetchResults(List<String> urls, String failureReason) {
        return urls.stream()
                .map(url -> new UniversitySourcePageResult(url, "", com.edurite.ai.university.UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false,
                        failureReason, com.edurite.ai.university.UniversityCrawlFailureType.FETCH_ERROR))
                .toList();
    }

    private String mergeWarning(String currentWarning, String additionalWarning) {
        if (additionalWarning == null || additionalWarning.isBlank()) {
            return currentWarning;
        }
        if (currentWarning == null || currentWarning.isBlank()) {
            return additionalWarning;
        }
        if (currentWarning.contains(additionalWarning)) {
            return currentWarning;
        }
        return currentWarning + " " + additionalWarning;
    }

    public List<String> getDefaultSources() {
        return registryService.getDefaultSources();
    }
}

