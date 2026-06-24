package com.edurite.auth.config;

import com.edurite.auth.config.AuthOtpProperties;
import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.service.AuthService;
import com.edurite.auth.service.GoogleTokenVerifierService;
import com.edurite.auth.service.OtpService;
import com.edurite.admin.service.PlatformSettingsService;
import com.edurite.compliance.service.ConsentService;
import com.edurite.gamification.service.GamificationService;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.district.service.LocationService;
import com.edurite.notification.service.NotificationService;
import com.edurite.security.service.JwtService;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentProfileCompletionService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.subscription.service.SubscriptionService;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeedDataSeederTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
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
    private AdminSeedDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new AdminSeedDataSeeder();
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
    }

    @Test
    void adminCreatedWhenEnabled() {
        Role adminRole = role("ROLE_ADMIN");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin@123")).thenReturn("encoded-admin-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        seeder.seed(
                roleRepository,
                userRepository,
                passwordEncoder,
                "admin@example.com",
                "Admin@123",
                "System",
                "Admin"
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-admin-password");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.getRoles()).anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        verify(passwordEncoder).encode("Admin@123");
    }

    @Test
    void adminNotCreatedWhenDisabled() throws Exception {
        ApplicationRunner runner = seeder.createAdminSeedRunner(
                false,
                roleRepository,
                userRepository,
                passwordEncoder,
                "admin@example.com",
                "Admin@123",
                "System",
                "Admin"
        );

        runner.run(null);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void existingAdminNotDuplicated() {
        Role adminRole = role("ROLE_ADMIN");
        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("admin@example.com");
        existing.setFirstName("System");
        existing.setLastName("Admin");
        existing.setPasswordHash("encoded-admin-password");
        existing.setStatus(UserStatus.ACTIVE);
        existing.setEmailVerified(true);
        existing.getRoles().add(adminRole);

        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("Admin@123", "encoded-admin-password")).thenReturn(true);

        seeder.seed(
                roleRepository,
                userRepository,
                passwordEncoder,
                "admin@example.com",
                "Admin@123",
                "System",
                "Admin"
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode("Admin@123");
    }

    @Test
    void seededAdminCanLogin() {
        Role adminRole = role("ROLE_ADMIN");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin@123")).thenReturn("encoded-admin-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        seeder.seed(
                roleRepository,
                userRepository,
                passwordEncoder,
                "admin@example.com",
                "Admin@123",
                "System",
                "Admin"
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User seededAdmin = userCaptor.getValue();

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(seededAdmin));
        when(passwordEncoder.matches("Admin@123", "encoded-admin-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(seededAdmin.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(seededAdmin.getId())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(eq(seededAdmin), any(), eq(null), eq("BASIC"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(seededAdmin)).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest("admin@example.com", "Admin@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.primaryRole()).isEqualTo("ROLE_ADMIN");
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}

