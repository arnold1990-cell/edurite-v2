package com.edurite.auth.service;

import com.edurite.user.entity.*;
import com.edurite.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class EmailVerificationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final SesV2Client ses = mock(SesV2Client.class);
    private final EmailVerificationService service = new EmailVerificationService(
            users, ses, "https://edurite.co.za", "info@edurite.co.za");

    @Test void sendsHashedSingleUseLinkAndPersistsOnlyAfterAcceptance() {
        User user = pending();
        when(ses.sendEmail(any(SendEmailRequest.class))).thenAnswer(invocation -> {
            assertThat(user.getEmailVerificationHash()).isNull();
            assertThat(user.getEmailVerificationSentAt()).isNull();
            assertThat(user.getEmailVerificationExpiresAt()).isNull();
            verify(users, never()).save(any());
            return SendEmailResponse.builder().messageId("accepted").build();
        });
        assertThat(service.send(user)).isTrue();
        var request = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(ses).sendEmail(request.capture());
        assertThat(request.getValue().fromEmailAddress()).isEqualTo("info@edurite.co.za");
        assertThat(request.getValue().destination().toAddresses()).containsExactly(user.getEmail());
        String text = request.getValue().content().simple().body().text().data();
        assertThat(text).contains("https://edurite.co.za/verify-email?");
        String token = text.split("token=")[1].split("\\s")[0];
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
        assertThat(user.getEmailVerificationHash()).isEqualTo(EmailVerificationService.hash(token)).hasSize(64).isNotEqualTo(token);
        assertThat(user.getEmailVerificationExpiresAt()).isEqualTo(user.getEmailVerificationSentAt().plusMinutes(30));
        verify(users).save(user);
        service.verify(user.getEmail(), token);
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.isEmailVerificationRequired()).isFalse();
        assertThat(user.getEmailVerificationHash()).isNull();
        assertThat(user.getEmailVerificationExpiresAt()).isNull();
        assertThatThrownBy(() -> service.verify(user.getEmail(), token)).hasMessageContaining("Invalid or expired");
    }

    @Test void sesFailurePreservesExistingHashAndTimestamps() {
        User user = pending();
        OffsetDateTime previous = OffsetDateTime.now().minusMinutes(2);
        user.setEmailVerificationHash("previous-hash");
        user.setEmailVerificationSentAt(previous);
        user.setEmailVerificationExpiresAt(previous.plusMinutes(30));
        when(ses.sendEmail(any(SendEmailRequest.class))).thenThrow(SesV2Exception.builder().statusCode(403).build());
        assertThatThrownBy(() -> service.resend(user.getEmail())).hasMessageContaining("could not be sent");
        assertThat(user.getEmailVerificationHash()).isEqualTo("previous-hash");
        assertThat(user.getEmailVerificationSentAt()).isEqualTo(previous);
        assertThat(user.getEmailVerificationExpiresAt()).isEqualTo(previous.plusMinutes(30));
        verify(users, never()).save(any());
    }

    @Test void credentialOrNetworkFailureDoesNotPersistNewToken() {
        User user = pending();
        when(ses.sendEmail(any(SendEmailRequest.class))).thenThrow(SdkClientException.create("sensitive details"));
        assertThat(service.send(user)).isFalse();
        assertThat(user.getEmailVerificationHash()).isNull();
        assertThat(user.getEmailVerificationSentAt()).isNull();
        assertThat(user.getEmailVerificationExpiresAt()).isNull();
        verify(users, never()).save(any());
    }

    @Test void throttlesResendWithinOneMinuteAndAllowsLaterResend() {
        User user = pending();
        user.setEmailVerificationSentAt(OffsetDateTime.now().minusSeconds(20));
        service.resend(user.getEmail());
        verifyNoInteractions(ses);
        verify(users, never()).save(any());
        user.setEmailVerificationSentAt(OffsetDateTime.now().minusSeconds(61));
        service.resend(user.getEmail());
        verify(ses).sendEmail(any(SendEmailRequest.class));
        verify(users).save(user);
    }

    @Test void rejectsExpiredInvalidAndUnknownTokens() {
        User user = pending();
        user.setEmailVerificationHash(EmailVerificationService.hash("secret"));
        user.setEmailVerificationExpiresAt(OffsetDateTime.now().minusMinutes(1));
        assertThatThrownBy(() -> service.verify(user.getEmail(), "secret")).hasMessageContaining("expired");
        user.setEmailVerificationExpiresAt(OffsetDateTime.now().plusMinutes(30));
        assertThatThrownBy(() -> service.verify(user.getEmail(), "wrong")).hasMessageContaining("Invalid");
        assertThatThrownBy(() -> service.verify("missing@example.com", "secret")).hasMessageContaining("Invalid");
        assertThat(user.isEmailVerified()).isFalse();
        verify(users, never()).save(any());
    }

    private User pending() {
        User user = new User();
        user.setEmail("student@example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerificationRequired(true);
        when(users.lockForEmailVerification(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }
}
