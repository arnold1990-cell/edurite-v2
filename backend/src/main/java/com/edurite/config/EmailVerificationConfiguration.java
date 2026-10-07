package com.edurite.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
public class EmailVerificationConfiguration {
    @Bean(destroyMethod = "close")
    public DefaultCredentialsProvider emailVerificationCredentialsProvider() {
        return DefaultCredentialsProvider.builder().build();
    }

    @Bean(destroyMethod = "close")
    public SesV2Client emailVerificationSesClient(
            @Value("${app.email-verification.aws-region:af-south-1}") String region,
            DefaultCredentialsProvider emailVerificationCredentialsProvider) {
        return SesV2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(emailVerificationCredentialsProvider)
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(20))
                        .apiCallAttemptTimeout(Duration.ofSeconds(10)))
                .build();
    }
}
