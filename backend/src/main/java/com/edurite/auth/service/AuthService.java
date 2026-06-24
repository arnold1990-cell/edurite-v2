package com.edurite.auth.service;

import com.edurite.auth.config.AuthOtpProperties;
import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.CompanyRegisterRequest;
import com.edurite.auth.dto.DevPasswordResetRequest;
import com.edurite.auth.dto.ForgotPasswordOtpRequest;
import com.edurite.auth.dto.GoogleIdentity;
import com.edurite.auth.dto.GoogleLoginRequest;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.dto.RegisterRequest;
import com.edurite.auth.dto.ResendVerificationOtpRequest;
import com.edurite.auth.dto.RegistrationResponse;
import com.edurite.auth.dto.ResetPasswordWithOtpRequest;
import com.edurite.auth.dto.SchoolRegisterRequest;
import com.edurite.auth.dto.StudentRegisterRequest;
import com.edurite.auth.dto.VerifyOtpRequest;
import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.auth.exception.InvalidOtpException;
import com.edurite.auth.exception.InvalidPhoneNumberException;
import com.edurite.admin.entity.PlatformSetting;
import com.edurite.admin.service.PlatformSettingsService;
import com.edurite.common.exception.DuplicateEmailException;
import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.compliance.service.ConsentService;
import com.edurite.district.entity.Circuit;
import com.edurite.district.entity.District;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.district.service.LocationService;
import com.edurite.gamification.service.GamificationService;
import com.edurite.notification.service.NotificationService;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolRegistrationRequest;
import com.edurite.school.portal.entity.SchoolStatus;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.security.service.JwtService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentProfileCompletionService;
import com.edurite.subscription.entity.PlanType;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.subscription.service.SubscriptionService;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final List<String> ROLE_PRIORITY = List.of(
            "ROLE_ADMIN",
            "ROLE_DISTRICT_ADMIN",
            "ROLE_DISTRICT_DIRECTOR",
            "ROLE_CIRCUIT_MANAGER",
            "ROLE_SUBJECT_ADVISOR",
            "ROLE_SCHOOL_ADMIN",
            "ROLE_TEACHER",
            "ROLE_COMPANY",
            "ROLE_SCHOOL_STUDENT",
            "ROLE_STUDENT"
    );
    private static final String GENERIC_FORGOT_PASSWORD_OTP_MESSAGE = "If the account exists, an OTP has been sent.";

    private enum LoginAuditReason {
        USER_NOT_FOUND,
        PASSWORD_MISMATCH,
        ACCOUNT_DISABLED,
        EMAIL_NOT_VERIFIED,
        ROLE_MISSING,
        APPROVAL_PENDING
    }

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final DistrictRepository districtRepository;
    private final DistrictAdminProfileRepository districtAdminProfileRepository;
    private final LocationService locationService;
    private final SchoolRepository schoolRepository;
    private final SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final AuthOtpProperties authOtpProperties;
    private final ConsentService consentService;
    private final GamificationService gamificationService;
    private final StudentProfileCompletionService studentProfileCompletionService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final PlatformSettingsService platformSettingsService;
    private final SubscriptionService subscriptionService;
    private final StudentPlanAccessService studentPlanAccessService;
    private final NotificationService notificationService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            CompanyProfileRepository companyProfileRepository,
            DistrictRepository districtRepository,
            DistrictAdminProfileRepository districtAdminProfileRepository,
            LocationService locationService,
            SchoolRepository schoolRepository,
            SchoolRegistrationRequestRepository schoolRegistrationRequestRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService,
            AuthOtpProperties authOtpProperties,
            ConsentService consentService,
            GamificationService gamificationService,
            StudentProfileCompletionService studentProfileCompletionService,
            GoogleTokenVerifierService googleTokenVerifierService,
            PlatformSettingsService platformSettingsService,
            SubscriptionService subscriptionService,
            StudentPlanAccessService studentPlanAccessService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.districtRepository = districtRepository;
        this.districtAdminProfileRepository = districtAdminProfileRepository;
        this.locationService = locationService;
        this.schoolRepository = schoolRepository;
        this.schoolRegistrationRequestRepository = schoolRegistrationRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.authOtpProperties = authOtpProperties;
        this.consentService = consentService;
        this.gamificationService = gamificationService;
        this.studentProfileCompletionService = studentProfileCompletionService;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.platformSettingsService = platformSettingsService;
        this.subscriptionService = subscriptionService;
        this.studentPlanAccessService = studentPlanAccessService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AuthResponse me(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new InvalidCredentialsException();
        }

        String email = normalizeEmail(principal.getName());

        User user = findUserByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        validateUserEligibleForSession(user, false);

        return issueAuthResponse(user);
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        ensureStudentRegistrationAllowed();

        User user = createUser(
                request.email(),
                request.phoneNumber(),
                request.password(),
                request.firstName(),
                request.lastName(),
                "ROLE_STUDENT"
        );

        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setPhone(user.getPhoneNumber());
        profile.setProfileCompleted(false);
        studentProfileRepository.save(profile);
        subscriptionService.initializeStudentTrialIfAbsent(user.getId());

        if (isOtpRequired()) {
            user.setEmailVerified(false);
            userRepository.save(user);
        }

        consentService.recordPopiaConsent(user, null);
        return buildRegistrationResponse(user);
    }

    @Transactional
    public RegistrationResponse registerStudent(StudentRegisterRequest request) {
        ensureStudentRegistrationAllowed();

        String[] names = splitFullName(request.fullName());
        String resolvedFirstName = Optional.ofNullable(trimToNull(request.firstName())).orElse(names[0]);
        String resolvedLastName = Optional.ofNullable(trimToNull(request.lastName())).orElse(names[1]);

        User user = createUser(
                request.email(),
                request.phone(),
                request.password(),
                resolvedFirstName,
                resolvedLastName,
                "ROLE_STUDENT"
        );

        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setInterests(trimToNull(request.interests()));
        profile.setLocation(trimToNull(request.location()));
        profile.setPhone(normalizePhoneNumber(request.phone()));
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(trimToNull(request.gender()));
        profile.setQualificationLevel(trimToNull(request.qualificationLevel()));
        profile.setProfileCompleted(false);
        studentProfileRepository.save(profile);
        subscriptionService.initializeStudentTrialIfAbsent(user.getId());

        if (isOtpRequired()) {
            user.setEmailVerified(false);
            userRepository.save(user);
        }

        consentService.recordPopiaConsent(user, request.consentVersion());
        return buildRegistrationResponse(user);
    }

    @Transactional
    public RegistrationResponse registerCompany(CompanyRegisterRequest request) {
        ensureCompanyRegistrationAllowed();

        String officialEmail = trimToNull(request.officialEmail());
        if (officialEmail == null) {
            officialEmail = request.email();
        }

        officialEmail = normalizeEmail(officialEmail);
        if (officialEmail == null || officialEmail.isBlank()) {
            throw new ResourceConflictException("Company email is required");
        }

        String normalizedMobile = normalizePhoneNumber(request.mobileNumber());
        if (normalizedMobile == null || normalizedMobile.isBlank()) {
            throw new ResourceConflictException("Company mobile number is required");
        }

        String companyName = trimToNull(request.companyName());
        if (companyName == null) {
            throw new ResourceConflictException("Company name is required");
        }

        String contactPersonName = trimToNull(request.contactPersonName());
        if (contactPersonName == null) {
            contactPersonName = companyName;
        }

        String registrationNumber = trimToNull(request.registrationNumber());
        if (registrationNumber == null) {
            registrationNumber = "PENDING-" + UUID.randomUUID();
        }

        if (companyProfileRepository.existsByRegistrationNumberIgnoreCase(registrationNumber)) {
            throw new ResourceConflictException("Company registration number already exists");
        }

        User user = createUser(
                officialEmail,
                normalizedMobile,
                request.password(),
                contactPersonName,
                companyName,
                "ROLE_COMPANY"
        );

        CompanyProfile profile = new CompanyProfile();
        profile.setUserId(user.getId());
        profile.setCompanyName(companyName);
        profile.setRegistrationNumber(registrationNumber);
        profile.setIndustry(request.industry());
        profile.setOfficialEmail(officialEmail);
        profile.setMobileNumber(normalizedMobile);
        profile.setContactPersonName(contactPersonName);
        profile.setAddress(request.address());
        profile.setWebsite(request.website());
        profile.setDescription(request.description());

        PlatformSetting settings = platformSettingsService.getCurrentSettingsEntity();
        profile.setStatus(settings.isManualCompanyApprovalRequired()
                ? CompanyApprovalStatus.PENDING
                : CompanyApprovalStatus.APPROVED);

        if (!settings.isManualCompanyApprovalRequired()) {
            profile.setReviewedAt(OffsetDateTime.now());
            profile.setReviewNotes("Auto-approved by platform setting");
        }

        companyProfileRepository.save(profile);

        if (isOtpRequired()) {
            user.setEmailVerified(false);
            userRepository.save(user);
        }

        consentService.recordPopiaConsent(user, request.consentVersion());
        return buildRegistrationResponse(user);
    }

    @Transactional
    public RegistrationResponse registerSchool(SchoolRegisterRequest request) {
        ensureSchoolRegistrationAllowed();

        String schoolName = requireValue(request.schoolName(), "School name is required");
        String emisNumber = requireValue(request.emisNumber(), "EMIS number is required").toUpperCase(Locale.ROOT);
        District district = locationService.requireActiveDistrict(request.districtId());
        Circuit circuit = locationService.requireActiveCircuit(request.circuitId(), district.getId());
        String schoolType = requireValue(request.schoolType(), "School type is required");
        String principalName = requireValue(request.principalName(), "Principal name is required");
        String principalEmail = normalizeEmail(request.principalEmail());
        String schoolEmail = normalizeEmail(request.schoolEmail());
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        String physicalAddress = requireValue(request.physicalAddress(), "Physical address is required");

        if (principalEmail == null || principalEmail.isBlank()) {
            throw new ResourceConflictException("Principal email is required");
        }
        if (schoolEmail == null || schoolEmail.isBlank()) {
            throw new ResourceConflictException("School email is required");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ResourceConflictException("Password and confirm password must match");
        }
        if (schoolRegistrationRequestRepository.existsByEmisNumberIgnoreCase(emisNumber)
                || schoolRepository.existsByRegistrationNumberIgnoreCase(emisNumber)) {
            throw new ResourceConflictException("EMIS number already exists");
        }

        User user = createUser(
                schoolEmail,
                phoneNumber,
                request.password(),
                schoolName,
                "School Admin",
                "ROLE_SCHOOL_ADMIN"
        );

        SchoolRegistrationRequest registration = new SchoolRegistrationRequest();
        registration.setUserId(user.getId());
        registration.setDistrictId(district.getId());
        registration.setCircuitId(circuit.getId());
        registration.setSchoolName(schoolName);
        registration.setEmisNumber(emisNumber);
        registration.setProvince(trimToNull(district.getProvince()));
        registration.setDistrictName(district.getDistrictName());
        registration.setCircuit(circuit.getName());
        registration.setSchoolType(schoolType);
        registration.setPrincipalName(principalName);
        registration.setPrincipalEmail(principalEmail);
        registration.setSchoolEmail(schoolEmail);
        registration.setPhoneNumber(phoneNumber);
        registration.setPhysicalAddress(physicalAddress);
        registration.setStatus(SchoolStatus.PENDING_DISTRICT_APPROVAL);
        registration.setSubmittedAt(OffsetDateTime.now());
        schoolRegistrationRequestRepository.save(registration);

        districtAdminProfileRepository.findByDistrictIdAndActiveTrueAndDeletedFalse(district.getId())
                .forEach(profile -> notificationService.createInApp(
                        profile.getUserId(),
                        "SCHOOL_REGISTRATION_REQUEST",
                        "New school registration request",
                        schoolName + " (" + emisNumber + ") submitted a district join request."
                ));
        notificationService.createInApp(
                user.getId(),
                "SCHOOL_REGISTRATION_SUBMITTED",
                "School registration submitted",
                "Your registration was submitted to " + district.getDistrictName() + " and is awaiting district approval."
        );

        return new RegistrationResponse(
                "School registration submitted successfully. You can sign in and track district approval.",
                user.getEmail(),
                false
        );
    }

    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidCredentialsException();
        }

        String username;
        try {
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new InvalidCredentialsException();
            }
            username = jwtService.extractUsername(refreshToken);
        } catch (RuntimeException ex) {
            throw new InvalidCredentialsException();
        }

        User user = findUserByEmail(username)
                .orElseThrow(InvalidCredentialsException::new);

        validateUserEligibleForSession(user, true);

        Set<String> effectiveRoles = resolveEffectiveRoles(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .authorities(effectiveRoles.toArray(String[]::new))
                .build();

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new InvalidCredentialsException();
        }

        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = authenticateActiveUser(request);

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        try {
            gamificationService.awardLoginPoints(user);
        } catch (RuntimeException ex) {
            log.warn("[auth] login points award failed userId={} reason={}", user.getId(), ex.getMessage());
        }

        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdentity identity = googleTokenVerifierService.verifyIdToken(request.idToken());
        String normalizedEmail = normalizeEmail(identity.email());
        String requestedRole = resolveGoogleRequestedRole(request);

        User user = findUserByEmail(normalizedEmail)
                .map(existing -> activateExistingGoogleUser(existing, identity, requestedRole))
                .orElseGet(() -> provisionGoogleUser(identity, requestedRole));

        validateUserEligibleForSession(user, false);

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        try {
            gamificationService.awardLoginPoints(user);
        } catch (RuntimeException ex) {
            log.warn("[auth] google login points award failed userId={} reason={}", user.getId(), ex.getMessage());
        }

        return issueAuthResponse(user);
    }

    @Transactional
    public VerificationStatusResponse resendRegistrationOtp(ResendVerificationOtpRequest request) {
        if (!isOtpRequired()) {
            return new VerificationStatusResponse("OTP verification is currently disabled. You can sign in.");
        }

        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());

        User user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(() -> new InvalidOtpException("No account found for this phone number."));

        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new InvalidOtpException("No active account found for this phone number.");
        }

        if (user.isEmailVerified()) {
            return new VerificationStatusResponse("Account is already verified. You can sign in.");
        }

        otpService.sendVerificationOtp(normalizedPhone);
        return new VerificationStatusResponse("Verification OTP sent.");
    }

    @Transactional
    public VerificationStatusResponse verifyRegistrationOtp(VerifyOtpRequest request) {
        if (!isOtpRequired()) {
            return new VerificationStatusResponse("OTP verification is currently disabled. You can sign in.");
        }

        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());

        User user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(() -> new InvalidOtpException("Invalid phone number or OTP code"));

        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new InvalidCredentialsException();
        }

        if (user.isEmailVerified()) {
            return new VerificationStatusResponse("Account already verified. You can sign in.");
        }

        boolean approved = otpService.verifyVerificationOtp(normalizedPhone, request.code().trim());

        if (!approved) {
            throw new InvalidOtpException("Invalid or expired OTP code");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        return new VerificationStatusResponse("Phone verification complete. You can sign in.");
    }

    @Transactional
    public VerificationStatusResponse requestPasswordResetOtp(ForgotPasswordOtpRequest request) {
        if (!isOtpRequired()) {
            return new VerificationStatusResponse("OTP verification is currently disabled. You can sign in.");
        }

        String identifier = request.resolvedIdentifier();
        Optional<User> user = findUserByAccountIdentifier(identifier);

        log.info("[auth] forgot-password OTP requested identifier={} userFound={}", identifier, user.isPresent());

        if (user.isPresent()) {
            String normalizedPhone = normalizePhoneNumber(user.get().getPhoneNumber());

            if (normalizedPhone == null) {
                return new VerificationStatusResponse("Account exists, but no phone number is registered.");
            }

            try {
                otpService.sendPasswordResetOtp(normalizedPhone);
            } catch (RuntimeException ex) {
                log.warn("[auth] forgot-password otp dispatch failed phone={} reason={}", normalizedPhone, ex.getMessage());
                return new VerificationStatusResponse("Account exists, but we could not send OTP right now. Please try again.");
            }
        }

        return new VerificationStatusResponse(GENERIC_FORGOT_PASSWORD_OTP_MESSAGE);
    }

    @Transactional
    public VerificationStatusResponse resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());

        User user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(() -> new InvalidOtpException("Invalid phone number or OTP code"));

        boolean approved = otpService.verifyPasswordResetOtp(normalizedPhone, request.code().trim());

        if (!approved) {
            throw new InvalidOtpException("Invalid or expired OTP code");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return new VerificationStatusResponse("Password reset complete");
    }

    @Transactional(readOnly = true)
    public Map<String, String> keepAlive(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new InvalidCredentialsException();
        }

        String email = normalizeEmail(principal.getName());
        User user = findUserByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        validateUserEligibleForSession(user, false);
        log.info("[auth] keep-alive accepted email={}", user.getEmail());

        return Map.of(
                "status", "ok",
                "message", "Session is active",
                "serverTime", OffsetDateTime.now().toString()
        );
    }

    @Transactional
    public VerificationStatusResponse resetPasswordForDev(DevPasswordResetRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ResourceConflictException("Email is required");
        }

        User user = findUserByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("[auth-dev] password reset applied email={} userId={}", maskIdentifier(normalizedEmail), user.getId());
        return new VerificationStatusResponse("Password updated");
    }

    private User authenticateActiveUser(LoginRequest request) {
        String schoolName = trimToNull(request.resolvedSchoolName());
        String emisNumber = trimToNull(request.resolvedEmisNumber());
        String identifier = request.resolvedIdentifier();
        User resolvedUser;

        if (schoolName != null || emisNumber != null) {
            if (schoolName == null || emisNumber == null) {
                throw new InvalidCredentialsException("School name and EMIS number are required.");
            }
            resolvedUser = resolveSchoolUserForLogin(schoolName, emisNumber);
            identifier = emisNumber;
        } else {
            resolvedUser = resolveUserForLoginIdentifier(identifier);
        }

        if (request.password() == null
                || resolvedUser.getPasswordHash() == null
                || resolvedUser.getPasswordHash().isBlank()
                || !passwordEncoder.matches(request.password(), resolvedUser.getPasswordHash())) {
            throw loginFailure(LoginAuditReason.PASSWORD_MISMATCH, identifier);
        }

        validateUserEligibleForLogin(resolvedUser, identifier);
        return resolvedUser;
    }

    private RegistrationResponse buildRegistrationResponse(User user) {
        if (!isOtpRequired()) {
            return new RegistrationResponse("Account created successfully. You can sign in now.", user.getEmail(), false);
        }

        String normalizedPhone = normalizePhoneNumber(user.getPhoneNumber());

        if (normalizedPhone == null) {
            return new RegistrationResponse(
                    "Account created, but no phone number is registered for OTP verification. Contact support.",
                    user.getEmail(),
                    true
            );
        }

        try {
            otpService.sendVerificationOtp(normalizedPhone);
            return new RegistrationResponse(
                    "Account created successfully. Verify your phone number using OTP before signing in.",
                    user.getEmail(),
                    true
            );
        } catch (RuntimeException ex) {
            log.warn("[auth] registration otp dispatch failed phone={} reason={}", normalizedPhone, ex.getMessage());
            return new RegistrationResponse(
                    "Account created, but we could not send OTP right now. Please use resend OTP to verify your phone number.",
                    user.getEmail(),
                    true
            );
        }
    }

    private User createUser(
            String email,
            String phoneNumber,
            String password,
            String firstName,
            String lastName,
            String roleName
    ) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ResourceConflictException("Email is required");
        }

        if (password == null || password.isBlank()) {
            throw new ResourceConflictException("Password is required");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        if (normalizedPhone != null && userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw new ResourceConflictException("An account with phone number '%s' already exists".formatted(normalizedPhone));
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceConflictException("Role '%s' does not exist".formatted(roleName)));

        String safeFirstName = Optional.ofNullable(trimToNull(firstName)).orElse("EduRite");
        String safeLastName = Optional.ofNullable(trimToNull(lastName)).orElse("User");

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(normalizedPhone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(safeFirstName);
        user.setLastName(safeLastName);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPlanType(PlanType.BASIC);
        user.getRoles().add(role);

        return userRepository.save(user);
    }

    private String resolveGoogleRequestedRole(GoogleLoginRequest request) {
        String requestedRole = request.resolvedRole();

        if (!"STUDENT".equals(requestedRole) && !"COMPANY".equals(requestedRole)) {
            throw new InvalidCredentialsException("Google sign-in is available for student and company accounts only.");
        }

        return requestedRole;
    }

    private User activateExistingGoogleUser(User existing, GoogleIdentity identity, String requestedRole) {
        if (existing.getStatus() == UserStatus.DELETED) {
            throw new InvalidCredentialsException("Unable to sign in with Google.");
        }

        boolean hasStudentRole = hasRole(existing, "ROLE_STUDENT");
        boolean hasCompanyRole = hasRole(existing, "ROLE_COMPANY");
        boolean hasAdminRole = hasRole(existing, "ROLE_ADMIN");
        Optional<CompanyProfile> companyProfile = companyProfileRepository.findByUserId(existing.getId());

        boolean googleEligibleAccount = hasStudentRole || hasCompanyRole || companyProfile.isPresent();

        if (hasAdminRole || !googleEligibleAccount) {
            throw new InvalidCredentialsException("Google sign-in is available for student and company accounts only.");
        }

        boolean changed = false;

        if (!existing.isEmailVerified()) {
            existing.setEmailVerified(true);
            changed = true;
        }

        if (existing.getStatus() != UserStatus.ACTIVE) {
            existing.setStatus(UserStatus.ACTIVE);
            changed = true;
        }

        if (trimToNull(existing.getFirstName()) == null && trimToNull(identity.firstName()) != null) {
            existing.setFirstName(identity.firstName().trim());
            changed = true;
        }

        if (trimToNull(existing.getLastName()) == null && trimToNull(identity.lastName()) != null) {
            existing.setLastName(identity.lastName().trim());
            changed = true;
        }

        if (changed) {
            existing = userRepository.save(existing);
        }

        boolean effectiveCompany = hasCompanyRole || companyProfile.isPresent();

        if (effectiveCompany) {
            ensureGoogleCompanyProfile(existing, identity, requestedRole, companyProfile);
        } else if (studentProfileRepository.findByUserId(existing.getId()).isEmpty()) {
            StudentProfile profile = new StudentProfile();
            profile.setUserId(existing.getId());
            profile.setFirstName(Optional.ofNullable(trimToNull(existing.getFirstName())).orElse("EduRite"));
            profile.setLastName(Optional.ofNullable(trimToNull(existing.getLastName())).orElse("User"));
            profile.setProfileCompleted(false);
            studentProfileRepository.save(profile);
        }

        return existing;
    }

    private User provisionGoogleUser(GoogleIdentity identity, String requestedRole) {
        return "COMPANY".equals(requestedRole)
                ? provisionGoogleCompany(identity)
                : provisionGoogleStudent(identity);
    }

    private User provisionGoogleStudent(GoogleIdentity identity) {
        ensureStudentRegistrationAllowed();

        User user = createUser(
                identity.email(),
                null,
                UUID.randomUUID() + "#Google1!",
                Optional.ofNullable(trimToNull(identity.firstName())).orElse("EduRite"),
                Optional.ofNullable(trimToNull(identity.lastName())).orElse("User"),
                "ROLE_STUDENT"
        );

        user.setEmailVerified(true);
        user = userRepository.save(user);

        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setProfileCompleted(false);
        studentProfileRepository.save(profile);
        subscriptionService.initializeStudentTrialIfAbsent(user.getId());

        consentService.recordPopiaConsent(user, null);

        return user;
    }

    private User provisionGoogleCompany(GoogleIdentity identity) {
        ensureCompanyRegistrationAllowed();

        String companyName = resolveGoogleCompanyName(identity);
        String contactPersonName = Optional.ofNullable(trimToNull(identity.fullName())).orElse(companyName);

        User user = createUser(
                identity.email(),
                null,
                UUID.randomUUID() + "#Google1!",
                contactPersonName,
                companyName,
                "ROLE_COMPANY"
        );

        user.setEmailVerified(true);
        user = userRepository.save(user);

        CompanyProfile profile = new CompanyProfile();
        profile.setUserId(user.getId());
        profile.setCompanyName(companyName);
        profile.setRegistrationNumber("GOOGLE-" + UUID.randomUUID());
        profile.setOfficialEmail(user.getEmail());
        profile.setContactPersonName(contactPersonName);
        profile.setEmailVerified(true);
        profile.setMobileVerified(false);
        profile.setDescription("Created with Google sign-in.");

        PlatformSetting settings = platformSettingsService.getCurrentSettingsEntity();
        profile.setStatus(settings.isManualCompanyApprovalRequired()
                ? CompanyApprovalStatus.PENDING
                : CompanyApprovalStatus.APPROVED);

        if (!settings.isManualCompanyApprovalRequired()) {
            profile.setReviewedAt(OffsetDateTime.now());
            profile.setReviewNotes("Auto-approved by platform setting");
        }

        companyProfileRepository.save(profile);
        consentService.recordPopiaConsent(user, null);

        return user;
    }

    private void ensureGoogleCompanyProfile(
            User user,
            GoogleIdentity identity,
            String requestedRole,
            Optional<CompanyProfile> existingCompanyProfile
    ) {
        if (existingCompanyProfile.isPresent()) {
            CompanyProfile profile = existingCompanyProfile.get();
            boolean changed = false;

            if (!profile.isEmailVerified()) {
                profile.setEmailVerified(true);
                changed = true;
            }

            if (trimToNull(profile.getOfficialEmail()) == null) {
                profile.setOfficialEmail(user.getEmail());
                changed = true;
            }

            if (changed) {
                companyProfileRepository.save(profile);
            }
            return;
        }

        if (!"COMPANY".equals(requestedRole)) {
            return;
        }

        CompanyProfile profile = new CompanyProfile();
        profile.setUserId(user.getId());
        profile.setCompanyName(resolveGoogleCompanyName(identity));
        profile.setRegistrationNumber("GOOGLE-" + UUID.randomUUID());
        profile.setOfficialEmail(user.getEmail());
        profile.setContactPersonName(Optional.ofNullable(trimToNull(identity.fullName())).orElse(user.getFirstName()));
        profile.setEmailVerified(true);
        profile.setMobileVerified(false);
        profile.setStatus(CompanyApprovalStatus.PENDING);
        profile.setDescription("Created with Google sign-in.");
        companyProfileRepository.save(profile);
    }

    private String resolveGoogleCompanyName(GoogleIdentity identity) {
        String fullName = trimToNull(identity.fullName());

        if (fullName != null) {
            return fullName;
        }

        String email = trimToNull(identity.email());
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }

        return "Google Company";
    }

    public AuthResponse issueAuthResponse(User user) {
        return issueAuthResponse(user, null);
    }

    public AuthResponse issueAuthResponse(User user, String message) {
        Set<String> roles = resolveEffectiveRoles(user);

        CompanyProfile companyProfile = companyProfileRepository.findByUserId(user.getId()).orElse(null);
        SchoolRegistrationRequest schoolRegistration = schoolRegistrationRequestRepository == null ? null : schoolRegistrationRequestRepository.findByUserId(user.getId()).orElse(null);
        StudentProfile studentProfile = studentProfileRepository.findByUserId(user.getId()).orElse(null);

        String approvalStatus = companyProfile != null
                ? companyProfile.getStatus().name()
                : schoolRegistration == null ? null : schoolRegistration.getStatus().name();
        String primaryRole = resolvePrimaryRole(roles);
        String planType = studentPlanAccessService.hasPremiumAccess(user.getId()) ? PlanType.PREMIUM.name() : PlanType.BASIC.name();

        String accessToken = jwtService.generateAccessToken(user, roles, approvalStatus, planType);
        String refreshToken = jwtService.generateRefreshToken(user);

        String companyName = companyProfile == null ? null : companyProfile.getCompanyName();
        String schoolName = schoolRegistration == null ? null : schoolRegistration.getSchoolName();
        String role = primaryRole == null ? null : primaryRole.replace("ROLE_", "");

        Boolean profileCompleted = studentProfile == null
                ? null
                : studentProfileCompletionService.isProfileCompleted(studentProfile);

        Integer profileCompleteness = studentProfile == null
                ? null
                : studentProfileCompletionService.calculateCompleteness(studentProfile);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.accessTokenExpirationSeconds(),
                role,
                primaryRole,
                approvalStatus,
                user.isMustChangePassword(),
                new AuthResponse.UserSummary(
                        user.getId(),
                        user.getEmail(),
                        "%s %s".formatted(
                                Optional.ofNullable(user.getFirstName()).orElse(""),
                                Optional.ofNullable(user.getLastName()).orElse("")
                        ).trim(),
                        companyName,
                        schoolName,
                        user.getUsername(),
                        roles,
                        role,
                        primaryRole,
                        approvalStatus,
                        user.isEmailVerified(),
                        planType,
                        user.isMustChangePassword(),
                        profileCompleted,
                        profileCompleteness
                ),
                message
        );
    }

    private User resolveUserForLoginIdentifier(String identifier) {
        String normalized = trimToNull(identifier);

        if (normalized == null) {
            throw loginFailure(LoginAuditReason.USER_NOT_FOUND, identifier);
        }

        Optional<User> byEmail = findUserByEmail(normalized);

        if (byEmail.isPresent()) {
            return byEmail.get();
        }

        Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(normalized);

        if (byUsername.isPresent()) {
            return byUsername.get();
        }

        String normalizedPhone = tryNormalizePhoneNumber(normalized);

        if (normalizedPhone != null) {
            Optional<User> byPhone = userRepository.findByPhoneNumber(normalizedPhone);

            if (byPhone.isPresent()) {
                return byPhone.get();
            }
        }

        if (!normalized.contains("@")) {
            List<User> matches = userRepository.findAllByEmailLocalPart(normalized.toLowerCase(Locale.ROOT));

        if (matches.size() == 1) {
            return matches.getFirst();
        }

            if (matches.size() > 1) {
                throw loginFailure(LoginAuditReason.USER_NOT_FOUND, normalized);
            }
        }

        throw loginFailure(LoginAuditReason.USER_NOT_FOUND, normalized);
    }

    private void validateUserEligibleForLogin(User user, String identifier) {
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw loginFailure(LoginAuditReason.ACCOUNT_DISABLED, identifier);
        }

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw loginFailure(LoginAuditReason.ROLE_MISSING, identifier);
        }

        if (isOtpRequired() && !user.isEmailVerified()) {
            throw loginFailure(LoginAuditReason.EMAIL_NOT_VERIFIED, identifier);
        }

        CompanyProfile companyProfile = companyProfileRepository.findByUserId(user.getId()).orElse(null);
        SchoolRegistrationRequest schoolRegistration = schoolRegistrationRequestRepository == null ? null : schoolRegistrationRequestRepository.findByUserId(user.getId()).orElse(null);
        boolean hasAdminRole = hasRole(user, "ROLE_ADMIN");

        if (companyProfile != null && !hasAdminRole) {
            if (companyProfile.getDeletedAt() != null || companyProfile.getStatus() == CompanyApprovalStatus.SUSPENDED) {
                throw loginFailure(LoginAuditReason.ACCOUNT_DISABLED, identifier);
            }

            if (companyProfile.getStatus() == CompanyApprovalStatus.PENDING) {
                log.info("[auth] login allowed reason={} identifier={}", LoginAuditReason.APPROVAL_PENDING, maskIdentifier(identifier));
            }
        }

        if (schoolRegistration != null && !hasAdminRole && schoolRegistration.getStatus() == SchoolStatus.SUSPENDED) {
            throw loginFailure(LoginAuditReason.ACCOUNT_DISABLED, identifier);
        }
    }

    private void validateUserEligibleForSession(User user, boolean requireVerified) {
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new InvalidCredentialsException("Account is inactive or suspended.");
        }

        if (requireVerified && isOtpRequired() && !user.isEmailVerified()) {
            throw new InvalidCredentialsException("Account is not verified. Verify your phone number with OTP before signing in.");
        }

        CompanyProfile companyProfile = companyProfileRepository.findByUserId(user.getId()).orElse(null);
        SchoolRegistrationRequest schoolRegistration = schoolRegistrationRequestRepository == null ? null : schoolRegistrationRequestRepository.findByUserId(user.getId()).orElse(null);
        boolean hasAdminRole = hasRole(user, "ROLE_ADMIN");

        if (companyProfile != null && !hasAdminRole) {
            if (companyProfile.getDeletedAt() != null) {
                throw new InvalidCredentialsException("Company account is inactive.");
            }

            if (companyProfile.getStatus() == CompanyApprovalStatus.SUSPENDED) {
                throw new InvalidCredentialsException("Company account is suspended.");
            }
        }

        if (schoolRegistration != null && !hasAdminRole && schoolRegistration.getStatus() == SchoolStatus.SUSPENDED) {
            throw new InvalidCredentialsException("School account is suspended.");
        }
    }

    private Optional<User> findUserByAccountIdentifier(String identifier) {
        String normalized = trimToNull(identifier);

        if (normalized == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(resolveOptionalUserIdentifier(normalized));
    }

    private User resolveOptionalUserIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return findUserByEmail(identifier).orElse(null);
        }

        User byUsername = userRepository.findByUsernameIgnoreCase(identifier).orElse(null);
        if (byUsername != null) {
            return byUsername;
        }

        String normalizedPhone = tryNormalizePhoneNumber(identifier);

        if (normalizedPhone != null) {
            return userRepository.findByPhoneNumber(normalizedPhone).orElse(null);
        }

        List<User> localPartMatches = userRepository.findAllByEmailLocalPart(identifier.toLowerCase(Locale.ROOT));

        if (localPartMatches.size() == 1) {
            return localPartMatches.getFirst();
        }

        return null;
    }

    private Optional<User> findUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmailIgnoreCase(normalizedEmail);
    }

    private InvalidCredentialsException loginFailure(LoginAuditReason reason, String identifier) {
        log.warn("[auth] login failed reason={} identifier={}", reason, maskIdentifier(identifier));
        return switch (reason) {
            case ACCOUNT_DISABLED -> new InvalidCredentialsException("Account is inactive or suspended.");
            case EMAIL_NOT_VERIFIED -> new InvalidCredentialsException("Account is not verified. Verify your phone number with OTP before signing in.");
            default -> new InvalidCredentialsException();
        };
    }

    private User resolveSchoolUserForLogin(String schoolName, String emisNumber) {
        SchoolRegistrationRequest request = schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase(emisNumber)
                .filter(item -> schoolName.equalsIgnoreCase(item.getSchoolName()))
                .orElseThrow(InvalidCredentialsException::new);
        return userRepository.findById(request.getUserId())
                .orElseThrow(InvalidCredentialsException::new);
    }

    private String maskIdentifier(String identifier) {
        String normalized = trimToNull(identifier);

        if (normalized == null) {
            return "<blank>";
        }

        if (normalized.contains("@")) {
            String[] parts = normalized.split("@", 2);
            String local = parts[0];
            String domain = parts.length > 1 ? parts[1] : "";
            String maskedLocal = local.isBlank() ? "***" : local.charAt(0) + "***";
            return maskedLocal + "@" + domain.toLowerCase(Locale.ROOT);
        }

        if (normalized.length() <= 4) {
            return "****";
        }

        return normalized.substring(0, 2) + "***" + normalized.substring(normalized.length() - 2);
    }

    private Set<String> resolveEffectiveRoles(User user) {
        LinkedHashSet<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(this::normalizeRoleName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        boolean hasAdminRole = roles.contains("ROLE_ADMIN");

        if (companyProfileRepository.findByUserId(user.getId()).isPresent() && !hasAdminRole) {
            roles.remove("ROLE_STUDENT");
            roles.add("ROLE_COMPANY");
        }

        return roles;
    }

    private String resolvePrimaryRole(Set<String> roles) {
        return roles.stream()
                .min(Comparator.comparingInt(role -> {
                    int index = ROLE_PRIORITY.indexOf(role);
                    return index >= 0 ? index : Integer.MAX_VALUE;
                }))
                .orElse(null);
    }

    private boolean hasRole(User user, String roleName) {
        String normalized = normalizeRoleName(roleName);

        return user.getRoles().stream()
                .map(Role::getName)
                .map(this::normalizeRoleName)
                .anyMatch(normalized::equals);
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String compact = phoneNumber.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (compact.isEmpty()) {
            return null;
        }

        if (compact.startsWith("00")) {
            compact = "+" + compact.substring(2);
        }

        if (compact.matches("\\d{8}")) {
            compact = "267" + compact;
        }

        if (compact.matches("[1-9]\\d{7,14}")) {
            compact = "+" + compact;
        }

        if (!compact.matches("\\+[1-9]\\d{7,14}")) {
            throw new InvalidPhoneNumberException("Phone number must be in E.164 format, e.g. +26775314557");
        }

        return compact;
    }

    private String tryNormalizePhoneNumber(String value) {
        try {
            return normalizePhoneNumber(value);
        } catch (InvalidPhoneNumberException ex) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isOtpRequired() {
        return authOtpProperties.enabled();
    }

    private String[] splitFullName(String fullName) {
        String normalized = trimToNull(fullName);

        if (normalized == null) {
            return new String[]{"EduRite", "User"};
        }

        String[] parts = normalized.split("\\s+", 2);

        if (parts.length == 1) {
            return new String[]{parts[0], "User"};
        }

        return parts;
    }

    private void ensureStudentRegistrationAllowed() {
        PlatformSetting settings = platformSettingsService.getCurrentSettingsEntity();

        if (settings.isMaintenanceModeEnabled()) {
            throw new ResourceConflictException("Student registration is temporarily unavailable while maintenance mode is active");
        }

        if (!settings.isStudentRegistrationEnabled()) {
            throw new ResourceConflictException("Student registration is currently disabled by system settings");
        }
    }

    private void ensureCompanyRegistrationAllowed() {
        PlatformSetting settings = platformSettingsService.getCurrentSettingsEntity();

        if (settings.isMaintenanceModeEnabled()) {
            throw new ResourceConflictException("Company registration is temporarily unavailable while maintenance mode is active");
        }

        if (!settings.isCompanySelfRegistrationEnabled()) {
            throw new ResourceConflictException("Company self-registration is currently disabled by system settings");
        }
    }

    private void ensureSchoolRegistrationAllowed() {
        PlatformSetting settings = platformSettingsService.getCurrentSettingsEntity();

        if (settings.isMaintenanceModeEnabled()) {
            throw new ResourceConflictException("School registration is temporarily unavailable while maintenance mode is active");
        }
    }

    private String requireValue(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ResourceConflictException(message);
        }
        return trimmed;
    }
}

