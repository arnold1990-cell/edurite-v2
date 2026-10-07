package com.edurite.auth.service;

import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final SesV2Client ses;
    private final String baseUrl;
    private final String from;

    public EmailVerificationService(UserRepository users, SesV2Client ses,
            @Value("${app.email-verification.base-url:https://edurite.co.za}") String baseUrl,
            @Value("${app.email-verification.from:info@edurite.co.za}") String from) {
        this.users = users; this.ses = ses; this.baseUrl = baseUrl; this.from = from;
    }

    public boolean send(User user) {
        if (!user.isEmailVerificationRequired()) return false;
        if (user.getEmailVerificationSentAt() != null && user.getEmailVerificationSentAt().isAfter(OffsetDateTime.now().minusMinutes(1))) return true;
        if (from.isBlank()) return false;
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String link = UriComponentsBuilder.fromUriString(baseUrl).path("/verify-email")
                .queryParam("email", user.getEmail()).queryParam("token", token).build().encode().toUriString();
        String body = "Confirm your email to access EduRite and your 14-Day Free Trial.\n\n" + link
                + "\n\nThis single-use link expires in 30 minutes. If you did not register, ignore this email.";
        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(from)
                .destination(d -> d.toAddresses(user.getEmail()))
                .content(c -> c.simple(m -> m
                        .subject(s -> s.data("Verify your EduRite email").charset("UTF-8"))
                        .body(b -> b.text(t -> t.data(body).charset("UTF-8")))))
                .build();
        try {
            ses.sendEmail(request);
            log.info("Email verification SES accepted");
        } catch (SesV2Exception ex) {
            // Never log exception messages, request bodies, recipients or links.
            log.warn("Email verification SES rejected: status={} code={} requestId={}",
                    ex.statusCode(), ex.awsErrorDetails() == null ? "unknown" : ex.awsErrorDetails().errorCode(), ex.requestId());
            return false;
        } catch (SdkClientException ex) {
            log.warn("Email verification SES client failure: check IAM role, container IMDS access and SES connectivity");
            return false;
        }
        OffsetDateTime acceptedAt = OffsetDateTime.now();
        user.setEmailVerificationHash(hash(token));
        user.setEmailVerificationExpiresAt(acceptedAt.plusMinutes(30));
        user.setEmailVerificationSentAt(acceptedAt);
        users.save(user);
        return true;
    }

    @Transactional
    public VerificationStatusResponse resend(String email) {
        User user = users.lockForEmailVerification(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user != null && user.getStatus() == UserStatus.ACTIVE && user.getDeletedAt() == null) {
            if (user.isEmailVerificationRequired() && !send(user)) {
                throw new ResourceConflictException("Verification email could not be sent. Please try again later or contact support.");
            }
        }
        return new VerificationStatusResponse("If verification is needed, an email has been sent. Check your inbox and spam folder.");
    }

    @Transactional
    public VerificationStatusResponse verify(String email, String token) {
        User user = users.lockForEmailVerification(email.trim()).orElseThrow(() -> new ResourceConflictException("Invalid or expired verification link."));
        if (!user.isEmailVerificationRequired() || user.getDeletedAt() != null || user.getStatus() != UserStatus.ACTIVE
                || user.getEmailVerificationHash() == null || user.getEmailVerificationExpiresAt() == null
                || !user.getEmailVerificationExpiresAt().isAfter(OffsetDateTime.now())
                || !MessageDigest.isEqual(user.getEmailVerificationHash().getBytes(StandardCharsets.UTF_8), hash(token).getBytes(StandardCharsets.UTF_8))) {
            throw new ResourceConflictException("Invalid or expired verification link. Request a new email.");
        }
        user.setEmailVerified(true); user.setEmailVerificationRequired(false);
        user.setEmailVerificationHash(null); user.setEmailVerificationExpiresAt(null);
        users.save(user);
        return new VerificationStatusResponse("Email verified. You can now sign in to your Student Portal.");
    }

    static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
