package com.edurite.auth.config;

import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolRegistrationRequest;
import com.edurite.school.portal.entity.SchoolStatus;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolSeedDataSeederTest {

    @Mock private SchoolRepository schoolRepository;
    @Mock private SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;
    @Mock private SchoolUserProfileRepository schoolUserProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void schoolSeederCreatesActiveEduRiteSchoolAdminSeed() throws Exception {
        SchoolSeedDataSeeder seeder = new SchoolSeedDataSeeder();
        Role schoolAdminRole = new Role();
        schoolAdminRole.setName("ROLE_SCHOOL_ADMIN");

        when(schoolRepository.findAll()).thenReturn(List.of());
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> {
            School school = invocation.getArgument(0);
            if (school.getId() == null) {
                school.setId(UUID.randomUUID());
            }
            return school;
        });
        when(roleRepository.findByName("ROLE_SCHOOL_ADMIN")).thenReturn(Optional.of(schoolAdminRole));
        when(roleRepository.findByName("ROLE_TEACHER")).thenReturn(Optional.of(role("ROLE_TEACHER")));
        when(roleRepository.findByName("ROLE_SCHOOL_STUDENT")).thenReturn(Optional.of(role("ROLE_SCHOOL_STUDENT")));
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin@123")).thenReturn("encoded-admin");
        when(passwordEncoder.encode("Teacher@123")).thenReturn("encoded-teacher");
        when(passwordEncoder.encode("SchoolStudent@123")).thenReturn("encoded-student");
        when(passwordEncoder.encode("Student@123")).thenReturn("encoded-demo");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(schoolUserProfileRepository.findByUserIdAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(schoolUserProfileRepository.save(any(SchoolUserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolRegistrationRequestRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase("050020")).thenReturn(Optional.empty());
        when(schoolRegistrationRequestRepository.save(any(SchoolRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = seeder.schoolSeedRunner(
                schoolRepository,
                schoolRegistrationRequestRepository,
                schoolUserProfileRepository,
                userRepository,
                roleRepository,
                passwordEncoder,
                "schooladmin@edurite.com",
                "Admin@123",
                "teacher@edurite.com",
                "Teacher@123",
                "schoolstudent@edurite.com",
                "SchoolStudent@123",
                "arnold.student@edurite.com",
                "Student@123"
        );

        runner.run(null);

        ArgumentCaptor<School> schoolCaptor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository, org.mockito.Mockito.atLeastOnce()).save(schoolCaptor.capture());
        School savedSchool = schoolCaptor.getAllValues().getLast();
        assertThat(savedSchool.getSchoolName()).isEqualTo("EduRite");
        assertThat(savedSchool.getRegistrationNumber()).isEqualTo("050020");
        assertThat(savedSchool.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<SchoolRegistrationRequest> registrationCaptor = ArgumentCaptor.forClass(SchoolRegistrationRequest.class);
        verify(schoolRegistrationRequestRepository).save(registrationCaptor.capture());
        SchoolRegistrationRequest registration = registrationCaptor.getValue();
        assertThat(registration.getSchoolName()).isEqualTo("EduRite");
        assertThat(registration.getEmisNumber()).isEqualTo("050020");
        assertThat(registration.getStatus()).isEqualTo(SchoolStatus.ACTIVE);
        assertThat(registration.getPrincipalEmail()).isEqualTo("schooladmin@edurite.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(userCaptor.capture());
        User savedUser = userCaptor.getAllValues().stream()
                .filter(user -> "schooladmin@edurite.com".equals(user.getEmail()))
                .findFirst()
                .orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo("schooladmin@edurite.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-admin");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.isEmailVerified()).isTrue();
        assertThat(savedUser.getRoles()).anyMatch(role -> "ROLE_SCHOOL_ADMIN".equals(role.getName()));
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
