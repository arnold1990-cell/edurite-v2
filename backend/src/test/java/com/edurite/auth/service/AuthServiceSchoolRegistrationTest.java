package com.edurite.auth.service;

import com.edurite.admin.entity.PlatformSetting;
import com.edurite.admin.service.PlatformSettingsService;
import com.edurite.auth.config.AuthOtpProperties;
import com.edurite.auth.config.AuthOtpProperties;
import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.dto.RegistrationResponse;
import com.edurite.auth.dto.SchoolRegisterRequest;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.compliance.service.ConsentService;
import com.edurite.district.entity.Circuit;
import com.edurite.district.entity.District;
import com.edurite.district.entity.DistrictAdminProfile;
import com.edurite.district.entity.Province;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.district.service.LocationService;
import com.edurite.gamification.service.GamificationService;
import com.edurite.notification.service.NotificationService;
import com.edurite.school.portal.entity.SchoolRegistrationRequest;
import com.edurite.school.portal.entity.SchoolStatus;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.security.service.JwtService;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentProfileCompletionService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.subscription.service.SubscriptionService;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceSchoolRegistrationTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private CompanyProfileRepository companyProfileRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private DistrictAdminProfileRepository districtAdminProfileRepository;
    @Mock private LocationService locationService;
    @Mock private SchoolRepository schoolRepository;
    @Mock private SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private OtpService otpService;
    @Mock private ConsentService consentService;
    @Mock private GamificationService gamificationService;
    @Mock private StudentProfileCompletionService studentProfileCompletionService;
    @Mock private GoogleTokenVerifierService googleTokenVerifierService;
    @Mock private PlatformSettingsService platformSettingsService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private StudentPlanAccessService studentPlanAccessService;
    @Mock private NotificationService notificationService;

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
                new AuthOtpProperties(false),
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
    void registerSchoolCreatesPendingDistrictApprovalRequest() {
        PlatformSetting setting = new PlatformSetting();
        Province province = new Province();
        province.setId(UUID.randomUUID());
        province.setName("Gauteng");
        District district = new District();
        district.setId(UUID.randomUUID());
        district.setDistrictName("City of Johannesburg");
        district.setProvinceId(province.getId());
        Circuit circuit = new Circuit();
        circuit.setId(UUID.randomUUID());
        circuit.setDistrictId(district.getId());
        circuit.setName("Circuit A");
        Role role = new Role();
        role.setName("ROLE_SCHOOL_ADMIN");
        DistrictAdminProfile adminProfile = new DistrictAdminProfile();
        adminProfile.setUserId(UUID.randomUUID());

        when(platformSettingsService.getCurrentSettingsEntity()).thenReturn(setting);
        when(locationService.requireActiveDistrict(district.getId())).thenReturn(district);
        when(locationService.requireActiveCircuit(circuit.getId(), district.getId())).thenReturn(circuit);
        when(schoolRegistrationRequestRepository.existsByEmisNumberIgnoreCase("EMIS-001")).thenReturn(false);
        when(schoolRepository.existsByRegistrationNumberIgnoreCase("EMIS-001")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("school@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+26771234567")).thenReturn(false);
        when(roleRepository.findByName("ROLE_SCHOOL_ADMIN")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(schoolRegistrationRequestRepository.save(any(SchoolRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(districtAdminProfileRepository.findByDistrictIdAndActiveTrueAndDeletedFalse(district.getId())).thenReturn(List.of(adminProfile));
        RegistrationResponse response = authService.registerSchool(new SchoolRegisterRequest(
                "Kgale Secondary",
                "EMIS-001",
                district.getId(),
                circuit.getId(),
                "Public",
                "Jane Principal",
                "principal@example.com",
                "school@example.com",
                "+26771234567",
                "Plot 10 Gaborone",
                "Password@123",
                "Password@123"
        ));

        ArgumentCaptor<SchoolRegistrationRequest> captor = ArgumentCaptor.forClass(SchoolRegistrationRequest.class);
        verify(schoolRegistrationRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchoolStatus.PENDING_DISTRICT_APPROVAL);
        assertThat(captor.getValue().getEmisNumber()).isEqualTo("EMIS-001");
        assertThat(captor.getValue().getDistrictId()).isEqualTo(district.getId());
        assertThat(captor.getValue().getCircuitId()).isEqualTo(circuit.getId());
        assertThat(response.verificationRequired()).isFalse();
    }

    @Test
    void schoolLoginVerifiesSchoolNameAndEmisNumber() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("school@example.com");
        user.setPasswordHash("encoded");
        user.setFirstName("Kgale Secondary");
        user.setLastName("School Admin");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        Role role = new Role();
        role.setName("ROLE_SCHOOL_ADMIN");
        user.setRoles(Set.of(role));

        SchoolRegistrationRequest registrationRequest = new SchoolRegistrationRequest();
        registrationRequest.setUserId(userId);
        registrationRequest.setSchoolName("Kgale Secondary");
        registrationRequest.setEmisNumber("EMIS-001");
        registrationRequest.setStatus(SchoolStatus.PENDING_DISTRICT_APPROVAL);

        when(schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase("EMIS-001")).thenReturn(Optional.of(registrationRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "encoded")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(schoolRegistrationRequestRepository.findByUserId(userId)).thenReturn(Optional.of(registrationRequest));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(studentPlanAccessService.hasPremiumAccess(userId)).thenReturn(false);
        when(jwtService.generateAccessToken(any(User.class), anyCollection(), anyString(), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest(null, "Kgale Secondary", "EMIS-001", "Password@123"));

        assertThat(response.user().schoolName()).isEqualTo("Kgale Secondary");
        assertThat(response.user().approvalStatus()).isEqualTo("PENDING_DISTRICT_APPROVAL");
    }
}
