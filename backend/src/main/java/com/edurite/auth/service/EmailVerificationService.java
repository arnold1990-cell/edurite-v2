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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmailVerificationService {
    private final UserRepository users;
    private final ObjectProvider<JavaMailSender> mail;
    private final String baseUrl;
    private final String from;

    public EmailVerificationService(UserRepository users, ObjectProvider<JavaMailSender> mail,
            @Value("${app.email-verification.base-url:http://localhost:5173}") String baseUrl,
            @Value("${app.email-verification.from:}") String from) {
        this.users = users; this.mail = mail; this.baseUrl = baseUrl; this.from = from;
    }

    public boolean send(User user) {
        if (!user.isEmailVerificationRequired()) return false;
        if (user.getEmailVerificationSentAt() != null && user.getEmailVerificationSentAt().isAfter(OffsetDateTime.now().minusMinutes(1))) return true;
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null || from.isBlank()) return false;
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String link = UriComponentsBuilder.fromUriString(baseUrl).path("/verify-email")
                .queryParam("email", user.getEmail()).queryParam("token", token).build().encode().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(user.getEmail());
        message.setSubject("Verify your EduRite email");
        message.setText("Confirm your email to access EduRite and your 14-Day Free Trial.\n\n" + link
                + "\n\nThis single-use link expires in 30 minutes. If you did not register, ignore this email.");
        try { sender.send(message); } catch (RuntimeException ex) { return false; }
        user.setEmailVerificationHash(hash(token));
        user.setEmailVerificationExpiresAt(OffsetDateTime.now().plusMinutes(30));
        user.setEmailVerificationSentAt(OffsetDateTime.now());
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
