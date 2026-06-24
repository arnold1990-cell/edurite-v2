package com.edurite.auth.service;

import com.edurite.auth.config.TwilioVerifyProperties;
import com.edurite.auth.exception.OtpDispatchException;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class TwilioOtpService implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(TwilioOtpService.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE = new ParameterizedTypeReference<>() {
    };

    private final RestClient twilioVerifyRestClient;
    private final TwilioVerifyProperties twilioVerifyProperties;

    public TwilioOtpService(
            @Qualifier("twilioVerifyRestClient") RestClient twilioVerifyRestClient,
            TwilioVerifyProperties twilioVerifyProperties
    ) {
        this.twilioVerifyRestClient = twilioVerifyRestClient;
        this.twilioVerifyProperties = twilioVerifyProperties;
    }

    @PostConstruct
    void logConfigurationStatus() {
        boolean configured = !isBlank(twilioVerifyProperties.accountSid())
                && !isBlank(twilioVerifyProperties.authToken())
                && !isBlank(twilioVerifyProperties.verifyServiceSid());
        log.info(
                "[otp] twilio verify configuration loaded configured={} accountSidPresent={} authTokenPresent={} verifyServiceSidPresent={} baseUrl={}",
                configured,
                !isBlank(twilioVerifyProperties.accountSid()),
                !isBlank(twilioVerifyProperties.authToken()),
                !isBlank(twilioVerifyProperties.verifyServiceSid()),
                isBlank(twilioVerifyProperties.baseUrl()) ? "https://verify.twilio.com/v2" : twilioVerifyProperties.baseUrl().trim()
        );
    }

    @Override
    public void sendVerificationOtp(String phoneNumber) {
        sendOtp(phoneNumber);
    }

    @Override
    public void sendPasswordResetOtp(String phoneNumber) {
        sendOtp(phoneNumber);
    }

    @Override
    public boolean verifyVerificationOtp(String phoneNumber, String code) {
        return verifyOtp(phoneNumber, code);
    }

    @Override
    public boolean verifyPasswordResetOtp(String phoneNumber, String code) {
        return verifyOtp(phoneNumber, code);
    }

    private void sendOtp(String phoneNumber) {
        validateConfiguration();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", phoneNumber);
        form.add("Channel", "sms");
        try {
            log.info("[otp] sending OTP to {} via Twilio Verify serviceSid={}", phoneNumber, twilioVerifyProperties.verifyServiceSid());
            Map<String, Object> body = twilioVerifyRestClient.post()
                    .uri("/Services/{serviceSid}/Verifications", twilioVerifyProperties.verifyServiceSid())
                    .header(HttpHeaders.AUTHORIZATION, basicAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP_RESPONSE);
            Object status = body == null ? null : body.get("status");
            log.info("[otp] twilio response status={} phone={}", status, phoneNumber);
        } catch (RestClientResponseException ex) {
            log.error("[otp] twilio verify otp dispatch failed phone={} status={} response={}", phoneNumber, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new OtpDispatchException("Unable to send OTP at the moment. Please try again.");
        } catch (RuntimeException ex) {
            log.error("[otp] failed to send OTP phone={} reason={}", phoneNumber, ex.getMessage(), ex);
            throw new OtpDispatchException("Unable to send OTP at the moment. Please try again.");
        }
    }

    private boolean verifyOtp(String phoneNumber, String code) {
        validateConfiguration();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", phoneNumber);
        form.add("Code", code);
        try {
            Map<String, Object> body = twilioVerifyRestClient.post()
                    .uri("/Services/{serviceSid}/VerificationCheck", twilioVerifyProperties.verifyServiceSid())
                    .header(HttpHeaders.AUTHORIZATION, basicAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP_RESPONSE);
            return isApproved(body);
        } catch (HttpClientErrorException ex) {
            log.warn("[otp] twilio verify otp check rejected phone={} status={} response={}", phoneNumber, ex.getStatusCode(), ex.getResponseBodyAsString());
            return false;
        } catch (RestClientResponseException ex) {
            log.error("[otp] twilio verify otp check failed phone={} status={} response={}", phoneNumber, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new OtpDispatchException("Unable to validate OTP right now. Please try again.");
        } catch (RuntimeException ex) {
            log.error("[otp] twilio verify otp check failed phone={} reason={}", phoneNumber, ex.getMessage(), ex);
            throw new OtpDispatchException("Unable to validate OTP right now. Please try again.");
        }
    }

    private boolean isApproved(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return false;
        }
        Object status = body.get("status");
        if (status instanceof String statusValue && "approved".equalsIgnoreCase(statusValue)) {
            return true;
        }
        Object valid = body.get("valid");
        return valid instanceof Boolean validValue && validValue;
    }

    private void validateConfiguration() {
        if (isBlank(twilioVerifyProperties.accountSid())
                || isBlank(twilioVerifyProperties.authToken())
                || isBlank(twilioVerifyProperties.verifyServiceSid())) {
            throw new OtpDispatchException("Twilio Verify is not configured. Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_VERIFY_SERVICE_SID.");
        }
    }

    private String basicAuthorizationHeader() {
        String username = twilioVerifyProperties.accountSid().trim();
        String password = twilioVerifyProperties.authToken().trim();
        String token = java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

