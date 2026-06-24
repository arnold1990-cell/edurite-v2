package com.edurite;

import com.edurite.ai.config.GeminiProperties;
import com.edurite.auth.config.AdminSeedProperties;
import com.edurite.auth.config.AuthOtpProperties;
import com.edurite.auth.config.GoogleSignInProperties;
import com.edurite.auth.config.TwilioVerifyProperties;
import com.edurite.subscription.payment.PayPalPaymentProperties;
import com.edurite.subscription.payment.PayFastConfig;
import com.edurite.subscription.payment.PaymentGatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        GeminiProperties.class,
        AuthOtpProperties.class,
        TwilioVerifyProperties.class,
        GoogleSignInProperties.class,
        AdminSeedProperties.class,
        PaymentGatewayProperties.class,
        PayPalPaymentProperties.class,
        PayFastConfig.class
})
@EnableScheduling
/**
 * This class named EduRiteApplication is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class EduRiteApplication {

    /**
     * this method handles the "main" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public static void main(String[] args) {
        SpringApplication.run(EduRiteApplication.class, args);
    }
}

