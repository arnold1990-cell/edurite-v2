package com.edurite.ai.service;

import com.edurite.ai.exception.AiServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService implements AiProviderService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private static final MediaType JSON = MediaType.get("application/json");
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String FRIENDLY_FAILURE_MESSAGE = "AI guidance is temporarily unavailable. Please try again later.";
    private static final long[] RETRY_BACKOFF_MS = {300L, 800L};

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${openai.max-retries:1}")
    private int maxRetries;

    public OpenAiService(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(environment.getProperty("openai.connect-timeout-seconds", Integer.class, 15)))
                .readTimeout(Duration.ofSeconds(environment.getProperty("openai.read-timeout-seconds", Integer.class, 45)))
                .writeTimeout(Duration.ofSeconds(environment.getProperty("openai.write-timeout-seconds", Integer.class, 45)))
                .callTimeout(Duration.ofSeconds(environment.getProperty("openai.call-timeout-seconds", Integer.class, 60)))
                .build();
    }

    @Override
    public String generateContent(String prompt) {
        String resolvedKey = resolveApiKey();
        if (resolvedKey.isBlank()) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_PROVIDER_CONFIGURATION_INVALID",
                    "OpenAI API key is missing.",
                    FRIENDLY_FAILURE_MESSAGE);
        }

        String resolvedModel = resolveModel();
        if (resolvedModel.isBlank()) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_PROVIDER_CONFIGURATION_INVALID",
                    "OpenAI model is blank.",
                    FRIENDLY_FAILURE_MESSAGE);
        }

        String endpoint = resolveBaseUrl() + "/v1/chat/completions";
        String payload = serializePayload(resolvedModel, prompt);

        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + resolvedKey)
                .post(RequestBody.create(payload, JSON))
                .build();

        for (int attempt = 0; attempt <= Math.max(0, maxRetries); attempt++) {
            boolean retry = attempt < Math.max(0, maxRetries);
            try (Response response = okHttpClient.newCall(request).execute()) {
                int status = response.code();
                String body = readSnippet(response.body());
                if (response.isSuccessful()) {
                    return extractContent(body);
                }

                boolean retriableStatus = status == 408 || status == 429 || status >= 500;
                log.warn("OpenAI request failed: status={}, attempt={}, retryable={}, body={}",
                        status, attempt + 1, retriableStatus, sanitize(body));
                if (retry && retriableStatus) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw mapError(status, body);
            } catch (IOException ex) {
                boolean retryable = retry && isRetryableIoFailure(ex);
                log.warn("OpenAI IO failure: attempt={}, retryable={}, message={}",
                        attempt + 1, retryable, sanitize(ex.getMessage()), ex);
                if (retryable) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw new AiServiceException(HttpStatus.GATEWAY_TIMEOUT,
                        "AI_PROVIDER_TIMEOUT",
                        "OpenAI request timed out or failed.",
                        FRIENDLY_FAILURE_MESSAGE);
            }
        }

        throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI_PROVIDER_UNAVAILABLE",
                "OpenAI request failed after retries.",
                FRIENDLY_FAILURE_MESSAGE);
    }

    private String resolveApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    private String resolveModel() {
        String value = model == null ? "" : model.trim();
        return value.isBlank() ? DEFAULT_MODEL : value;
    }

    private String resolveBaseUrl() {
        String value = baseUrl == null ? "" : baseUrl.trim();
        if (value.isBlank()) {
            value = DEFAULT_BASE_URL;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }

    private String serializePayload(String resolvedModel, String prompt) {
        Map<String, Object> payload = Map.of(
                "model", resolvedModel,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new AiServiceException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI_PROVIDER_REQUEST_SERIALIZATION_FAILED",
                    "Failed to serialize OpenAI request payload.",
                    FRIENDLY_FAILURE_MESSAGE);
        }
    }

    private String extractContent(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "AI_PROVIDER_RESPONSE_INVALID",
                        "OpenAI returned empty choices/message content.",
                        FRIENDLY_FAILURE_MESSAGE);
            }
            return content.asText();
        } catch (JsonProcessingException ex) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "AI_PROVIDER_RESPONSE_INVALID",
                    "OpenAI returned malformed JSON.",
                    FRIENDLY_FAILURE_MESSAGE);
        }
    }

    private AiServiceException mapError(int status, String body) {
        String detail = "OpenAI request failed with status " + status + ". " + sanitize(body);
        if (status == 401 || status == 403) {
            return new AiServiceException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_AUTH", detail, FRIENDLY_FAILURE_MESSAGE);
        }
        if (status == 400 || status == 404) {
            return new AiServiceException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_CONFIGURATION_INVALID", detail, FRIENDLY_FAILURE_MESSAGE);
        }
        if (status == 408 || status == 429 || status >= 500) {
            return new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "AI_PROVIDER_UNAVAILABLE", detail, FRIENDLY_FAILURE_MESSAGE);
        }
        return new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "AI_PROVIDER_REQUEST_FAILED", detail, FRIENDLY_FAILURE_MESSAGE);
    }

    private String readSnippet(ResponseBody body) throws IOException {
        return body == null ? "" : body.string();
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

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value
                .replaceAll("sk-[A-Za-z0-9_-]{20,}", "[REDACTED_API_KEY]")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }
}


