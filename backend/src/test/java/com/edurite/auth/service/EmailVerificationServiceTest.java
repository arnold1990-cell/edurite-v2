package com.edurite.auth.service;

import com.edurite.user.entity.*;
import com.edurite.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class EmailVerificationServiceTest {
    @Test void sendsHashedSingleUseEmailLinkAndThrottlesResends() {
        UserRepository users = mock(UserRepository.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        User user = pending();
        when(users.lockForEmailVerification(user.getEmail())).thenReturn(Optional.of(user));
        var service = new EmailVerificationService(users, provider, "https://edurite.example", "verify@edurite.example");
        assertThat(service.send(user)).isTrue();
        assertThat(service.send(user)).isTrue();
        var mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, times(1)).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly(user.getEmail());
        String token = mail.getValue().getText().split("token=")[1].split("\\s")[0];
        assertThat(user.getEmailVerificationHash()).isEqualTo(EmailVerificationService.hash(token)).isNotEqualTo(token);
        service.verify(user.getEmail(), token);
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.isEmailVerificationRequired()).isFalse();
        assertThatThrownBy(() -> service.verify(user.getEmail(), token)).hasMessageContaining("Invalid or expired");
    }
    @Test void rejectsExpiredLinksAndKeepsAccountBlockedWhenMailUnavailable() {
        var users = mock(UserRepository.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        var service = new EmailVerificationService(users, provider, "https://edurite.example", "");
        User user = pending();
        assertThat(service.send(user)).isFalse();
        assertThat(user.isEmailVerificationRequired()).isTrue();
        user.setEmailVerificationHash(EmailVerificationService.hash("secret"));
        user.setEmailVerificationExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(users.lockForEmailVerification(user.getEmail())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.verify(user.getEmail(), "secret")).hasMessageContaining("expired");
        assertThat(user.isEmailVerified()).isFalse();
    }
    private User pending() { var user = new User(); user.setEmail("student@example.com"); user.setStatus(UserStatus.ACTIVE); user.setEmailVerificationRequired(true); return user; }
}
