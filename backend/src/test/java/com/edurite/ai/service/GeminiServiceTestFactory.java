package com.edurite.ai.service;

import com.edurite.ai.config.GeminiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

final class GeminiServiceTestFactory {

    private GeminiServiceTestFactory() {
    }

    static GeminiService service() {
        return service("test-gemini-api-key", "gemini-2.5-flash", "https://generativelanguage.googleapis.com");
    }

    static GeminiService service(String apiKey) {
        return service(apiKey, "gemini-2.5-flash", "https://generativelanguage.googleapis.com");
    }

    static GeminiService service(String apiKey, String model, String baseUrl) {
        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey(apiKey);
        properties.setModel(model);
        properties.setBaseUrl(baseUrl);
        return new GeminiService(new ObjectMapper(), properties, null);
    }
}

