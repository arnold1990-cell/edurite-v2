package com.edurite.account;

import com.edurite.account.dto.ForcePasswordChangeRequest;
import com.edurite.account.dto.DeleteAccountRequest;
import com.edurite.account.entity.AccountDeletionAudit;
import com.edurite.account.repository.AccountDeletionAuditRepository;
import com.edurite.account.service.AccountService;
import com.edurite.auth.config.AuthOtpProperties;
import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.exception.InvalidOtpException;
import com.edurite.auth.service.AuthService;
import com.edurite.auth.service.OtpService;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.security.service.CurrentUserService;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccountDeletionAuditRepository deletionAuditRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private AuthService authService;

    private AccountService accountService;
    private Principal principal;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                currentUserService,
                userRepository,
                passwordEncoder,
                deletionAuditRepository,
                otpService,
                new AuthOtpProperties(true),
                authService
        );
        principal = () -> "admin@edurite.com";
    }

    @Test
    void selfDeleteSoftDeletesAndAnonymizesUser() {
        User user = activeUser("student@edurite.com", "ROLE_STUDENT");
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("deleted-password-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deletionAuditRepository.save(any(AccountDeletionAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = accountService.deleteMyAccount(principal, new DeleteAccountRequest("DELETE", "Testing account deletion"));

        assertThat(response.get("message")).isEqualTo("Account deleted successfully.");
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getRoles()).isEmpty();
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getEmail()).contains("deleted+");
    }

    @Test
    void adminDeleteSoftDeletesTargetUser() {
        User actor = activeUser("admin@edurite.com", "ROLE_ADMIN");
        User target = activeUser("company@edurite.com", "ROLE_COMPANY");
        UUID targetId = target.getId();
        when(currentUserService.requireUser(principal)).thenReturn(actor);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode(any())).thenReturn("deleted-password-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deletionAuditRepository.save(any(AccountDeletionAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = accountService.deleteAccountByAdmin(principal, targetId, "Admin cleanup");

        assertThat(response.get("message")).isEqualTo("Account deleted successfully.");
        assertThat(target.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(target.getDeletedAt()).isNotNull();
        assertThat(target.getDeletionReason()).isEqualTo("Admin cleanup");
        assertThat(target.getRoles()).isEmpty();
    }

    @Test
    void selfDeleteRejectsInvalidConfirmationText() {
        User user = activeUser("student@edurite.com", "ROLE_STUDENT");
        when(currentUserService.requireUser(principal)).thenReturn(user);

        assertThatThrownBy(() -> accountService.deleteMyAccount(principal, new DeleteAccountRequest("remove", "bad text")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Please type DELETE to confirm account removal.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void adminDeleteWritesAuditRow() {
        User actor = activeUser("admin@edurite.com", "ROLE_ADMIN");
        User target = activeUser("student2@edurite.com", "ROLE_STUDENT");
        when(currentUserService.requireUser(principal)).thenReturn(actor);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(passwordEncoder.encode(any())).thenReturn("deleted-password-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deletionAuditRepository.save(any(AccountDeletionAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.deleteAccountByAdmin(principal, target.getId(), null);

        ArgumentCaptor<AccountDeletionAudit> captor = ArgumentCaptor.forClass(AccountDeletionAudit.class);
        verify(deletionAuditRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(target.getId());
        assertThat(captor.getValue().getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    void forcePasswordChangeUpdatesPasswordAndClearsFlag() {
        User user = activeUser("district@edurite.com", "ROLE_DISTRICT_ADMIN");
        user.setMustChangePassword(true);
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(passwordEncoder.matches("Temp@12345", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("Secure@123")).thenReturn("new-password-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authService.issueAuthResponse(user, "Password changed successfully."))
                .thenReturn(new AuthResponse(null, null, null, 0L, null, null, null, false, null, "Password changed successfully."));

        var response = accountService.forcePasswordChange(principal, new ForcePasswordChangeRequest("Temp@12345", "Secure@123", "Secure@123"));

        assertThat(response.message()).isEqualTo("Password changed successfully.");
        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(user.getPasswordHash()).isEqualTo("new-password-hash");
    }

    @Test
    void forcePasswordChangeRejectsConfirmationMismatch() {
        User user = activeUser("district@edurite.com", "ROLE_DISTRICT_ADMIN");
        user.setMustChangePassword(true);
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(passwordEncoder.matches("Temp@12345", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> accountService.forcePasswordChange(principal, new ForcePasswordChangeRequest("Temp@12345", "Secure@123", "Secure@124")))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessage("New password and confirm password do not match.");
    }

    @Test
    void forcePasswordChangeRejectsRequestWhenFlagNotSet() {
        User user = activeUser("district@edurite.com", "ROLE_DISTRICT_ADMIN");
        user.setMustChangePassword(false);
        when(currentUserService.requireUser(principal)).thenReturn(user);

        assertThatThrownBy(() -> accountService.forcePasswordChange(principal, new ForcePasswordChangeRequest("Temp@12345", "Secure@123", "Secure@123")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("This account is not currently required to change its password.");
    }

    private User activeUser(String email, String roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPhoneNumber("+26770000000");
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.getRoles().add(role);
        return user;
    }
}


