package com.edurite.auth.config;

import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.career.repository.CareerRepository;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.notification.repository.UserNotificationRepository;
import com.edurite.notification.service.NotificationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentFeatureDataSeederTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;
    @Mock
    private CareerRepository careerRepository;
    @Mock
    private BursaryRepository bursaryRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserNotificationRepository userNotificationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void studentSeederCreatesArnoldAndCompanyWithRegisteredPhoneNumbers() throws Exception {
        Role studentRole = role("ROLE_STUDENT");
        Role companyRole = role("ROLE_COMPANY");

        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("ROLE_COMPANY")).thenReturn(Optional.of(companyRole));
        when(userRepository.findByEmail("arnoldmadaz@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("company@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("+26775314557")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("+26772523672")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(companyProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentProfileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(careerRepository.count()).thenReturn(1L);
        when(bursaryRepository.count()).thenReturn(1L);
        when(userNotificationRepository.countByUserId(any(UUID.class))).thenReturn(1L);

        ApplicationRunner runner = new StudentFeatureDataSeeder().studentFeatureSeedRunner(
                userRepository,
                roleRepository,
                studentProfileRepository,
                companyProfileRepository,
                careerRepository,
                bursaryRepository,
                notificationService,
                userNotificationRepository,
                passwordEncoder,
                "arnoldmadaz@gmail.com",
                "Student@123",
                "Arnold",
                "Madaz",
                "+26775314557",
                "company@edurite.com",
                "Company@123",
                "EduRite Company",
                "Company Admin",
                "EDURITE-COMPANY-001",
                "+26772523672"
        );

        runner.run(null);

        verify(userRepository, atLeastOnce()).save(argThat(user ->
                "arnoldmadaz@gmail.com".equals(user.getEmail())
                        && "+26775314557".equals(user.getPhoneNumber())
        ));
        verify(userRepository, atLeastOnce()).save(argThat(user ->
                "company@edurite.com".equals(user.getEmail())
                        && "+26772523672".equals(user.getPhoneNumber())
        ));
        verify(studentProfileRepository, atLeastOnce()).save(argThat(profile ->
                "+26775314557".equals(profile.getPhone())
        ));
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}

