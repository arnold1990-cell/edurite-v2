package com.edurite.auth.service;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class LoggingSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsService.class);
    private final Environment environment;

    public LoggingSmsService(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void sendOtp(String mobileNumber, String otp) {
        boolean developmentMode = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("dev")
                        || profile.equalsIgnoreCase("test")
                        || profile.equalsIgnoreCase("local"));
        if (developmentMode) {
            log.info("[sms-dev] school password recovery OTP sent to {} otp={}", mobileNumber, otp);
        } else {
            log.warn("[sms] SMS provider is not configured. OTP dispatch requested for {}.", mobileNumber);
        }
    }
}
