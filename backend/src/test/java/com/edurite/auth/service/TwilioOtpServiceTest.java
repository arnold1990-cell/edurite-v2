package com.edurite.auth.service;

import com.edurite.auth.config.TwilioVerifyProperties;
import com.edurite.auth.exception.OtpDispatchException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TwilioOtpServiceTest {

    @Test
    void sendVerificationOtpThrowsClearErrorWhenConfigMissing() {
        RestClient restClient = mock(RestClient.class);
        TwilioOtpService service = new TwilioOtpService(restClient, new TwilioVerifyProperties("", "", "", null));

        assertThatThrownBy(() -> service.sendVerificationOtp("+26775314557"))
                .isInstanceOf(OtpDispatchException.class)
                .hasMessageContaining("Twilio Verify is not configured");
    }
}

