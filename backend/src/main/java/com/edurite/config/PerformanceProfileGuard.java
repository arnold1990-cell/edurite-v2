package com.edurite.config;

import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("performance")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PerformanceProfileGuard implements ApplicationRunner {

    private final Environment environment;

    public PerformanceProfileGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prodActive = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
        if (prodActive) {
            throw new IllegalStateException("The performance profile cannot be active with the prod profile.");
        }
    }
}
