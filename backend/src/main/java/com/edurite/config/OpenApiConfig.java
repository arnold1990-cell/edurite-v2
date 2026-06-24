package com.edurite.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @Configuration marks a class that defines Spring beans and setup.
@Configuration
/**
 * This class named OpenApiConfig is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class OpenApiConfig {

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    OpenAPI eduriteOpenApi() {
        return new OpenAPI().info(new Info().title("EduRite API").version("v1").description("EduRite MVP API"));
    }
}

