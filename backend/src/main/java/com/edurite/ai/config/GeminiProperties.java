package com.edurite.ai.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String apiKey = "";

    @NotBlank(message = "Gemini model must not be blank.")
    private String model = "gemini-2.5-flash";

    @NotBlank(message = "Gemini base URL must not be blank.")
    private String baseUrl = "https://generativelanguage.googleapis.com";

    @Positive(message = "gemini.connect-timeout-seconds must be greater than 0.")
    private int connectTimeoutSeconds = 20;

    @Positive(message = "gemini.read-timeout-seconds must be greater than 0.")
    private int readTimeoutSeconds = 45;

    @Positive(message = "gemini.write-timeout-seconds must be greater than 0.")
    private int writeTimeoutSeconds = 45;

    @Positive(message = "gemini.call-timeout-seconds must be greater than 0.")
    private int callTimeoutSeconds = 60;

    @Min(value = 0, message = "gemini.max-retries must be 0 or greater.")
    private int maxRetries = 2;

    private boolean startupHealthCheckEnabled = false;
}

