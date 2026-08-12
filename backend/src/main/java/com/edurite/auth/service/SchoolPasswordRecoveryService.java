package com.edurite.auth.service;

import com.edurite.auth.dto.SchoolPasswordRecoveryDtos;
import com.edurite.auth.entity.SchoolPasswordResetOtp;
import com.edurite.auth.exception.InvalidOtpException;
import com.edurite.auth.repository.SchoolPasswordResetOtpRepository;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolRegistrationRequest;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.service.SouthAfricanMobileNumber;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolPasswordRecoveryService {

    private static final String GENERIC_MESSAGE = "If the school account exists, an OTP has been sent to the registered mobile number.";
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SchoolRepository schoolRepository;
    private final SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final SchoolPasswordResetOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;

    public SchoolPasswordRecoveryService(
            SchoolRepository schoolRepository,
            SchoolRegistrationRequestRepository schoolRegistrationRequestRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            SchoolPasswordResetOtpRepository otpRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SmsService smsService
    ) {
        this.schoolRepository = schoolRepository;
        this.schoolRegistrationRequestRepository = schoolRegistrationRequestRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsService = smsService;
    }

    @Transactional
    public SchoolPasswordRecoveryDtos.RecoveryResponse requestOtp(SchoolPasswordRecoveryDtos.ForgotPasswordRequest request) {
        Optional<RecoveryContext> context = resolveContext(request.emis());
        if (context.isEmpty()) {
            return new SchoolPasswordRecoveryDtos.RecoveryResponse(GENERIC_MESSAGE, null, null);
        }

        RecoveryContext resolved = context.get();
        invalidateActiveOtps(resolved.schoolId(), resolved.user().getId());
        String otp = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        SchoolPasswordResetOtp record = new SchoolPasswordResetOtp();
        record.setSchoolId(resolved.schoolId());
        record.setUserId(resolved.user().getId());
        record.setEmisNumber(resolved.emis());
        record.setMobileNumber(resolved.mobileNumber());
        record.setOtpHash(passwordEncoder.encode(otp));
        record.setExpiresAt(OffsetDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        record.setUsed(false);
        record.setAttempts(0);
        otpRepository.save(record);
        smsService.sendOtp(resolved.mobileNumber(), otp);
        return new SchoolPasswordRecoveryDtos.RecoveryResponse(
                GENERIC_MESSAGE,
                SouthAfricanMobileNumber.mask(resolved.mobileNumber()),
                null
        );
    }

    @Transactional
    public SchoolPasswordRecoveryDtos.RecoveryResponse resendOtp(SchoolPasswordRecoveryDtos.ForgotPasswordRequest request) {
        return requestOtp(request);
    }

    @Transactional
    public SchoolPasswordRecoveryDtos.RecoveryResponse verifyOtp(SchoolPasswordRecoveryDtos.VerifyOtpRequest request) {
        RecoveryContext context = resolveContext(request.emis())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP code"));
        SchoolPasswordResetOtp record = otpRepository.findFirstBySchoolIdAndUserIdAndUsedFalseOrderByCreatedAtDesc(context.schoolId(), context.user().getId())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP code"));
        OffsetDateTime now = OffsetDateTime.now();
        if (record.getExpiresAt().isBefore(now)) {
            record.setUsed(true);
            otpRepository.save(record);
            throw new InvalidOtpException("Invalid or expired OTP code");
        }
        if (record.getAttempts() >= MAX_ATTEMPTS) {
            record.setUsed(true);
            otpRepository.save(record);
            throw new InvalidOtpException("Too many invalid attempts. Request a new OTP.");
        }
        if (!passwordEncoder.matches(request.otp().trim(), record.getOtpHash())) {
            record.setAttempts(record.getAttempts() + 1);
            if (record.getAttempts() >= MAX_ATTEMPTS) {
                record.setUsed(true);
                otpRepository.save(record);
                throw new InvalidOtpException("Too many invalid attempts. Request a new OTP.");
            }
            otpRepository.save(record);
            throw new InvalidOtpException("Invalid or expired OTP code");
        }
        String resetToken = UUID.randomUUID() + "-" + Long.toUnsignedString(SECURE_RANDOM.nextLong(), 36);
        record.setResetTokenHash(passwordEncoder.encode(resetToken));
        record.setResetTokenExpiresAt(now.plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
        otpRepository.save(record);
        return new SchoolPasswordRecoveryDtos.RecoveryResponse("OTP verified.", null, resetToken);
    }

    @Transactional
    public SchoolPasswordRecoveryDtos.RecoveryResponse resetPassword(SchoolPasswordRecoveryDtos.ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResourceConflictException("New password and confirm password do not match.");
        }
        validatePasswordPolicy(request.newPassword());
        SchoolPasswordResetOtp record = otpRepository.findAll().stream()
                .filter(item -> !item.isUsed())
                .filter(item -> item.getResetTokenHash() != null)
                .filter(item -> item.getResetTokenExpiresAt() != null && item.getResetTokenExpiresAt().isAfter(OffsetDateTime.now()))
                .filter(item -> passwordEncoder.matches(request.resetToken(), item.getResetTokenHash()))
                .findFirst()
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired reset token"));
        User user = userRepository.findById(record.getUserId())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired reset token"));
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new InvalidOtpException("Invalid or expired reset token");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        record.setUsed(true);
        otpRepository.save(record);
        return new SchoolPasswordRecoveryDtos.RecoveryResponse("Password reset complete.", null, null);
    }

    private Optional<RecoveryContext> resolveContext(String emis) {
        String normalizedEmis = emis == null ? null : emis.trim().toUpperCase(Locale.ROOT);
        if (normalizedEmis == null || normalizedEmis.isBlank()) {
            return Optional.empty();
        }
        Optional<School> school = schoolRepository.findByRegistrationNumberIgnoreCase(normalizedEmis)
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()));
        if (school.isEmpty()) {
            return Optional.empty();
        }
        String mobileNumber = SouthAfricanMobileNumber.normalize(school.get().getContactPhone());
        if (mobileNumber == null) {
            return Optional.empty();
        }
        User user = resolveSchoolAdminUser(school.get(), normalizedEmis).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(new RecoveryContext(school.get().getId(), user, normalizedEmis, mobileNumber));
    }

    private Optional<User> resolveSchoolAdminUser(School school, String emis) {
        List<SchoolUserProfile> profiles = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(school.getId(), "ROLE_SCHOOL_ADMIN");
        for (SchoolUserProfile profile : profiles) {
            Optional<User> user = userRepository.findById(profile.getUserId());
            if (user.isPresent()) {
                return user;
            }
        }
        Optional<SchoolRegistrationRequest> registration = schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase(emis);
        if (registration.isPresent()) {
            return userRepository.findById(registration.get().getUserId());
        }
        return userRepository.findByUsernameIgnoreCase(emis);
    }

    private void invalidateActiveOtps(UUID schoolId, UUID userId) {
        List<SchoolPasswordResetOtp> active = otpRepository.findBySchoolIdAndUserIdAndUsedFalseOrderByCreatedAtDesc(schoolId, userId);
        active.forEach(item -> item.setUsed(true));
        otpRepository.saveAll(active);
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new ResourceConflictException("New password must include uppercase, lowercase, number, and special character.");
        }
    }

    private record RecoveryContext(UUID schoolId, User user, String emis, String mobileNumber) {}
}
