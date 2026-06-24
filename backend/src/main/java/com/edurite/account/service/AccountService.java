package com.edurite.account.service;

// DTOs coming from frontend requests
import com.edurite.account.dto.ChangePasswordWithOtpRequest;
import com.edurite.account.dto.DeleteAccountRequest;
import com.edurite.account.dto.ForcePasswordChangeRequest;

// Entity for audit logging
import com.edurite.account.entity.AccountDeletionAudit;

// Repository to save audit logs
import com.edurite.account.repository.AccountDeletionAuditRepository;

// OTP configuration (enabled/disabled settings)
import com.edurite.auth.config.AuthOtpProperties;
import com.edurite.auth.dto.AuthResponse;

// Response returned after OTP actions
import com.edurite.auth.dto.VerificationStatusResponse;

// Custom exceptions
import com.edurite.auth.exception.InvalidOtpException;
import com.edurite.common.exception.ResourceConflictException;

// OTP service to send/verify codes
import com.edurite.auth.service.OtpService;
import com.edurite.auth.service.AuthService;

// Service to get currently logged-in user
import com.edurite.security.service.CurrentUserService;

// User entity and repository
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;

// Java utilities
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Password hashing
import org.springframework.security.crypto.password.PasswordEncoder;

// Spring annotations
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class contains BUSINESS LOGIC for account operations.
 *
 * It handles:
 * - Delete account
 * - Admin delete account
 * - Request OTP
 * - Change password with OTP
 */
