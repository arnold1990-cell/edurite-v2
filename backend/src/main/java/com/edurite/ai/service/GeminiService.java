package com.edurite.ai.service;

import com.edurite.ai.config.GeminiProperties;
import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GeminiService implements AiProviderService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE = "AI guidance is temporarily unavailable. Please try again later.";
    private static final String NORMALIZED_UNIVERSITY_RESPONSE_WARNING = "Live model response format differed from the required JSON schema; EduRite normalized the response.";
    private static final long[] RETRY_BACKOFF_MS = {300L, 800L};
    private static final MediaType JSON = MediaType.get("application/json");
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties geminiProperties;
    private final AiProviderOrchestratorService aiProviderOrchestratorService;
    private final int maxGeminiRetries;

    public GeminiService(ObjectMapper objectMapper,
                         GeminiProperties geminiProperties,
                         @Lazy AiProviderOrchestratorService aiProviderOrchestratorService) {
        this.objectMapper = objectMapper;
        this.geminiProperties = geminiProperties;
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
        this.maxGeminiRetries = Math.max(0, geminiProperties.getMaxRetries());
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(geminiProperties.getConnectTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(geminiProperties.getReadTimeoutSeconds()))
                .writeTimeout(Duration.ofSeconds(geminiProperties.getWriteTimeoutSeconds()))
                .callTimeout(Duration.ofSeconds(geminiProperties.getCallTimeoutSeconds()))
                .build();
    }

    @PostConstruct
    void logStartupConfiguration() {
        String resolvedKey = resolveApiKey();
        String configuredModelInput = resolveConfiguredModelInput();
        String resolvedModel = resolveModel();
        String resolvedBaseUrl = resolveBaseUrl();

        if (GeminiModelResolver.fallsBackToDefault(configuredModelInput)) {
            log.warn("Gemini configuration warning: configured model '{}' is invalid. Falling back to default model={}.",
                    trim(configuredModelInput), DEFAULT_MODEL);
        }

        if (resolvedKey.isBlank()) {
            log.warn("Gemini configuration loaded without an API key. Gemini-backed AI features are disabled until `gemini.api-key` or `GEMINI_API_KEY` is configured.");
            return;
        }
        log.info("Gemini configuration loaded: apiKeyPresent=true, model={}, baseUrl={}", resolvedModel, resolvedBaseUrl);

        boolean startupCheckEnabled = geminiProperties.isStartupHealthCheckEnabled();
        if (startupCheckEnabled) {
            GeminiHealthCheck healthCheck = checkHealth();
            if (healthCheck.endpointReachable()) {
                log.info("Gemini startup provider check succeeded: model={}, endpoint={}", healthCheck.model(), healthCheck.endpoint());
            } else {
                log.warn("Gemini startup provider check warning: model={}, endpoint={}, message={}",
                        healthCheck.model(), healthCheck.endpoint(), healthCheck.message());
            }
        } else {
            log.info("Gemini startup provider check skipped (gemini.startup-health-check-enabled=false).");
        }
    }



    public GeminiHealthCheck checkHealth() {
        GeminiRequestConfig config = resolveRequestConfig(false);
        String resolvedApiKey = config.apiKey();
        String resolvedModel = config.model();
        String endpoint = config.modelInfoEndpoint();

        if (resolvedApiKey.isBlank()) {
            return new GeminiHealthCheck(false, false, resolvedModel, endpoint,
                    "Gemini API key is missing.");
        }

        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("x-goog-api-key", resolvedApiKey)
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            int statusCode = response.code();
            if (statusCode >= 200 && statusCode < 300) {
                return new GeminiHealthCheck(true, true, resolvedModel, endpoint, "Gemini endpoint reachable.");
            }
            String body = readSnippet(response.body());
            log.info("Gemini health check response: status={}, endpoint={}", statusCode, endpoint);
            return new GeminiHealthCheck(true, false, resolvedModel, endpoint,
                    "Gemini endpoint check failed with status " + statusCode + ": " + trim(body));
        } catch (Exception ex) {
            log.warn("Gemini health check failed: model={}, endpoint={}, message={}", resolvedModel, endpoint, ex.getMessage(), ex);
            return new GeminiHealthCheck(true, false, resolvedModel, endpoint,
                    "Gemini endpoint check exception: " + ex.getMessage());
        }
    }

    public CareerAdviceResponse getCareerAdvice(CareerAdviceRequest request) {
        String modelText = aiProviderOrchestratorService != null
                ? aiProviderOrchestratorService.generateContent(buildPrompt(request))
                : generateContent(buildPrompt(request));
        return parseCareerAdvice(modelText);
    }

    public UniversitySourcesAnalysisResponse getUniversitySourcesAdvice(
            UniversitySourcesAnalysisRequest request,
            com.edurite.student.entity.StudentProfile profile,
            List<String> sourceUrls,
            List<UniversitySourcePageResult> fetchedPages,
            String combinedContext
    ) {
        List<String> safeSourceUrls = sourceUrls == null ? List.of() : sourceUrls;
        List<UniversitySourcePageResult> safeFetchedPages = fetchedPages == null ? List.of() : fetchedPages;
        String safeCombinedContext = combinedContext == null ? "" : combinedContext.trim();

        List<String> successUrls = safeFetchedPages.stream().filter(UniversitySourcePageResult::success)
                .map(UniversitySourcePageResult::sourceUrl).toList();
        List<String> failedUrls = safeFetchedPages.stream().filter(page -> !page.success())
                .map(UniversitySourcePageResult::sourceUrl).toList();
        int sourceUrlCount = safeSourceUrls.size();
        int fetchedPageCount = safeFetchedPages.size();
        int contextLength = safeCombinedContext.length();

        List<String> sourceLimitations = new ArrayList<>();
        if (sourceUrlCount == 0) {
            sourceLimitations.add("No external university sources were available for this request; guidance was generated from profile context.");
        }
        if (fetchedPageCount == 0) {
            sourceLimitations.add("No university pages were fetched; guidance was generated from profile context.");
        }
        if (contextLength == 0) {
            sourceLimitations.add("No combined source context was available; guidance was generated from profile context.");
        }

        log.info("University guidance context: sourceUrls={}, fetchedPages={}, successfulPages={}, failedPages={}, combinedContextLength={}",
                sourceUrlCount, fetchedPageCount, successUrls.size(), failedUrls.size(), contextLength);

        try {
            String prompt = buildUniversityPrompt(request, profile, safeFetchedPages, safeCombinedContext);
            log.info("University guidance Gemini-only execution: sourceUrls={}, successfulSources={}, failedSources={}",
                    safeSourceUrls.size(), successUrls.size(), failedUrls.size());
            String modelText = generateContent(prompt);
            UniversitySourcesAnalysisResponse parsed = parseUniversityAdvice(modelText, safeSourceUrls, successUrls, failedUrls);
            log.info("University guidance Gemini call succeeded: recommendations={}, programmes={}",
                    parsed.recommendedCareers() == null ? 0 : parsed.recommendedCareers().size(),
                    parsed.recommendedProgrammes() == null ? 0 : parsed.recommendedProgrammes().size());
            return withRuntimeWarnings(enrichWithWarnings(parsed, failedUrls), sourceLimitations);
        } catch (Exception ex) {
            log.warn("University guidance Gemini call failed: reason={}", ex.getMessage(), ex);
            String primaryWarning = liveAttemptFailureWarning(ex);
            return fallbackUniversityResponse(request, safeSourceUrls, successUrls, failedUrls,
                    buildFallbackWarnings(sourceLimitations,
                            primaryWarning));
        }
    }

    @Override
    public String generateContent(String prompt) {
        GeminiRequestConfig config = resolveRequestConfig(true);
        if (config.apiKey().isBlank() || config.model().isBlank() || config.baseUrl().isBlank()) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_PROVIDER_CONFIGURATION_INVALID",
                    "Gemini provider configuration is incomplete.",
                    FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
        }
        return invokeGemini(prompt, config);
    }

    private String invokeGemini(String prompt, GeminiRequestConfig config) {
        String requestBody = serializeGenerateContentRequest(prompt);

        Request httpRequest = new Request.Builder()
                .url(config.generateEndpoint())
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", config.apiKey())
                .post(RequestBody.create(requestBody, JSON))
                .build();

        log.info("Starting Gemini call: model={}, endpointPath={}, endpoint={}, apiKeyPresent={}",
                config.model(), config.endpointPath(), config.generateEndpoint(), !config.apiKey().isBlank());

        for (int attempt = 0; attempt <= maxGeminiRetries; attempt++) {
            boolean retry = attempt < maxGeminiRetries;
            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                int statusCode = response.code();
                String bodySnippet = readSnippet(response.body());
                log.info("Gemini HTTP response received: status={}, model={}, attempt={}, retried={}",
                        statusCode, config.model(), attempt + 1, attempt > 0);

                if (response.isSuccessful()) {
                    if (bodySnippet.isBlank()) {
                        throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                                "AI_PROVIDER_RESPONSE_INVALID",
                                "Gemini returned an empty response body.",
                                "AI provider returned an empty response. Please try again shortly.");
                    }
                    return extractModelText(bodySnippet);
                }

                boolean retriableStatus = statusCode == 408 || statusCode == 429 || statusCode >= 500;
                log.warn("Gemini call failed: status={}, model={}, retried={}, snippet={}",
                        statusCode, config.model(), attempt > 0, sanitizeProviderBody(bodySnippet));

                if (retry && retriableStatus) {
                    sleepBeforeRetry(attempt);
                    continue;
                }

                throw buildProviderFailure(statusCode, bodySnippet);
            } catch (IOException ex) {
                boolean retryableIoFailure = retry && isRetryableIoFailure(ex);
                log.warn("Gemini call IO failure: model={}, attempt={}, retried={}, retryableIoFailure={}, message={}",
                        config.model(), attempt + 1, attempt > 0, retryableIoFailure, ex.getMessage(), ex);
                if (retryableIoFailure) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw new AiServiceException(HttpStatus.GATEWAY_TIMEOUT,
                        "AI_PROVIDER_TIMEOUT",
                        "Gemini request timed out or failed.",
                        FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
            }
        }

        throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI_PROVIDER_UNAVAILABLE",
                "Gemini request failed after retries.",
                FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
    }

    private String liveAttemptFailureWarning(Exception ex) {
        if (ex instanceof AiServiceException aiServiceException) {
            log.warn("Gemini live attempt failed: errorCode={}, status={}, detail={}",
                    aiServiceException.getErrorCode(),
                    aiServiceException.getStatus(),
                    trim(aiServiceException.getMessage()));
        }
        return FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE;
    }

    private AiServiceException buildProviderFailure(int statusCode, String bodySnippet) {
        ProviderErrorDetails providerErrorDetails = extractProviderErrorDetails(bodySnippet);
        String detail = firstNonBlank(providerErrorDetails.message(), sanitizeProviderBody(bodySnippet));
        String message = "Gemini request failed with status " + statusCode
                + (detail.isBlank() ? "" : ". " + detail);

        if (statusCode == 401 || statusCode == 403) {
            return new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "AI_PROVIDER_AUTH",
                    message,
                    FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
        }
        if (statusCode == 404 || statusCode == 400) {
            return new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "AI_PROVIDER_CONFIGURATION_INVALID",
                    message,
                    FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
        }
        if (statusCode == 408 || statusCode == 429 || statusCode >= 500) {
            return new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_PROVIDER_UNAVAILABLE",
                    message,
                    FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
        }
        return new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI_PROVIDER_REQUEST_FAILED",
                message,
                FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
    }

    private String buildPrompt(CareerAdviceRequest request) {
        return """
                You are a career guidance assistant.
                Return ONLY strict JSON with this exact schema:
                {
                  \"recommendedCareers\": [
                    {
                      \"name\": \"string\",
                      \"matchScore\": 0,
                      \"reason\": \"string\",
                      \"improvements\": [\"string\"]
                    }
                  ]
                }
                Rules:
                - Output valid JSON only (no markdown, no prose, no code fences).
                - Recommend 3-5 careers.
                - matchScore must be integer from 0 to 100.
                - reason and improvements should be concise and actionable.

                Student profile:
                qualificationLevel: %s
                interests: %s
                skills: %s
                location: %s
                """.formatted(
                sanitizePromptValue(request.qualificationLevel()),
                sanitizePromptValue(request.interests()),
                sanitizePromptValue(request.skills()),
                sanitizePromptValue(request.location())
        );
    }

    private String buildUniversityPrompt(UniversitySourcesAnalysisRequest request,
                                         com.edurite.student.entity.StudentProfile profile,
                                         List<UniversitySourcePageResult> fetchedPages,
                                         String combinedContext) {
        String pageMetadata = fetchedPages.stream()
                .map(page -> "%s | %s | %s | keywords=%s".formatted(
                        page.sourceUrl(), page.success() ? "success" : "failed", page.pageType(), page.extractedKeywords()))
                .reduce("", (a, b) -> a + "\n" + b);

        return """
                You are EduRite's academic and career guidance assistant.
                Return ONLY valid JSON with this schema:
                {
                  "recommendedCareers": [
                    {
                      "name": "string",
                      "reason": "string",
                      "requirements": ["string"],
                      "relatedProgrammes": ["string"]
                    }
                  ],
                  "recommendedProgrammes": [
                    {
                      "name": "string",
                      "university": "string",
                      "admissionRequirements": ["string"],
                      "notes": "string"
                    }
                  ],
                  "recommendedUniversities": ["string"],
                  "minimumRequirements": ["string"],
                  "keyRequirements": ["string"],
                  "skillGaps": ["string"],
                  "recommendedNextSteps": ["string"],
                  "warnings": ["string"],
                  "inferredGuidance": ["string"],
                  "bursarySuggestions": [
                    {
                      "name": "string",
                      "provider": "string",
                      "eligibility": ["string"],
                      "notes": "string",
                      "sourceUrls": ["string"],
                      "officialSource": true
                    }
                  ],
                  "summary": "string",
                  "suitabilityScore": 0
                }

                Rules:
                - Return student-friendly, practical guidance.
                - Output valid JSON only (no markdown, no code fences, no prose outside JSON).
                - Keep sourced facts separate from inferredGuidance.
                - Recommend at least %d careers if enough evidence exists.
                - Recommend at least %d university programmes if enough evidence exists.
                - Each recommended career must include specific requirements and relatedProgrammes.
                - Each recommended programme must include admissionRequirements and notes.
                - Do not include application due dates or deadline fields anywhere.
                - minimumRequirements MUST always mention Grade 12 passes, English, and Mathematics for mathematics-related pathways.
                - Do not hallucinate APS scores, subject minimums, bursary criteria, or due dates.
                - Use only the retrieved source content and the student profile as evidence.
                - If a fact is not explicitly supported by the retrieved content, return "Not found in fetched sources" for that field.
                - If source metadata/context is empty, keep recommendations conservative and explain the limitation in warnings.
                - Prefer official university/provider pages over secondary pages when choosing bursarySuggestions and programme facts.
                - Keep suitabilityScore between 0 and 100.
                - If model cannot provide clean JSON, still provide section headings with bullet points.

                Student profile:
                firstName: %s
                lastName: %s
                qualificationLevel: %s
                interests: %s
                skills: %s
                experience: %s
                location: %s
                cvUploaded: %s
                transcriptUploaded: %s

                Request focus:
                targetProgram: %s
                careerInterest: %s
                qualificationLevel: %s

                Source metadata:
                %s

                Combined academic context (truncated):
                %s
                """.formatted(
                request.safeMaxRecommendations(),
                request.safeMaxRecommendations(),
                sanitizePromptValue(profile.getFirstName()),
                sanitizePromptValue(profile.getLastName()),
                sanitizePromptValue(profile.getQualificationLevel()),
                sanitizePromptValue(profile.getInterests()),
                sanitizePromptValue(profile.getSkills()),
                sanitizePromptValue(profile.getExperience()),
                sanitizePromptValue(profile.getLocation()),
                profile.getCvFileUrl() != null,
                profile.getTranscriptFileUrl() != null,
                sanitizePromptValue(request.targetProgram()),
                sanitizePromptValue(request.careerInterest()),
                sanitizePromptValue(request.qualificationLevel()),
                sanitizePromptValue(pageMetadata),
                sanitizePromptValue(combinedContext)
        );
    }

    private String extractModelText(String geminiBody) {
        if (geminiBody == null || geminiBody.isBlank()) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "AI_PROVIDER_RESPONSE_INVALID",
                    "Gemini returned an empty response body.",
                    FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
        }

        try {
            JsonElement rootElement = JsonParser.parseString(geminiBody);
            if (!rootElement.isJsonObject()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "AI_PROVIDER_RESPONSE_INVALID",
                        "Gemini returned a non-object response payload.",
                        FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
            }
            JsonObject root = rootElement.getAsJsonObject();

            ProviderErrorDetails providerError = extractProviderErrorDetails(root);
            if (providerError.hasError()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "AI_PROVIDER_REQUEST_FAILED",
                        "Gemini returned provider error payload: " + providerError.toMessage(),
                        FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
            }

            String blockReason = extractPromptBlockReason(root);

            LinkedHashSet<String> textParts = new LinkedHashSet<>();
            JsonArray candidates = getJsonArray(root, "candidates");
            collectCandidateText(candidates, textParts);
            collectTextValues(root.get("content"), textParts);
            collectTextValues(root.get("parts"), textParts);
            collectTextValues(root.get("output"), textParts);
            collectTextValues(root.get("outputs"), textParts);
            collectTextValues(root.get("message"), textParts);
            collectTextValues(root.get("messages"), textParts);
            collectTextValues(root.get("text"), textParts);
            collectTextValues(root.get("output_text"), textParts);

            if (textParts.isEmpty() && !blockReason.isBlank()) {
                throw buildBlockedProviderResponseException(blockReason);
            }

            if (textParts.isEmpty()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "AI_PROVIDER_RESPONSE_INVALID",
                        "Gemini returned no candidate text parts.",
                        FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
            }

            return stripCodeFences(String.join("\n", textParts));
        } catch (AiServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Gemini payload parsing failed before extracting model text. snippet={}", sanitizeProviderBody(geminiBody), ex);
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "AI_PROVIDER_RESPONSE_INVALID",
                    "Gemini returned malformed JSON payload.",
                    FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
        }
    }

    private void collectCandidateText(JsonArray candidates, Set<String> collector) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (int i = 0; i < candidates.size(); i++) {
            JsonElement candidate = candidates.get(i);
            if (!candidate.isJsonObject()) {
                continue;
            }
            JsonObject candidateObject = candidate.getAsJsonObject();
            collectTextValues(candidateObject.get("content"), collector);
            collectTextValues(candidateObject.get("parts"), collector);
            collectTextValues(candidateObject.get("output"), collector);
            collectTextValues(candidateObject.get("output_text"), collector);
        }
    }

    private void collectTextValues(JsonElement element, Set<String> collector) {
        collectTextValues(element, collector, null, 0);
    }

    private void collectTextValues(JsonElement element, Set<String> collector, String keyHint, int depth) {
        if (element == null || element.isJsonNull() || depth > 12) {
            return;
        }

        if (element.isJsonPrimitive()) {
            if (keyHint != null && isTextKey(keyHint)) {
                String value = element.getAsString();
                if (value != null && !value.isBlank()) {
                    collector.add(value.trim());
                }
            }
            return;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                collectTextValues(array.get(i), collector, keyHint, depth + 1);
            }
            return;
        }

        if (!element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if ("error".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            collectTextValues(entry.getValue(), collector, entry.getKey(), depth + 1);
        }
    }

    private boolean isTextKey(String key) {
        String normalized = key == null ? "" : key.trim().toLowerCase();
        return "text".equals(normalized)
                || "output_text".equals(normalized)
                || "content".equals(normalized);
    }

    private String extractPromptBlockReason(JsonObject root) {
        JsonObject promptFeedback = getJsonObject(root, "promptFeedback");
        String blockReason = getString(promptFeedback, "blockReason");
        if (!blockReason.isBlank()) {
            return blockReason;
        }

        JsonArray candidates = getJsonArray(root, "candidates");
        if (candidates == null) {
            return "";
        }
        for (int i = 0; i < candidates.size(); i++) {
            JsonElement candidate = candidates.get(i);
            if (!candidate.isJsonObject()) {
                continue;
            }
            String finishReason = getString(candidate.getAsJsonObject(), "finishReason");
            if ("SAFETY".equalsIgnoreCase(finishReason) || "BLOCKED".equalsIgnoreCase(finishReason)) {
                return finishReason;
            }
        }
        return "";
    }

    private AiServiceException buildBlockedProviderResponseException(String blockReason) {
        return new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI_PROVIDER_BLOCKED",
                "Gemini blocked generation with reason: " + blockReason,
                FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
    }

    private CareerAdviceResponse parseCareerAdvice(String modelText) {
        try {
            CareerAdviceResponse response = objectMapper.readValue(extractLikelyJson(modelText), CareerAdviceResponse.class);
            if (response.recommendedCareers() == null || response.recommendedCareers().isEmpty()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "AI_PROVIDER_RESPONSE_INVALID",
                        "Gemini returned no career recommendations.",
                        "AI provider returned an incomplete guidance response. Please try again shortly.");
            }

            List<CareerAdviceResponse.RecommendedCareer> sanitized = response.recommendedCareers().stream()
                    .map(item -> new CareerAdviceResponse.RecommendedCareer(
                            item.name(),
                            normalizeScore(item.matchScore()),
                            item.reason(),
                            item.improvements() == null ? List.of() : item.improvements()
                    ))
                    .toList();
            log.info("Gemini JSON parsed successfully: recommendations={}", sanitized.size());
            return new CareerAdviceResponse(sanitized);
        } catch (JsonProcessingException ex) {
            log.warn("Gemini JSON parse failure: contentSnippet={}", trim(modelText));
            return fallbackCareerAdvice(modelText);
        }
    }

    private UniversitySourcesAnalysisResponse parseUniversityAdvice(String modelText,
                                                                    List<String> sourceUrls,
                                                                    List<String> successUrls,
                                                                    List<String> failedUrls) {
        try {
            UniversityModelResponse parsed = objectMapper.readValue(extractLikelyJson(modelText), UniversityModelResponse.class);
            return buildUniversityResponse(parsed, sourceUrls, successUrls, failedUrls);
        } catch (JsonProcessingException ex) {
            log.warn("University guidance JSON parse failed: reason={}, contentSnippet={}",
                    ex.getOriginalMessage(), trim(modelText));
            UniversityModelResponse sectioned = parseSectionedUniversityAdvice(modelText);
            if (sectioned != null) {
                sectioned.warnings = appendWarning(sectioned.warnings, NORMALIZED_UNIVERSITY_RESPONSE_WARNING);
                return buildUniversityResponse(sectioned, sourceUrls, successUrls, failedUrls);
            }

            UniversityModelResponse loose = parseLooseUniversityAdvice(modelText, sourceUrls);
            if (loose != null) {
                return buildUniversityResponse(loose, sourceUrls, successUrls, failedUrls);
            }

            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_PROVIDER_RESPONSE_INVALID",
                    "Gemini returned an invalid response payload for university guidance.",
                    FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
        }
    }

    private UniversitySourcesAnalysisResponse buildUniversityResponse(UniversityModelResponse parsed,
                                                                      List<String> sourceUrls,
                                                                      List<String> successUrls,
                                                                      List<String> failedUrls) {
        List<String> minimumRequirements = enforceMinimumRequirements(defaultList(parsed.minimumRequirements), parsed);
        List<String> keyRequirements = mergeKeyAndMinimumRequirements(defaultList(parsed.keyRequirements), minimumRequirements);
        return new UniversitySourcesAnalysisResponse(
                true,
                false,
                "SUCCESS",
                "LIVE",
                resolveGroundingStatus(successUrls, failedUrls, sourceUrls),
                calculateEvidenceCoverage(sourceUrls, successUrls),
                null,
                sourceUrls,
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                sanitizePromptValue(parsed.summary),
                defaultList(parsed.inferredGuidance),
                defaultCareerList(parsed.recommendedCareers),
                defaultProgrammeList(parsed.recommendedProgrammes),
                defaultBursaryList(parsed.bursarySuggestions),
                defaultList(parsed.recommendedUniversities),
                minimumRequirements,
                keyRequirements,
                defaultList(parsed.skillGaps),
                defaultList(parsed.recommendedNextSteps),
                defaultList(parsed.warnings),
                normalizeScore(parsed.suitabilityScore),
                resolveModel()
        );
    }

    private UniversityModelResponse parseSectionedUniversityAdvice(String modelText) {
        if (modelText == null || modelText.isBlank()) {
            return null;
        }

        List<String> headers = List.of(
                "Recommended careers",
                "Recommended programmes",
                "Recommended universities",
                "Skill gaps",
                "Recommended next steps",
                "Next steps",
                "Warnings",
                "Summary"
        );

        String normalized = modelText.replace("\r", "").trim();
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentHeader = null;

        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String header = headers.stream().filter(h -> h.equalsIgnoreCase(trimmed.replace(":", ""))).findFirst().orElse(null);
            if (header != null) {
                currentHeader = header;
                sections.putIfAbsent(header, new ArrayList<>());
                continue;
            }
            if (currentHeader != null) {
                sections.get(currentHeader).add(stripBullet(trimmed));
            }
        }

        if (sections.isEmpty()) {
            return null;
        }

        UniversityModelResponse response = new UniversityModelResponse();
        response.recommendedCareers = sections.getOrDefault("Recommended careers", List.of()).stream()
                .filter(v -> !v.isBlank())
                .map(v -> {
                    UniversityModelResponse.RecommendedCareerPayload payload = new UniversityModelResponse.RecommendedCareerPayload();
                    payload.name = v;
                    payload.reason = "Derived from section-based fallback parsing.";
                    payload.requirements = List.of("Verify subject requirements with the university");
                    payload.relatedProgrammes = List.of();
                    return payload;
                }).toList();
        response.recommendedProgrammes = sections.getOrDefault("Recommended programmes", List.of()).stream()
                .filter(v -> !v.isBlank())
                .map(v -> {
                    UniversityModelResponse.RecommendedProgrammePayload payload = new UniversityModelResponse.RecommendedProgrammePayload();
                    payload.name = v;
                    payload.university = "University Source";
                    payload.admissionRequirements = List.of("Not found in fetched sources");
                    payload.notes = "Verify exact programme requirements from official university programme pages.";
                    return payload;
                }).toList();
        response.recommendedUniversities = sections.getOrDefault("Recommended universities", List.of());
        response.skillGaps = sections.getOrDefault("Skill gaps", List.of());
        response.recommendedNextSteps = sections.containsKey("Recommended next steps")
                ? sections.get("Recommended next steps")
                : sections.getOrDefault("Next steps", List.of());
        response.warnings = sections.getOrDefault("Warnings", List.of());
        response.summary = String.join(" ", sections.getOrDefault("Summary", List.of()));
        response.minimumRequirements = List.of();
        response.keyRequirements = List.of();
        response.inferredGuidance = List.of("Verify all unsupported facts against official university or bursary pages.");
        response.bursarySuggestions = List.of();
        response.suitabilityScore = 60;
        return response;
    }

    private UniversityModelResponse parseLooseUniversityAdvice(String modelText, List<String> sourceUrls) {
        UniversityModelResponse response = new UniversityModelResponse();
        CareerAdviceResponse looseCareerAdvice = fallbackCareerAdvice(modelText);

        response.recommendedCareers = looseCareerAdvice.recommendedCareers().stream()
                .map(item -> {
                    UniversityModelResponse.RecommendedCareerPayload payload = new UniversityModelResponse.RecommendedCareerPayload();
                    payload.name = sanitizeSourceBoundValue(item.name());
                    payload.reason = sanitizeSourceBoundValue(item.reason());
                    payload.requirements = item.improvements() == null || item.improvements().isEmpty()
                            ? List.of("Verify subject and admission requirements on official university pages.")
                            : item.improvements();
                    payload.relatedProgrammes = List.of();
                    return payload;
                })
                .toList();
        response.recommendedProgrammes = List.of();
        response.recommendedUniversities = sourceUrls == null
                ? List.of()
                : sourceUrls.stream()
                .map(this::toUniversityName)
                .filter(name -> name != null && !name.isBlank() && !"University Source".equalsIgnoreCase(name))
                .distinct()
                .toList();
        response.minimumRequirements = List.of();
        response.keyRequirements = List.of();
        response.skillGaps = List.of();
        response.recommendedNextSteps = List.of();
        response.warnings = List.of(NORMALIZED_UNIVERSITY_RESPONSE_WARNING);
        response.summary = trim(stripCodeFences(modelText));
        response.inferredGuidance = List.of();
        response.bursarySuggestions = List.of();
        response.suitabilityScore = 0;
        return response;
    }

    private List<String> appendWarning(List<String> warnings, String warning) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(defaultList(warnings));
        if (warning != null && !warning.isBlank()) {
            merged.add(warning);
        }
        return new ArrayList<>(merged);
    }


    private UniversitySourcesAnalysisResponse enrichWithWarnings(UniversitySourcesAnalysisResponse response,
                                                                 List<String> failedUrls) {
        Set<String> warnings = new LinkedHashSet<>(defaultList(response.warnings()));
        if (!failedUrls.isEmpty()) {
            warnings.add("Some sources failed to load and were skipped.");
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
                response.recommendedCareers(),
                response.recommendedProgrammes(),
                response.bursarySuggestions(),
                response.recommendedUniversities(),
                response.minimumRequirements(),
                response.keyRequirements(),
                response.skillGaps(),
                response.recommendedNextSteps(),
                new ArrayList<>(warnings),
                response.suitabilityScore(),
                response.rawModelUsed()
        );
    }


    private UniversitySourcesAnalysisResponse withRuntimeWarnings(UniversitySourcesAnalysisResponse response,
                                                                  List<String> runtimeWarnings) {
        if (runtimeWarnings == null || runtimeWarnings.isEmpty()) {
            return response;
        }

        LinkedHashSet<String> warnings = new LinkedHashSet<>(defaultList(response.warnings()));
        warnings.addAll(runtimeWarnings);

        return new UniversitySourcesAnalysisResponse(
                true,
                false,
                response.status(),
                response.mode(),
                response.groundingStatus(),
                response.evidenceCoverage(),
                runtimeWarnings.get(0),
                response.requestedSources(),
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
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
                new ArrayList<>(warnings),
                response.suitabilityScore(),
                response.rawModelUsed()
        );
    }

    private List<String> buildFallbackWarnings(List<String> runtimeWarnings, String primaryWarning) {
        LinkedHashSet<String> mergedWarnings = new LinkedHashSet<>();
        if (primaryWarning != null && !primaryWarning.isBlank()) {
            mergedWarnings.add(primaryWarning);
        }
        if (runtimeWarnings != null) {
            runtimeWarnings.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .forEach(mergedWarnings::add);
        }
        return new ArrayList<>(mergedWarnings);
    }

    private UniversitySourcesAnalysisResponse fallbackUniversityResponse(
            UniversitySourcesAnalysisRequest request,
            List<String> sourceUrls,
            List<String> successUrls,
            List<String> failedUrls,
            List<String> warnings
    ) {
        String warningMessage = warnings == null || warnings.isEmpty()
                ? FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE
                : warnings.get(0);
        return new UniversitySourcesAnalysisResponse(
                false,
                false,
                "ERROR",
                "UNAVAILABLE",
                resolveGroundingStatus(successUrls, failedUrls, sourceUrls),
                calculateEvidenceCoverage(sourceUrls, successUrls),
                warningMessage,
                sourceUrls,
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                "Live AI guidance is currently unavailable for this request.",
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
                resolveModel()
        );
    }

    private List<String> enforceMinimumRequirements(List<String> provided,
                                                    UniversityModelResponse parsed) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(defaultMinimumRequirements());
        merged.addAll(provided);
        merged.addAll(defaultList(parsed.keyRequirements).stream()
                .filter(item -> item.toLowerCase().contains("grade 12")
                        || item.toLowerCase().contains("mathematics")
                        || item.toLowerCase().contains("english"))
                .toList());
        return new ArrayList<>(merged);
    }

    private List<String> mergeKeyAndMinimumRequirements(List<String> keyRequirements,
                                                         List<String> minimumRequirements) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(minimumRequirements);
        merged.addAll(keyRequirements);
        return new ArrayList<>(merged);
    }

    private List<String> defaultMinimumRequirements() {
        return List.of(
                "Grade 12 passes are required for university admission pathways.",
                "Mathematics is required for mathematics-related programmes.",
                "English is required for admission and academic communication."
        );
    }

    private String stripBullet(String value) {
        return value.replaceFirst("^[-*•]+\\s*", "").trim();
    }

    private String toUniversityName(String url) {
        String normalized = url == null ? "" : url.toLowerCase();
        if (normalized.contains("unisa")) {
            return "UNISA";
        }
        if (normalized.contains("uj")) {
            return "University of Johannesburg";
        }
        return "University Source";
    }

    private String sanitizePromptValue(String value) {
        if (value == null) {
            return "not provided";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "not provided" : trimmed;
    }

    private String sanitizeSourceBoundValue(String value) {
        if (value == null) {
            return "Not found in fetched sources";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "Not found in fetched sources" : trimmed;
    }

    private Integer normalizeScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String stripCodeFences(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```json", "").replaceFirst("^```", "").trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 250 ? value : value.substring(0, 250) + "...";
    }

    private String extractLikelyJson(String value) {
        String cleaned = stripCodeFences(value);
        if (cleaned == null || cleaned.isBlank()) {
            return "";
        }

        int objectStart = cleaned.indexOf('{');
        int objectEnd = cleaned.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return cleaned.substring(objectStart, objectEnd + 1).trim();
        }

        int arrayStart = cleaned.indexOf('[');
        int arrayEnd = cleaned.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return cleaned.substring(arrayStart, arrayEnd + 1).trim();
        }
        return cleaned.trim();
    }

    private CareerAdviceResponse fallbackCareerAdvice(String modelText) {
        List<String> lines = modelText == null ? List.of() : modelText.lines()
                .map(this::stripBullet)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.toLowerCase().startsWith("recommended careers"))
                .distinct()
                .limit(5)
                .toList();

        List<CareerAdviceResponse.RecommendedCareer> careers = lines.stream()
                .map(line -> new CareerAdviceResponse.RecommendedCareer(
                        line.length() > 70 ? line.substring(0, 70) : line,
                        65,
                        "Derived from the live AI response content.",
                        List.of()
                ))
                .toList();

        return new CareerAdviceResponse(careers.stream()
                .sorted(Comparator.comparing(CareerAdviceResponse.RecommendedCareer::name))
                .toList());
    }

    private String readSnippet(ResponseBody responseBody) throws IOException {
        return responseBody == null ? "" : responseBody.string();
    }

    private String serializeGenerateContentRequest(String prompt) {
        GeminiGenerateContentRequest payload = new GeminiGenerateContentRequest(
                List.of(new GeminiGenerateContentRequest.Content(
                        List.of(new GeminiGenerateContentRequest.Part(prompt)))
                )
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new AiServiceException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI_PROVIDER_REQUEST_SERIALIZATION_FAILED",
                    "Failed to serialize Gemini request payload.",
                    FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
        }
    }

    private ProviderErrorDetails extractProviderErrorDetails(String payload) {
        if (payload == null || payload.isBlank()) {
            return ProviderErrorDetails.empty();
        }
        try {
            JsonElement parsed = JsonParser.parseString(payload);
            if (!parsed.isJsonObject()) {
                return ProviderErrorDetails.empty();
            }
            return extractProviderErrorDetails(parsed.getAsJsonObject());
        } catch (RuntimeException ex) {
            return ProviderErrorDetails.empty();
        }
    }

    private ProviderErrorDetails extractProviderErrorDetails(JsonObject root) {
        JsonObject errorObject = getJsonObject(root, "error");
        if (errorObject == null) {
            return ProviderErrorDetails.empty();
        }
        return new ProviderErrorDetails(
                getString(errorObject, "status"),
                getString(errorObject, "message"),
                getString(errorObject, "code")
        );
    }

    private JsonObject getJsonObject(JsonObject parent, String key) {
        if (parent == null || key == null || !parent.has(key)) {
            return null;
        }
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private JsonArray getJsonArray(JsonObject parent, String key) {
        if (parent == null || key == null || !parent.has(key)) {
            return null;
        }
        JsonElement element = parent.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private String getString(JsonObject parent, String key) {
        if (parent == null || key == null || !parent.has(key)) {
            return "";
        }
        JsonElement element = parent.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (!element.isJsonPrimitive()) {
            return "";
        }
        String value = element.getAsString();
        return value == null ? "" : value.trim();
    }

    private String sanitizeProviderBody(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value
                .replaceAll("AIza[0-9A-Za-z_\\-]{20,}", "[REDACTED_API_KEY]")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        return trim(normalized);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "";
    }

    private String resolveApiKey() {
        String value = geminiProperties.getApiKey();
        return value == null ? "" : value.trim();
    }

    private String resolveConfiguredModelInput() {
        String value = geminiProperties.getModel();
        return value == null ? "" : value.trim();
    }

    private String resolveModel() {
        return GeminiModelResolver.resolveModelName(resolveConfiguredModelInput());
    }

    private String resolveBaseUrl() {
        String configuredBaseUrl = geminiProperties.getBaseUrl();
        String resolved = configuredBaseUrl == null ? "" : configuredBaseUrl.trim();
        if (resolved.isBlank()) {
            resolved = GEMINI_BASE_URL;
        }
        String normalized = GeminiModelResolver.normalizeBaseUrl(resolved);
        return normalized.isBlank() ? GEMINI_BASE_URL : normalized;
    }

    private GeminiRequestConfig resolveRequestConfig(boolean logValues) {
        String configuredModelInput = resolveConfiguredModelInput();
        boolean modelFallbackUsed = GeminiModelResolver.fallsBackToDefault(configuredModelInput);
        String resolvedModel = GeminiModelResolver.resolveModelName(configuredModelInput);
        if (resolvedModel.isBlank()) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_PROVIDER_CONFIGURATION_INVALID",
                    "Gemini model resolved to blank value.",
                    FRIENDLY_PROVIDER_UNAVAILABLE_MESSAGE);
        }
        String resolvedBaseUrl = resolveBaseUrl();
        String endpointPath = GeminiModelResolver.buildGenerateContentPath(configuredModelInput, resolvedBaseUrl);
        String modelInfoPath = GeminiModelResolver.buildModelInfoPath(configuredModelInput, resolvedBaseUrl);
        String resolvedApiKey = resolveApiKey();
        GeminiRequestConfig config = new GeminiRequestConfig(
                resolvedApiKey,
                resolvedModel,
                resolvedBaseUrl,
                endpointPath,
                resolvedBaseUrl + endpointPath,
                resolvedBaseUrl + modelInfoPath
        );
        if (logValues) {
            log.info("Gemini request config: apiKeyPresent={}, model={}, baseUrl={}, endpoint={}, modelFallbackUsed={}",
                    !resolvedApiKey.isBlank(),
                    resolvedModel,
                    resolvedBaseUrl,
                    config.generateEndpoint(),
                    modelFallbackUsed);
            if (modelFallbackUsed) {
                log.warn("Configured Gemini model '{}' is invalid. Using default model={}.",
                        trim(configuredModelInput), DEFAULT_MODEL);
            }
        }
        return config;
    }

    private boolean isRetryableIoFailure(IOException ex) {
        return ex instanceof InterruptedIOException && !(ex instanceof ConnectException);
    }

    private void sleepBeforeRetry(int attempt) {
        long backoff = RETRY_BACKOFF_MS[Math.min(attempt, RETRY_BACKOFF_MS.length - 1)];
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private List<String> defaultList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private List<String> defaultListOrNotFound(List<String> value) {
        if (value == null || value.isEmpty()) {
            return List.of("Not found in fetched sources");
        }
        return value;
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedCareer> defaultCareerList(
            List<UniversityModelResponse.RecommendedCareerPayload> value) {
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .filter(item -> item != null && item.name != null && !item.name.isBlank())
                .map(item -> new UniversitySourcesAnalysisResponse.RecommendedCareer(
                        item.name,
                        sanitizeSourceBoundValue(item.reason),
                        defaultListOrNotFound(item.requirements),
                        defaultList(item.relatedProgrammes)
                ))
                .toList();
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedProgramme> defaultProgrammeList(
            List<UniversityModelResponse.RecommendedProgrammePayload> value) {
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .filter(item -> item != null && item.name != null && !item.name.isBlank())
                .map(item -> new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                        item.name,
                        sanitizeSourceBoundValue(item.university),
                        defaultListOrNotFound(item.admissionRequirements),
                        sanitizeSourceBoundValue(item.notes)
                ))
                .toList();
    }



    public record GeminiHealthCheck(
            boolean apiKeyPresent,
            boolean endpointReachable,
            String model,
            String endpoint,
            String message
    ) {
    }

    private String resolveGroundingStatus(List<String> successUrls, List<String> failedUrls, List<String> sourceUrls) {
        if (sourceUrls == null || sourceUrls.isEmpty()) {
            return "NO_LIVE_SOURCES";
        }
        if (successUrls != null && !successUrls.isEmpty() && (failedUrls == null || failedUrls.isEmpty())) {
            return "FULLY_GROUNDED";
        }
        if (successUrls != null && !successUrls.isEmpty()) {
            return "PARTIALLY_GROUNDED";
        }
        return "INSUFFICIENT_EVIDENCE";
    }

    private int calculateEvidenceCoverage(List<String> sourceUrls, List<String> successUrls) {
        if (sourceUrls == null || sourceUrls.isEmpty()) {
            return 0;
        }
        return (int) Math.round((successUrls == null ? 0D : (100.0 * successUrls.size() / sourceUrls.size())));
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedBursary> defaultBursaryList(
            List<UniversityModelResponse.RecommendedBursaryPayload> value) {
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .filter(item -> item != null && item.name != null && !item.name.isBlank())
                .map(item -> new UniversitySourcesAnalysisResponse.RecommendedBursary(
                        item.name,
                        sanitizeSourceBoundValue(item.provider),
                        defaultListOrNotFound(item.eligibility),
                        sanitizeSourceBoundValue(item.notes),
                        defaultList(item.sourceUrls),
                        item.officialSource
                ))
                .toList();
    }

    private static class UniversityModelResponse {
        public String summary;
        public List<RecommendedCareerPayload> recommendedCareers;
        public List<RecommendedProgrammePayload> recommendedProgrammes;
        public List<String> recommendedUniversities;
        public List<String> minimumRequirements;
        public List<String> keyRequirements;
        public List<String> skillGaps;
        public List<String> recommendedNextSteps;
        public List<String> warnings;
        public List<String> inferredGuidance;
        public List<RecommendedBursaryPayload> bursarySuggestions;
        public Integer suitabilityScore;

        private static class RecommendedCareerPayload {
            public String name;
            public String reason;
            public List<String> requirements;
            public List<String> relatedProgrammes;
        }

        private static class RecommendedProgrammePayload {
            public String name;
            public String university;
            public List<String> admissionRequirements;
            public String notes;
        }

        private static class RecommendedBursaryPayload {
            public String name;
            public String provider;
            public List<String> eligibility;
            public String notes;
            public List<String> sourceUrls;
            public boolean officialSource;
        }
    }

    private record ProviderErrorDetails(
            String status,
            String message,
            String code
    ) {
        private static ProviderErrorDetails empty() {
            return new ProviderErrorDetails("", "", "");
        }

        private boolean hasError() {
            return !(status == null || status.isBlank())
                    || !(message == null || message.isBlank())
                    || !(code == null || code.isBlank());
        }

        private String toMessage() {
            if (message != null && !message.isBlank()) {
                return message.trim();
            }
            if (status != null && !status.isBlank()) {
                return status.trim();
            }
            if (code != null && !code.isBlank()) {
                return code.trim();
            }
            return "";
        }
    }

    private record GeminiGenerateContentRequest(
            List<Content> contents
    ) {
        private record Content(
                List<Part> parts
        ) {
        }

        private record Part(
                String text
        ) {
        }
    }

    private record GeminiRequestConfig(
            String apiKey,
            String model,
            String baseUrl,
            String endpointPath,
            String generateEndpoint,
            String modelInfoEndpoint
    ) {
    }
}

