package com.edurite.auth;

import com.edurite.auth.config.AuthOtpProperties;
import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.ForgotPasswordOtpRequest;
import com.edurite.auth.dto.DevPasswordResetRequest;
import com.edurite.auth.dto.GoogleIdentity;
import com.edurite.auth.dto.GoogleLoginRequest;
import com.edurite.auth.dto.RegistrationResponse;
import com.edurite.auth.dto.ResetPasswordWithOtpRequest;
import com.edurite.auth.dto.StudentRegisterRequest;
import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.auth.exception.InvalidOtpException;
import com.edurite.auth.service.AuthService;
import com.edurite.auth.service.GoogleTokenVerifierService;
import com.edurite.auth.service.OtpService;
import com.edurite.admin.entity.PlatformSetting;
import com.edurite.admin.service.PlatformSettingsService;
import com.edurite.compliance.service.ConsentService;
import com.edurite.common.exception.DuplicateEmailException;
import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.district.service.LocationService;
import com.edurite.gamification.service.GamificationService;
import com.edurite.notification.service.NotificationService;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.security.service.JwtService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentProfileCompletionService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.subscription.service.SubscriptionService;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceVerificationFlowUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private DistrictAdminProfileRepository districtAdminProfileRepository;
    @Mock
    private LocationService locationService;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private OtpService otpService;
    @Mock
    private ConsentService consentService;
    @Mock
    private GamificationService gamificationService;
    @Mock
    private StudentProfileCompletionService studentProfileCompletionService;
    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;
    @Mock
    private PlatformSettingsService platformSettingsService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private StudentPlanAccessService studentPlanAccessService;
    @Mock
    private NotificationService notificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                studentProfileRepository,
                companyProfileRepository,
                districtRepository,
                districtAdminProfileRepository,
                locationService,
                schoolRepository,
                schoolRegistrationRequestRepository,
                passwordEncoder,
                jwtService,
                otpService,
                new AuthOtpProperties(true),
                consentService,
                gamificationService,
                studentProfileCompletionService,
                googleTokenVerifierService,
                platformSettingsService,
                subscriptionService,
                studentPlanAccessService,
                notificationService
        );

        PlatformSetting settings = new PlatformSetting();
        settings.setStudentRegistrationEnabled(true);
        settings.setCompanySelfRegistrationEnabled(true);
        settings.setMaintenanceModeEnabled(false);
        settings.setManualCompanyApprovalRequired(true);
        lenient().when(platformSettingsService.getCurrentSettingsEntity()).thenReturn(settings);
        lenient().when(subscriptionService.initializeStudentTrialIfAbsent(any())).thenReturn(null);
        lenient().when(studentPlanAccessService.hasPremiumAccess(any())).thenReturn(false);
    }

    @Test
    void registrationCreatesActiveUserWithOtpDispatch() {
        String email = "student@example.com";
        String phone = "+26770000000";
        Role studentRole = new Role();
        studentRole.setName("ROLE_STUDENT");

        when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(false);
        when(userRepository.existsByPhoneNumber(phone)).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = authService.registerStudent(studentRequest(email, phone));

        assertThat(response.verificationRequired()).isTrue();
        assertThat(response.message()).contains("Account created successfully");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(createdUser.isEmailVerified()).isFalse();
        assertThat(createdUser.getPhoneNumber()).isEqualTo(phone);
        verify(otpService, times(1)).sendVerificationOtp(phone);
        verify(subscriptionService, times(1)).initializeStudentTrialIfAbsent(any(UUID.class));
    }

    @Test
    void registeredVerifiedStudentCanLoginWithSameEmailAndPasswordWithoutPasswordReset() {
        String email = "NewStudent@Example.COM";
        String normalizedEmail = "newstudent@example.com";
        String phone = "+26770000002";
        Role studentRole = new Role();
        studentRole.setName("ROLE_STUDENT");

        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(userRepository.existsByPhoneNumber(phone)).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("StrongPass@123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.registerStudent(studentRequest(email, phone));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        createdUser.setEmailVerified(true);

        when(userRepository.findByEmailIgnoreCase(normalizedEmail)).thenReturn(Optional.of(createdUser));
        when(passwordEncoder.matches("StrongPass@123", "encoded-password")).thenReturn(true);
        when(companyProfileRepository.findByUserId(createdUser.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(createdUser.getId())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(eq(createdUser), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(createdUser)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new com.edurite.auth.dto.LoginRequest(email, "StrongPass@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(createdUser.getEmail()).isEqualTo(normalizedEmail);
        verify(passwordEncoder).encode("StrongPass@123");
        verify(passwordEncoder).matches("StrongPass@123", "encoded-password");
        verify(otpService, never()).sendPasswordResetOtp(anyString());
    }

    @Test
    void duplicateEmailRegistrationFails() {
        when(userRepository.existsByEmailIgnoreCase("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerStudent(studentRequest("duplicate@example.com", "+26770000001")))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("duplicate@example.com");
    }

    @Test
    void loginWithValidCredentialsReturnsJwtWithoutOtpOrPasswordReset() {
        String email = "student@login.com";
        String phone = "+26770000010";
        User user = activeStudent(email, phone);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass@123", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(eq(user), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new com.edurite.auth.dto.LoginRequest(email, "StrongPass@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(otpService, never()).sendVerificationOtp(anyString());
        verify(otpService, never()).sendPasswordResetOtp(anyString());
        verify(passwordEncoder).matches("StrongPass@123", "encoded-password");
        verify(passwordEncoder, never()).encode("StrongPass@123");
        verify(gamificationService, times(1)).awardLoginPoints(user);
    }

    @Test
    void ownerOverrideLoginAlwaysReturnsPremiumPlanType() {
        String email = "arnoldmadaz@gmail.com";
        String phone = "+26770000999";
        User user = activeStudent(email, phone);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass@123", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(studentPlanAccessService.hasPremiumAccess(user.getId())).thenReturn(true);
        when(jwtService.generateAccessToken(eq(user), any(), eq(null), eq("PREMIUM"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new com.edurite.auth.dto.LoginRequest(email, "StrongPass@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().planType()).isEqualTo("PREMIUM");
        assertThat(user.getPlanType()).isEqualTo(com.edurite.subscription.entity.PlanType.PREMIUM);
        verify(jwtService).generateAccessToken(eq(user), any(), eq(null), eq("PREMIUM"));
        verify(userRepository, atLeastOnce()).save(user);
    }

    @Test
    void loginIsCaseInsensitiveForRegisteredEmail() {
        User user = activeStudent("Student@Login.com", "+26770000011");

        when(userRepository.findByEmailIgnoreCase("student@login.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass@123", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(eq(user), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new com.edurite.auth.dto.LoginRequest("STUDENT@LOGIN.COM", "StrongPass@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userRepository).findByEmailIgnoreCase("student@login.com");
        verify(passwordEncoder).matches("StrongPass@123", "encoded-password");
    }

    @Test
    void seededAdminUserCanLoginWithStoredPasswordHash() {
        User user = activeUser("admin@edurite.com", "+26770000012", "ROLE_ADMIN");

        when(userRepository.findByEmailIgnoreCase("admin@edurite.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Admin@123", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(eq(user), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new com.edurite.auth.dto.LoginRequest("admin@edurite.com", "Admin@123"));

        assertThat(response.primaryRole()).isEqualTo("ROLE_ADMIN");
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void companyUserCanLoginWithValidPassword() {
        User user = activeUser("company@edurite.com", "+26770000052", "ROLE_COMPANY");
        CompanyProfile profile = new CompanyProfile();
        profile.setUserId(user.getId());
        profile.setStatus(CompanyApprovalStatus.APPROVED);

        when(userRepository.findByEmailIgnoreCase("company@edurite.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Company@123", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(jwtService.generateAccessToken(eq(user), any(), eq("APPROVED"), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new com.edurite.auth.dto.LoginRequest("company@edurite.com", "Company@123"));

        assertThat(response.primaryRole()).isEqualTo("ROLE_COMPANY");
        assertThat(response.approvalStatus()).isEqualTo("APPROVED");
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void companyLoginWithWrongPasswordFails() {
        User user = activeUser("company@edurite.com", "+26770000053", "ROLE_COMPANY");

        when(userRepository.findByEmailIgnoreCase("company@edurite.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new com.edurite.auth.dto.LoginRequest("company@edurite.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid password.");
    }

    @Test
    void deletedCompanyUserCannotLogin() {
        User user = activeUser("company@edurite.com", "+26770000054", "ROLE_COMPANY");
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(java.time.OffsetDateTime.now());

        when(userRepository.findByEmailIgnoreCase("company@edurite.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Company@123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new com.edurite.auth.dto.LoginRequest("company@edurite.com", "Company@123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Account is inactive or suspended.");
    }

    @Test
    void schoolAdminTeacherAndSchoolStudentCanLogin() {
        User schoolAdmin = activeUser("schooladmin@edurite.com", "+26770000100", "ROLE_SCHOOL_ADMIN");
        User teacher = activeUser("teacher@edurite.com", "+26770000101", "ROLE_TEACHER");
        User schoolStudent = activeUser("schoolstudent@edurite.com", "+26770000102", "ROLE_SCHOOL_STUDENT");

        when(userRepository.findByEmailIgnoreCase("schooladmin@edurite.com")).thenReturn(Optional.of(schoolAdmin));
        when(userRepository.findByEmailIgnoreCase("teacher@edurite.com")).thenReturn(Optional.of(teacher));
        when(userRepository.findByEmailIgnoreCase("schoolstudent@edurite.com")).thenReturn(Optional.of(schoolStudent));
        when(passwordEncoder.matches("School@123", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(any(User.class), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse schoolAdminResponse = authService.login(new com.edurite.auth.dto.LoginRequest("schooladmin@edurite.com", "School@123"));
        AuthResponse teacherResponse = authService.login(new com.edurite.auth.dto.LoginRequest("teacher@edurite.com", "School@123"));
        AuthResponse learnerResponse = authService.login(new com.edurite.auth.dto.LoginRequest("schoolstudent@edurite.com", "School@123"));

        assertThat(schoolAdminResponse.primaryRole()).isEqualTo("ROLE_SCHOOL_ADMIN");
        assertThat(teacherResponse.primaryRole()).isEqualTo("ROLE_TEACHER");
        assertThat(learnerResponse.primaryRole()).isEqualTo("ROLE_SCHOOL_STUDENT");
    }

    @Test
    void googleStudentSignInCreatesStudentAccountWhenEmailIsNew() {
        Role studentRole = new Role();
        studentRole.setName("ROLE_STUDENT");

        when(googleTokenVerifierService.verifyIdToken("student-google-token"))
                .thenReturn(new GoogleIdentity("Google.Student@Example.com", "Google", "Student", "Google Student"));
        when(userRepository.findByEmailIgnoreCase("google.student@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("google.student@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-google-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(any(User.class), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("student-google-token", "STUDENT"));

        assertThat(response.primaryRole()).isEqualTo("ROLE_STUDENT");
        assertThat(response.user().email()).isEqualTo("google.student@example.com");
        verify(studentProfileRepository).save(any(StudentProfile.class));
        verify(companyProfileRepository, never()).save(any(CompanyProfile.class));
    }

    @Test
    void googleCompanySignInCreatesPendingCompanyAccountWhenEmailIsNew() {
        Role companyRole = new Role();
        companyRole.setName("ROLE_COMPANY");
        AtomicReference<CompanyProfile> savedCompanyProfile = new AtomicReference<>();

        when(googleTokenVerifierService.verifyIdToken("company-google-token"))
                .thenReturn(new GoogleIdentity("Company.Owner@Example.com", "Company", "Owner", "Example Company"));
        when(userRepository.findByEmailIgnoreCase("company.owner@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("company.owner@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_COMPANY")).thenReturn(Optional.of(companyRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-google-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(invocation -> {
            CompanyProfile profile = invocation.getArgument(0);
            savedCompanyProfile.set(profile);
            return profile;
        });
        when(companyProfileRepository.findByUserId(any()))
                .thenAnswer(invocation -> Optional.ofNullable(savedCompanyProfile.get()));
        when(studentProfileRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(any(User.class), any(), eq("PENDING"), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("company-google-token", "COMPANY"));

        assertThat(response.primaryRole()).isEqualTo("ROLE_COMPANY");
        assertThat(response.approvalStatus()).isEqualTo("PENDING");
        assertThat(response.user().email()).isEqualTo("company.owner@example.com");
        assertThat(savedCompanyProfile.get()).isNotNull();
        assertThat(savedCompanyProfile.get().getOfficialEmail()).isEqualTo("company.owner@example.com");
        assertThat(savedCompanyProfile.get().getStatus()).isEqualTo(CompanyApprovalStatus.PENDING);
        assertThat(savedCompanyProfile.get().isEmailVerified()).isTrue();
        verify(studentProfileRepository, never()).save(any(StudentProfile.class));
    }

    @Test
    void loginWithPhoneIdentifierResolvesUserByPhone() {
        String email = "student@login-phone.com";
        String phone = "+26770000031";
        User user = activeStudent(email, phone);

        when(userRepository.findByEmailIgnoreCase(phone)).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass@123", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(eq(user), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new com.edurite.auth.dto.LoginRequest(phone, "StrongPass@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(passwordEncoder).matches("StrongPass@123", "encoded-password");
        verify(userRepository).findByEmailIgnoreCase(phone);
        verify(userRepository).findByPhoneNumber(phone);
    }

    @Test
    void loginWithUniqueEmailLocalPartUsernameSucceeds() {
        User user = activeStudent("student@school.com", "+26770000030");

        when(userRepository.findByEmailIgnoreCase("student")).thenReturn(Optional.empty());
        when(userRepository.findAllByEmailLocalPart("student")).thenReturn(List.of(user));
        when(passwordEncoder.matches("StrongPass@123", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(eq(user), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new com.edurite.auth.dto.LoginRequest("student", "StrongPass@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userRepository).findAllByEmailLocalPart("student");
        verify(passwordEncoder).matches("StrongPass@123", "encoded-password");
    }

    @Test
    void loginWithInvalidCredentialsDoesNotSendOtp() {
        User user = activeStudent("bad@example.com", "+26770000032");

        when(userRepository.findByEmailIgnoreCase("bad@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new com.edurite.auth.dto.LoginRequest("bad@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid password.");

        verify(otpService, never()).sendVerificationOtp(anyString());
        verify(otpService, never()).sendPasswordResetOtp(anyString());
    }

    @Test
    void loginWithAmbiguousLocalPartFails() {
        User first = activeStudent("student@school1.com", "+26770000040");
        User second = activeStudent("student@school2.com", "+26770000041");

        when(userRepository.findByEmailIgnoreCase("student")).thenReturn(Optional.empty());
        when(userRepository.findAllByEmailLocalPart("student")).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> authService.login(new com.edurite.auth.dto.LoginRequest("student", "StrongPass@123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void forgotPasswordRequestSendsOtpForExistingAccountIdentifier() {
        User user = activeStudent("forgot@example.com", "+26770000020");

        when(userRepository.findByEmailIgnoreCase("forgot@example.com")).thenReturn(Optional.of(user));

        VerificationStatusResponse response = authService.requestPasswordResetOtp(new ForgotPasswordOtpRequest("forgot@example.com", null));

        assertThat(response.message()).contains("If the account exists");
        verify(otpService, times(1)).sendPasswordResetOtp("+26770000020");
    }

    @Test
    void resetPasswordSucceedsWhenOtpIsValid() {
        User user = activeStudent("reset@example.com", "+26770000021");

        when(userRepository.findByPhoneNumber("+26770000021")).thenReturn(Optional.of(user));
        when(otpService.verifyPasswordResetOtp("+26770000021", "654321")).thenReturn(true);
        when(passwordEncoder.encode("NewStrong@123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationStatusResponse response = authService.resetPasswordWithOtp(
                new ResetPasswordWithOtpRequest("+26770000021", "654321", "NewStrong@123")
        );

        assertThat(response.message()).contains("Password reset complete");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void passwordResetChangesPasswordAndLoginUsesNewPasswordOnly() {
        String email = "reset-login@example.com";
        User user = activeStudent(email, "+26770000023");

        when(userRepository.findByPhoneNumber("+26770000023")).thenReturn(Optional.of(user));
        when(otpService.verifyPasswordResetOtp("+26770000023", "654321")).thenReturn(true);
        when(passwordEncoder.encode("NewStrong@123")).thenReturn("new-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationStatusResponse response = authService.resetPasswordWithOtp(
                new ResetPasswordWithOtpRequest("+26770000023", "654321", "NewStrong@123")
        );

        assertThat(response.message()).contains("Password reset complete");
        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-password");

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldStrong@123", "new-encoded-password")).thenReturn(false);
        when(passwordEncoder.matches("NewStrong@123", "new-encoded-password")).thenReturn(true);
        when(companyProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(eq(user), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        assertThatThrownBy(() -> authService.login(new com.edurite.auth.dto.LoginRequest(email, "OldStrong@123")))
                .isInstanceOf(InvalidCredentialsException.class);

        AuthResponse loginResponse = authService.login(new com.edurite.auth.dto.LoginRequest(email, "NewStrong@123"));
        assertThat(loginResponse.accessToken()).isEqualTo("access-token");
    }

    @Test
    void devPasswordRepairEndpointCanResetKnownAccountPassword() {
        User user = activeStudent("repair@example.com", "+26770000024");

        when(userRepository.findByEmailIgnoreCase("repair@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Repair@1234")).thenReturn("repair-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationStatusResponse response = authService.resetPasswordForDev(new DevPasswordResetRequest("repair@example.com", "Repair@1234"));

        assertThat(response.message()).isEqualTo("Password updated");
        assertThat(user.getPasswordHash()).isEqualTo("repair-encoded-password");
    }

    @Test
    void resetPasswordFailsWhenOtpIsInvalid() {
        User user = activeStudent("reset-invalid@example.com", "+26770000022");

        when(userRepository.findByPhoneNumber("+26770000022")).thenReturn(Optional.of(user));
        when(otpService.verifyPasswordResetOtp("+26770000022", "111111")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetPasswordWithOtp(
                new ResetPasswordWithOtpRequest("+26770000022", "111111", "NewStrong@123")
        ))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid or expired OTP code");
    }

    private StudentRegisterRequest studentRequest(String email, String phone) {
        return new StudentRegisterRequest(
                "Student Example",
                "Student",
                "Example",
                "Engineering",
                "Gaborone",
                phone,
                LocalDate.of(2005, 1, 2),
                "Female",
                "High School",
                true,
                "v1.0",
                email,
                "StrongPass@123"
        );
    }

    private User activeStudent(String email, String phone) {
        return activeUser(email, phone, "ROLE_STUDENT");
    }

    private User activeUser(String email, String phone, String roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPasswordHash("encoded-password");
        user.setFirstName("Student");
        user.setLastName("Example");
        user.getRoles().add(role);
        return user;
    }
}