@Service // Marks this as a Spring service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    // Dependencies (injected automatically by Spring)
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountDeletionAuditRepository deletionAuditRepository;
    private final OtpService otpService;
    private final AuthOtpProperties authOtpProperties;
    private final AuthService authService;

    /**
     * Constructor injection (best practice)
     */
    public AccountService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccountDeletionAuditRepository deletionAuditRepository,
            OtpService otpService,
            AuthOtpProperties authOtpProperties,
            AuthService authService
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.deletionAuditRepository = deletionAuditRepository;
        this.otpService = otpService;
        this.authOtpProperties = authOtpProperties;
        this.authService = authService;
    }

    /**
     * USER deletes their own account
     */
    @Transactional
    public Map<String, String> deleteMyAccount(Principal principal, DeleteAccountRequest request) {

        // Get currently logged-in user
        User user = currentUserService.requireUser(principal);

        // Check confirmation text
        if (!"DELETE".equalsIgnoreCase(request.confirmationText().trim())) {
            throw new ResourceConflictException("Please type DELETE to confirm account removal.");
        }

        // Perform deletion logic
        return anonymizeAndDeactivate(user, request.reason());
    }

    /**
     * ADMIN deletes another user's account
     */
    @Transactional
    public Map<String, String> deleteAccountByAdmin(Principal principal, UUID userId, String reason) {

        // Admin performing the action
        User actor = currentUserService.requireUser(principal);

        // Target user
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceConflictException("User not found"));

        // If already deleted
        if (target.getStatus() == UserStatus.DELETED) {
            return Map.of("message", "Account is already deleted.");
        }

        // Set reason
        String actorReason = reason == null || reason.isBlank()
                ? "Deleted by admin " + actor.getId()
                : reason.trim();

        return anonymizeAndDeactivate(target, actorReason);
    }

    /**
     * Step 1: Request OTP for password change
     */
    @Transactional
    public VerificationStatusResponse requestPasswordChangeOtp(Principal principal) {

        User user = currentUserService.requireUser(principal);

        // Ensure OTP feature is enabled
        requireOtpEnabled();

        // Get phone number
        String phoneNumber = normalizeStoredPhone(user.getPhoneNumber());

        if (phoneNumber == null) {
            throw new InvalidOtpException("No phone number is registered for this account.");
        }

        // Send OTP
        otpService.sendPasswordResetOtp(phoneNumber);

        return new VerificationStatusResponse("Password change OTP sent.");
    }

    /**
     * Step 2: Change password using OTP
     */
    @Transactional
    public AuthResponse changePasswordWithOtp(
            Principal principal,
            ChangePasswordWithOtpRequest request
    ) {

        User user = currentUserService.requireUser(principal);

        requireOtpEnabled();

        // Check current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidOtpException("Current password is incorrect.");
        }

        // Prevent same password reuse
        if (request.currentPassword().equals(request.newPassword())) {
            throw new InvalidOtpException("New password must be different from the current password.");
        }

        String phoneNumber = normalizeStoredPhone(user.getPhoneNumber());

        if (phoneNumber == null) {
            throw new InvalidOtpException("No phone number is registered for this account.");
        }

        // Verify OTP
        boolean approved = otpService.verifyPasswordResetOtp(
                phoneNumber,
                request.code().trim()
        );

        if (!approved) {
            throw new InvalidOtpException("Invalid or expired OTP code");
        }

        // Update password (IMPORTANT: hashed)
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);

        userRepository.save(user);

        return authService.issueAuthResponse(user, "Password changed successfully.");
    }

    @Transactional
    public AuthResponse forcePasswordChange(
            Principal principal,
            ForcePasswordChangeRequest request
    ) {
        User user = currentUserService.requireUser(principal);
        String roleNames = user.getRoles().stream().map(role -> role.getName() == null ? "UNKNOWN" : role.getName()).sorted().collect(Collectors.joining(","));
        log.info(
                "First-login password change requested: email={}, roles={}, mustChangePassword={}, emailVerified={}",
                user.getEmail(),
                roleNames,
                user.isMustChangePassword(),
                user.isEmailVerified()
        );

        if (!user.isMustChangePassword()) {
            log.warn("First-login password change rejected: email={}, reason=mustChangePassword flag is false", user.getEmail());
            throw new ResourceConflictException("This account is not currently required to change its password.");
        }

        boolean currentPasswordMatches = passwordEncoder.matches(request.currentPassword(), user.getPasswordHash());
        log.info("First-login password verification result: email={}, matched={}", user.getEmail(), currentPasswordMatches);
        if (!currentPasswordMatches) {
            log.warn("First-login password change rejected: email={}, reason=current password mismatch", user.getEmail());
            throw new InvalidOtpException("Current password is incorrect.");
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            log.warn("First-login password change rejected: email={}, reason=confirm password mismatch", user.getEmail());
            throw new InvalidOtpException("New password and confirm password do not match.");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            log.warn("First-login password change rejected: email={}, reason=new password matches current password", user.getEmail());
            throw new InvalidOtpException("New password must be different from the current password.");
        }

        validatePasswordStrength(request.newPassword());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("First-login password change completed: email={}, roles={}, mustChangePassword={}", user.getEmail(), roleNames, user.isMustChangePassword());

        return authService.issueAuthResponse(user, "Password changed successfully.");
    }

    /**
     * CORE deletion logic (used by both user + admin)
     */
    private Map<String, String> anonymizeAndDeactivate(User user, String reason) {

        OffsetDateTime now = OffsetDateTime.now();
        String resolvedReason = reason == null || reason.isBlank() ? "Account deleted" : reason.trim();

        // Save audit log
        AccountDeletionAudit audit = new AccountDeletionAudit();
        audit.setUserId(user.getId());
        audit.setReason(resolvedReason);
        audit.setDeletedAt(now);
        deletionAuditRepository.save(audit);

        // Anonymize user data (VERY IMPORTANT)
        String anonymizedEmail = "deleted+" + user.getId() + "@edurite.local";

        user.setEmail(anonymizedEmail);
        user.setUsername(null);
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setPhoneNumber(null);

        // Replace password with random value
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.getRoles().clear();

        user.setEmailVerified(false);
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(now);
        user.setDeletionReason(resolvedReason);

        userRepository.save(user);

        return Map.of("message", "Account deleted successfully.");
    }

    /**
     * Ensures OTP feature is enabled
     */
    private void requireOtpEnabled() {
        if (!authOtpProperties.enabled()) {
            throw new ResourceConflictException("Password change OTP is currently unavailable.");
        }
    }

    /**
     * Cleans phone number
     */
    private String normalizeStoredPhone(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String compact = phoneNumber.trim();
        return compact.isEmpty() ? null : compact;
    }

    private void validatePasswordStrength(String password) {
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(character -> !Character.isLetterOrDigit(character));

        if (!(hasUppercase && hasLowercase && hasDigit && hasSpecial)) {
            log.warn("First-login password change rejected: reason=password strength validation failed");
            throw new InvalidOtpException("New password must include uppercase, lowercase, number, and special character.");
        }
    }
}

