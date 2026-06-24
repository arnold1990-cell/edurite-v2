package com.edurite.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TwilioConfig {

    @Bean(name = "twilioVerifyRestClient")
    RestClient twilioVerifyRestClient(RestClient.Builder builder, TwilioVerifyProperties properties) {
        String baseUrl = properties.baseUrl() == null || properties.baseUrl().isBlank()
                ? "https://verify.twilio.com/v2"
                : properties.baseUrl().trim();
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}

