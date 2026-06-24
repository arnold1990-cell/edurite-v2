package com.edurite.ai.service;

final class GeminiModelResolver {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String DEFAULT_API_VERSION = "v1beta";

    private GeminiModelResolver() {
    }

    static String resolveModelName(String configuredModel) {
        String normalized = normalizeConfiguredModel(configuredModel);
        if (normalized.isEmpty()) {
            return DEFAULT_MODEL;
        }
        return isLikelyGeminiModel(normalized) ? normalized : DEFAULT_MODEL;
    }

    static boolean fallsBackToDefault(String configuredModel) {
        String normalized = normalizeConfiguredModel(configuredModel);
        return !normalized.isEmpty() && !isLikelyGeminiModel(normalized);
    }

    static String resolveApiVersion(String configuredModel, String configuredBaseUrl) {
        String fromModel = extractApiVersion(configuredModel);
        if (!fromModel.isBlank()) {
            return fromModel;
        }
        String fromBaseUrl = extractApiVersion(configuredBaseUrl);
        if (!fromBaseUrl.isBlank()) {
            return fromBaseUrl;
        }
        return DEFAULT_API_VERSION;
    }

    static String normalizeBaseUrl(String configuredBaseUrl) {
        if (configuredBaseUrl == null) {
            return "";
        }

        String normalized = configuredBaseUrl.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        int modelsIndex = normalized.indexOf("/models/");
        if (modelsIndex > 0) {
            normalized = normalized.substring(0, modelsIndex).trim();
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1).trim();
            }
        }

        normalized = stripVersionSuffix(normalized, "/v1beta");
        normalized = stripVersionSuffix(normalized, "/v1");

        return normalized;
    }

    static String buildGenerateContentPath(String configuredModel, String configuredBaseUrl) {
        String apiVersion = resolveApiVersion(configuredModel, configuredBaseUrl);
        return "/" + apiVersion + "/models/" + resolveModelName(configuredModel) + ":generateContent";
    }

    static String buildModelInfoPath(String configuredModel, String configuredBaseUrl) {
        String apiVersion = resolveApiVersion(configuredModel, configuredBaseUrl);
        return "/" + apiVersion + "/models/" + resolveModelName(configuredModel);
    }

    static String buildGenerateContentPath(String configuredModel) {
        return buildGenerateContentPath(configuredModel, null);
    }

    private static String normalizeConfiguredModel(String configuredModel) {
        if (configuredModel == null) {
            return "";
        }

        String normalized = configuredModel.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }

        if (normalized.startsWith("https://") || normalized.startsWith("http://")) {
            int modelsIndex = normalized.indexOf("/models/");
            if (modelsIndex >= 0) {
                normalized = normalized.substring(modelsIndex + 1).trim();
            }
        }

        if (normalized.startsWith("v1beta/")) {
            normalized = normalized.substring("v1beta/".length()).trim();
        }

        if (normalized.startsWith("v1/")) {
            normalized = normalized.substring("v1/".length()).trim();
        }

        while (normalized.startsWith("models/")) {
            normalized = normalized.substring("models/".length()).trim();
        }

        int queryStart = normalized.indexOf('?');
        if (queryStart >= 0) {
            normalized = normalized.substring(0, queryStart).trim();
        }

        int actionStart = normalized.indexOf(':');
        if (actionStart >= 0) {
            normalized = normalized.substring(0, actionStart).trim();
        }

        while (normalized.startsWith("models/")) {
            normalized = normalized.substring("models/".length()).trim();
        }

        return normalized;
    }

    private static boolean isLikelyGeminiModel(String normalizedModel) {
        if (normalizedModel == null || normalizedModel.isBlank()) {
            return false;
        }
        if (!normalizedModel.toLowerCase().startsWith("gemini-")) {
            return false;
        }
        return normalizedModel.matches("^[A-Za-z0-9._-]+$");
    }

    private static String extractApiVersion(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.contains("/v1beta/") || normalized.startsWith("v1beta/") || normalized.endsWith("/v1beta")) {
            return "v1beta";
        }
        if (normalized.contains("/v1/") || normalized.startsWith("v1/") || normalized.endsWith("/v1")) {
            return "v1";
        }
        return "";
    }

    private static String stripVersionSuffix(String value, String versionSuffix) {
        String normalized = value;
        while (normalized.toLowerCase().endsWith(versionSuffix)) {
            normalized = normalized.substring(0, normalized.length() - versionSuffix.length()).trim();
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1).trim();
            }
        }
        return normalized;
    }
}

