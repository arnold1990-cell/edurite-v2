package com.edurite.auth.service;

import com.edurite.auth.entity.EmailPasswordReset;
import com.edurite.auth.repository.EmailPasswordResetRepository;
import com.edurite.auth.exception.InvalidOtpException;
import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

@Service
public class EmailPasswordRecoveryService {
    public static final String REQUEST_MESSAGE = "If the account is eligible, a verification code will be sent. Check your inbox and spam folder.";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(EmailPasswordRecoveryService.class);
    private final UserRepository users;
    private final EmailPasswordResetRepository resets;
    private final PasswordEncoder encoder;
    private final SesV2Client ses;
    private final String from;
    private final String dummyOtpHash;

    public EmailPasswordRecoveryService(UserRepository users, EmailPasswordResetRepository resets,
            PasswordEncoder encoder, SesV2Client ses,
            @Value("${app.email-verification.from:info@edurite.co.za}") String from) {
        this.users = users; this.resets = resets; this.encoder = encoder; this.ses = ses; this.from = from;
        this.dummyOtpHash = encoder.encode("000000");
    }

    // Return the same HTTP response before account lookup or SES delivery to avoid timing enumeration.
    @Async
    @Transactional
    public java.util.concurrent.CompletableFuture<Void> requestOtp(String email) {
        User user = users.lockForEmailVerification(normalize(email)).orElse(null);
        if (!eligible(user)) return java.util.concurrent.CompletableFuture.completedFuture(null);
        EmailPasswordReset record = resets.findByUserId(user.getId()).orElseGet(() -> {
            EmailPasswordReset value = new EmailPasswordReset(); value.setUserId(user.getId()); return value;
        });
        OffsetDateTime now = OffsetDateTime.now();
        if (record.getRequestedAt() != null && record.getRequestedAt().isAfter(now.minusMinutes(1))) return java.util.concurrent.CompletableFuture.completedFuture(null);
        if (record.getWindowStartedAt() == null || !record.getWindowStartedAt().isAfter(now.minusHours(1))) {
            record.setWindowStartedAt(now); record.setRequestCount(0);
        }
        if (record.getRequestCount() >= 5) return java.util.concurrent.CompletableFuture.completedFuture(null);
        record.setRequestedAt(now);
        record.setRequestCount(record.getRequestCount() + 1);
        // Persist throttling even on delivery failure. Existing accepted code stays valid on failure.
        resets.save(record);
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        String hash = encoder.encode(code);
        SendEmailRequest request = SendEmailRequest.builder().fromEmailAddress(from)
                .destination(d -> d.toAddresses(user.getEmail()))
                .content(c -> c.simple(m -> m.subject(s -> s.data("Your EduRite password reset code"))
                    .body(b -> b.text(t -> t.data("Your EduRite verification code is: " + code
                        + "\n\nThis single-use code expires in 10 minutes. If you did not request a password reset, ignore this email.")))))
                .build();
        try {
            ses.sendEmail(request);
        } catch (SesV2Exception ex) {
            log.warn("Password recovery SES rejected: status={} requestId={}", ex.statusCode(), ex.requestId());
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        } catch (SdkClientException ex) {
            log.warn("Password recovery SES client failure; check AWS credentials and connectivity");
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        record.setOtpHash(hash); record.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        record.setAttempts(0); record.setResetTokenHash(null); record.setResetTokenExpiresAt(null);
        resets.save(record);
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }

    public static Void deliveryFailed(Throwable failure) {
        log.warn("Password recovery processing failed; check database and mail connectivity");
        return null;
    }

    // Invalid attempts MUST commit; rolling back would defeat the attempt limit.
    @Transactional(noRollbackFor = InvalidOtpException.class)
    public String verify(String email, String code) {
        User user = users.lockForEmailVerification(normalize(email)).orElse(null);
        EmailPasswordReset record = eligible(user) ? resets.findByUserId(user.getId()).orElse(null) : null;
        if (record == null || record.getOtpHash() == null || record.getExpiresAt() == null
                || !record.getExpiresAt().isAfter(OffsetDateTime.now()) || record.getAttempts() >= 5) {
            // Match the expensive comparison used for a wrong code without exposing account state.
            encoder.matches(code, dummyOtpHash);
            throw invalid();
        }
        record.setAttempts(record.getAttempts() + 1);
        if (!encoder.matches(code, record.getOtpHash())) {
            if (record.getAttempts() >= 5) record.setOtpHash(null);
            resets.save(record);
            throw invalid();
        }
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        record.setOtpHash(null); record.setExpiresAt(null);
        record.setResetTokenHash(EmailVerificationService.hash(token));
        record.setResetTokenExpiresAt(OffsetDateTime.now().plusMinutes(10));
        resets.save(record);
        return token;
    }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public VerificationStatusResponse resetWithOtp(String email, String code, String password) {
        String resetToken = verify(email, code);
        return reset(email, resetToken, password);
    }

    @Transactional
    public VerificationStatusResponse reset(String email, String token, String password) {
        User user = lockedEligibleUser(email);
        EmailPasswordReset record = resets.findByUserId(user.getId()).orElseThrow(this::invalid);
        if (record.getResetTokenHash() == null || record.getResetTokenExpiresAt() == null
                || !record.getResetTokenExpiresAt().isAfter(OffsetDateTime.now())
                || !java.security.MessageDigest.isEqual(record.getResetTokenHash().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    EmailVerificationService.hash(token).getBytes(java.nio.charset.StandardCharsets.UTF_8))) throw invalid();
        user.setPasswordHash(encoder.encode(password));
        user.setMustChangePassword(false);
        user.setTokenVersion(Math.addExact(user.getTokenVersion(), 1));
        users.save(user);
        record.setResetTokenHash(null); record.setResetTokenExpiresAt(null);
        record.setOtpHash(null); record.setExpiresAt(null);
        resets.save(record);
        return new VerificationStatusResponse("Password reset complete. Sign in with your new password.");
    }

    private User lockedEligibleUser(String email) {
        User user = users.lockForEmailVerification(normalize(email)).orElseThrow(this::invalid);
        if (!eligible(user)) throw invalid();
        return user;
    }
    private boolean eligible(User user) { return user != null && user.getStatus() == UserStatus.ACTIVE && user.getDeletedAt() == null; }
    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private InvalidOtpException invalid() { return new InvalidOtpException("Invalid or expired code or reset session. Request a new code if needed."); }
}
