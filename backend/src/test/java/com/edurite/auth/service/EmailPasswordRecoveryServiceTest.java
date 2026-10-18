package com.edurite.auth.service;

import com.edurite.auth.entity.EmailPasswordReset;
import com.edurite.auth.exception.InvalidOtpException;
import com.edurite.auth.repository.EmailPasswordResetRepository;
import com.edurite.user.entity.*;
import com.edurite.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class EmailPasswordRecoveryServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final EmailPasswordResetRepository resets = mock(EmailPasswordResetRepository.class);
    private final SesV2Client ses = mock(SesV2Client.class);
    private final PasswordEncoder encoder = spy(new BCryptPasswordEncoder(4));
    private final EmailPasswordRecoveryService service = new EmailPasswordRecoveryService(users, resets, encoder, ses, "info@edurite.co.za");
    private final User user = new User();
    private EmailPasswordReset record;

    private void eligible() {
        user.setId(UUID.randomUUID()); user.setEmail("student@example.com");
        user.setStatus(UserStatus.ACTIVE); user.setPasswordHash(encoder.encode("Existing@123"));
        user.setEmailVerified(true);
        Role role = new Role(); role.setName("ROLE_STUDENT"); user.getRoles().add(role);
        when(users.lockForEmailVerification(user.getEmail())).thenReturn(Optional.of(user));
        when(resets.findByUserId(user.getId())).thenAnswer(i -> Optional.ofNullable(record));
        when(resets.save(any())).thenAnswer(i -> { record = i.getArgument(0); return record; });
    }
    private String request() {
        service.requestOtp(" Student@Example.com ");
        var captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(ses, atLeastOnce()).sendEmail(captor.capture());
        SendEmailRequest request = captor.getValue();
        assertThat(request.fromEmailAddress()).isEqualTo("info@edurite.co.za");
        assertThat(request.destination().toAddresses()).containsExactly(user.getEmail());
        String code = request.content().simple().body().text().data().split("code is: ")[1].substring(0, 6);
        assertThat(code).matches("[0-9]{6}");
        assertThat(record.getOtpHash()).isNotEqualTo(code);
        assertThat(encoder.matches(code, record.getOtpHash())).isTrue();
        return code;
    }
    @Test void missingPhoneCanResetWithSingleUseOtpAndTokenPreservingRolesAndVerification() {
        eligible(); String originalHash = user.getPasswordHash();
        assertThat(user.getPhoneNumber()).isNull();
        String code = request();
        assertThat(record.getExpiresAt()).isBetween(OffsetDateTime.now().plusMinutes(9), OffsetDateTime.now().plusMinutes(11));
        String token = service.verify(user.getEmail(), code);
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
        assertThat(record.getResetTokenHash()).isEqualTo(EmailVerificationService.hash(token));
        assertThat(record.getOtpHash()).isNull();
        assertThatThrownBy(() -> service.verify(user.getEmail(), code)).isInstanceOf(InvalidOtpException.class);
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        service.reset(user.getEmail(), token, "Changed@123");
        assertThat(encoder.matches("Changed@123", user.getPasswordHash())).isTrue();
        assertThat(encoder.matches("Existing@123", user.getPasswordHash())).isFalse();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getRoles()).extracting(Role::getName).containsExactly("ROLE_STUDENT");
        assertThatThrownBy(() -> service.reset(user.getEmail(), token, "Other@123")).isInstanceOf(InvalidOtpException.class);
    }
    @Test void unknownAndInactiveAccountsDoNotSendOrChangePasswords() {
        service.requestOtp("unknown@example.com"); verifyNoInteractions(resets, ses);
        eligible(); user.setStatus(UserStatus.SUSPENDED);
        service.requestOtp(user.getEmail()); verifyNoInteractions(resets, ses);
        assertThatThrownBy(() -> service.verify("unknown@example.com", "123456")).isInstanceOf(InvalidOtpException.class);
    }
    @Test void rejectsExpiredCodeAndExpiredResetSession() {
        eligible(); String code = request(); record.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.verify(user.getEmail(), code)).isInstanceOf(InvalidOtpException.class);
        record.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        String token = service.verify(user.getEmail(), code);
        record.setResetTokenExpiresAt(OffsetDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.reset(user.getEmail(), token, "Changed@123")).isInstanceOf(InvalidOtpException.class);
        verify(users, never()).save(any());
    }
    @Test void fiveWrongCodesLockOutEvenTheCorrectCode() {
        eligible(); String code = request(); String wrong = code.equals("000000") ? "111111" : "000000";
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatThrownBy(() -> service.verify(user.getEmail(), wrong)).isInstanceOf(InvalidOtpException.class);
            assertThat(record.getAttempts()).isEqualTo(attempt);
        }
        assertThatThrownBy(() -> service.verify(user.getEmail(), code)).isInstanceOf(InvalidOtpException.class);
        assertThat(record.getOtpHash()).isNull(); verify(users, never()).save(any());
    }
    @Test void resendInvalidatesPreviousCodeAndResetSessionAndThrottlesRequests() {
        eligible(); String code = request(); String token = service.verify(user.getEmail(), code);
        service.requestOtp(user.getEmail()); verify(ses, times(1)).sendEmail(any(SendEmailRequest.class));
        record.setRequestedAt(OffsetDateTime.now().minusSeconds(61));
        request(); verify(ses, times(2)).sendEmail(any(SendEmailRequest.class));
        assertThat(record.getResetTokenHash()).isNull();
        assertThatThrownBy(() -> service.reset(user.getEmail(), token, "Changed@123")).isInstanceOf(InvalidOtpException.class);
        record.setRequestCount(5); record.setRequestedAt(OffsetDateTime.now().minusSeconds(61));
        service.requestOtp(user.getEmail()); verify(ses, times(2)).sendEmail(any(SendEmailRequest.class));
        record.setWindowStartedAt(OffsetDateTime.now().minusHours(2));
        service.requestOtp(user.getEmail()); verify(ses, times(3)).sendEmail(any(SendEmailRequest.class));
    }
    @Test void deliveryFailuresKeepPreviousCodeAndPersistThrottleWithoutChangingPassword() {
        eligible(); String code = request(); String hash = record.getOtpHash();
        record.setRequestedAt(OffsetDateTime.now().minusSeconds(61));
        when(ses.sendEmail(any(SendEmailRequest.class))).thenThrow(SesV2Exception.builder().statusCode(503).build());
        service.requestOtp(user.getEmail());
        assertThat(record.getOtpHash()).isEqualTo(hash); assertThat(record.getRequestCount()).isEqualTo(2);
        record.setRequestedAt(OffsetDateTime.now().minusSeconds(61));
        when(ses.sendEmail(any(SendEmailRequest.class))).thenThrow(SdkClientException.create("sensitive credentials"));
        service.requestOtp(user.getEmail());
        assertThat(record.getOtpHash()).isEqualTo(hash); assertThat(record.getRequestCount()).isEqualTo(3);
        assertThat(service.verify(user.getEmail(), code)).isNotBlank(); verify(users, never()).save(any());
    }
    @Test void invalidAttemptsCommitThroughSpringTransactionProxy() {
        eligible(); String code = request();
        var transactions = mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenAnswer(i -> new org.springframework.transaction.support.SimpleTransactionStatus());
        var interceptor = new org.springframework.transaction.interceptor.TransactionInterceptor(transactions,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
        var factory = new org.springframework.aop.framework.ProxyFactory(service);
        factory.addAdvice(interceptor);
        var proxy = (EmailPasswordRecoveryService) factory.getProxy();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> proxy.resetWithOtp(user.getEmail(), wrong, "Changed@123")).isInstanceOf(InvalidOtpException.class);
        }
        verify(transactions, times(5)).commit(any());
        verify(transactions, never()).rollback(any());
        assertThat(record.getAttempts()).isEqualTo(5);
    }

    @Test void unknownAndExpiredCodeChecksStillPerformHashComparison() {
        eligible(); String code = request();
        clearInvocations(encoder);
        assertThatThrownBy(() -> service.verify("unknown@example.com", code)).isInstanceOf(InvalidOtpException.class);
        verify(encoder).matches(eq(code), anyString());
        clearInvocations(encoder);
        record.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.verify(user.getEmail(), code)).isInstanceOf(InvalidOtpException.class);
        verify(encoder).matches(eq(code), anyString());
    }

    @Test void registrationVerificationTokenCannotAuthorizePasswordRecovery() {
        eligible(); String originalHash = user.getPasswordHash();
        user.setEmailVerificationHash(EmailVerificationService.hash("123456"));
        user.setEmailVerificationExpiresAt(OffsetDateTime.now().plusMinutes(30));
        assertThatThrownBy(() -> service.resetWithOtp(user.getEmail(), "123456", "Changed@123"))
                .isInstanceOf(InvalidOtpException.class);
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        assertThat(user.getTokenVersion()).isZero();
        assertThat(user.getEmailVerificationHash()).isEqualTo(EmailVerificationService.hash("123456"));
        verify(users, never()).save(any());
    }

    @Test void badResetTokenAndOtherAccountCannotChangePassword() {
        eligible(); String token = service.verify(user.getEmail(), request());
        assertThatThrownBy(() -> service.reset(user.getEmail(), "wrong", "Changed@123")).isInstanceOf(InvalidOtpException.class);
        assertThatThrownBy(() -> service.reset("other@example.com", token, "Changed@123")).isInstanceOf(InvalidOtpException.class);
        verify(users, never()).save(any());
    }
}
