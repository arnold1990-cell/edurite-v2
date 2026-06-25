package com.edurite.auth.config;

import com.edurite.district.entity.District;
import com.edurite.district.repository.DistrictRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolSeedDataSeederTest {

    @Mock private SchoolRepository schoolRepository;
    @Mock private SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;
    @Mock private SchoolUserProfileRepository schoolUserProfileRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void schoolSeederCreatesActiveEduRiteSchoolAdminSeedWhenDistrictExists() throws Exception {
        SchoolSeedDataSeeder seeder = new SchoolSeedDataSeeder();
        Role schoolAdminRole = new Role();
        schoolAdminRole.setName("ROLE_SCHOOL_ADMIN");
        District district = district();

        when(districtRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(district));
        when(schoolRepository.findByRegistrationNumberIgnoreCase("99999999")).thenReturn(Optional.empty());
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
        when(schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase("99999999")).thenReturn(Optional.empty());
        when(schoolRegistrationRequestRepository.save(any(SchoolRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = seeder.schoolSeedRunner(
                schoolRepository,
                schoolRegistrationRequestRepository,
                schoolUserProfileRepository,
                districtRepository,
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
        assertThat(savedSchool.getRegistrationNumber()).isEqualTo("99999999");
        assertThat(savedSchool.getStatus()).isEqualTo("ACTIVE");
        assertThat(savedSchool.getDistrictId()).isEqualTo(district.getId());

        ArgumentCaptor<SchoolRegistrationRequest> registrationCaptor = ArgumentCaptor.forClass(SchoolRegistrationRequest.class);
        verify(schoolRegistrationRequestRepository).save(registrationCaptor.capture());
        SchoolRegistrationRequest registration = registrationCaptor.getValue();
        assertThat(registration.getSchoolName()).isEqualTo("EduRite");
        assertThat(registration.getEmisNumber()).isEqualTo("99999999");
        assertThat(registration.getStatus()).isEqualTo(SchoolStatus.ACTIVE);
        assertThat(registration.getPrincipalEmail()).isEqualTo("schooladmin@edurite.com");
        assertThat(registration.getDistrictId()).isEqualTo(district.getId());
        assertThat(registration.getDistrictName()).isEqualTo("Kgale");
        assertThat(registration.getProvince()).isEqualTo("Gauteng");

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

    @Test
    void schoolSeederSkipsRegistrationWhenDistrictMissing() throws Exception {
        SchoolSeedDataSeeder seeder = new SchoolSeedDataSeeder();

        when(districtRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
        when(schoolRepository.findByRegistrationNumberIgnoreCase("99999999")).thenReturn(Optional.empty());
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> {
            School school = invocation.getArgument(0);
            if (school.getId() == null) {
                school.setId(UUID.randomUUID());
            }
            return school;
        });
        when(roleRepository.findByName("ROLE_SCHOOL_ADMIN")).thenReturn(Optional.of(role("ROLE_SCHOOL_ADMIN")));
        when(roleRepository.findByName("ROLE_TEACHER")).thenReturn(Optional.of(role("ROLE_TEACHER")));
        when(roleRepository.findByName("ROLE_SCHOOL_STUDENT")).thenReturn(Optional.of(role("ROLE_SCHOOL_STUDENT")));
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(schoolUserProfileRepository.findByUserIdAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(schoolUserProfileRepository.save(any(SchoolUserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = seeder.schoolSeedRunner(
                schoolRepository,
                schoolRegistrationRequestRepository,
                schoolUserProfileRepository,
                districtRepository,
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

        verify(schoolRegistrationRequestRepository, never()).save(any(SchoolRegistrationRequest.class));
        verify(schoolRegistrationRequestRepository, never()).findByUserId(any());
        verify(schoolRegistrationRequestRepository, never()).findByEmisNumberIgnoreCase(any());
    }

    @Test
    void schoolSeederUsesExistingEmisSchoolInsteadOfMutatingUnrelatedSchool() throws Exception {
        SchoolSeedDataSeeder seeder = new SchoolSeedDataSeeder();
        UUID seededSchoolId = UUID.randomUUID();
        Role schoolAdminRole = role("ROLE_SCHOOL_ADMIN");
        District district = district();
        School existingSeededSchool = new School();
        existingSeededSchool.setId(seededSchoolId);
        existingSeededSchool.setSchoolName("Old Name");
        existingSeededSchool.setRegistrationNumber("99999999");
        existingSeededSchool.setStatus("ACTIVE");

        when(districtRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(district));
        when(schoolRepository.findByRegistrationNumberIgnoreCase("99999999")).thenReturn(Optional.of(existingSeededSchool));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName("ROLE_SCHOOL_ADMIN")).thenReturn(Optional.of(schoolAdminRole));
        when(roleRepository.findByName("ROLE_TEACHER")).thenReturn(Optional.of(role("ROLE_TEACHER")));
        when(roleRepository.findByName("ROLE_SCHOOL_STUDENT")).thenReturn(Optional.of(role("ROLE_SCHOOL_STUDENT")));
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
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
        when(schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase("99999999")).thenReturn(Optional.empty());
        when(schoolRegistrationRequestRepository.save(any(SchoolRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = seeder.schoolSeedRunner(
                schoolRepository,
                schoolRegistrationRequestRepository,
                schoolUserProfileRepository,
                districtRepository,
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
        assertThat(savedSchool.getId()).isEqualTo(seededSchoolId);
        assertThat(savedSchool.getSchoolName()).isEqualTo("EduRite");
        assertThat(savedSchool.getRegistrationNumber()).isEqualTo("99999999");
    }

    @Test
    void schoolSeederUpdatesExistingRegistrationFoundByUser() throws Exception {
        SchoolSeedDataSeeder seeder = new SchoolSeedDataSeeder();
        UUID schoolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        District district = district();

        School school = new School();
        school.setId(schoolId);
        school.setRegistrationNumber("99999999");

        Role schoolAdminRole = role("ROLE_SCHOOL_ADMIN");
        User existingAdmin = new User();
        existingAdmin.setId(userId);
        existingAdmin.setEmail("schooladmin@edurite.com");
        existingAdmin.setPasswordHash("encoded-admin");
        existingAdmin.setStatus(UserStatus.ACTIVE);
        existingAdmin.setEmailVerified(true);
        existingAdmin.getRoles().add(schoolAdminRole);

        SchoolRegistrationRequest existingRegistration = new SchoolRegistrationRequest();
        existingRegistration.setId(registrationId);
        existingRegistration.setUserId(userId);
        existingRegistration.setEmisNumber("99999999");
        existingRegistration.setDistrictId(district.getId());
        existingRegistration.setDistrictName("Old District");
        existingRegistration.setProvince("Old Province");

        when(districtRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(district));
        when(schoolRepository.findByRegistrationNumberIgnoreCase("99999999")).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName("ROLE_SCHOOL_ADMIN")).thenReturn(Optional.of(schoolAdminRole));
        when(roleRepository.findByName("ROLE_TEACHER")).thenReturn(Optional.of(role("ROLE_TEACHER")));
        when(roleRepository.findByName("ROLE_SCHOOL_STUDENT")).thenReturn(Optional.of(role("ROLE_SCHOOL_STUDENT")));
        when(userRepository.findByEmailIgnoreCase("schooladmin@edurite.com")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmailIgnoreCase("teacher@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("schoolstudent@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("arnold.student@edurite.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("Admin@123", "encoded-admin")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(schoolUserProfileRepository.findByUserIdAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(schoolUserProfileRepository.save(any(SchoolUserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolRegistrationRequestRepository.findByUserId(userId)).thenReturn(Optional.of(existingRegistration));
        when(schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase("99999999")).thenReturn(Optional.empty());
        when(schoolRegistrationRequestRepository.save(any(SchoolRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = seeder.schoolSeedRunner(
                schoolRepository,
                schoolRegistrationRequestRepository,
                schoolUserProfileRepository,
                districtRepository,
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

        verify(schoolRegistrationRequestRepository, never()).delete(any(SchoolRegistrationRequest.class));
        ArgumentCaptor<SchoolRegistrationRequest> registrationCaptor = ArgumentCaptor.forClass(SchoolRegistrationRequest.class);
        verify(schoolRegistrationRequestRepository).save(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().getId()).isEqualTo(registrationId);
        assertThat(registrationCaptor.getValue().getDistrictId()).isEqualTo(district.getId());
        assertThat(registrationCaptor.getValue().getDistrictName()).isEqualTo("Kgale");
        assertThat(registrationCaptor.getValue().getProvince()).isEqualTo("Gauteng");
    }

    @Test
    void schoolSeederUpdatesExistingRegistrationFoundByEmis() throws Exception {
        SchoolSeedDataSeeder seeder = new SchoolSeedDataSeeder();
        UUID schoolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        District district = district();

        School school = new School();
        school.setId(schoolId);
        school.setRegistrationNumber("99999999");

        Role schoolAdminRole = role("ROLE_SCHOOL_ADMIN");
        User existingAdmin = new User();
        existingAdmin.setId(userId);
        existingAdmin.setEmail("schooladmin@edurite.com");
        existingAdmin.setPasswordHash("encoded-admin");
        existingAdmin.setStatus(UserStatus.ACTIVE);
        existingAdmin.setEmailVerified(true);
        existingAdmin.getRoles().add(schoolAdminRole);

        SchoolRegistrationRequest existingRegistration = new SchoolRegistrationRequest();
        existingRegistration.setId(registrationId);
        existingRegistration.setUserId(UUID.randomUUID());
        existingRegistration.setEmisNumber("99999999");

        when(districtRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(district));
        when(schoolRepository.findByRegistrationNumberIgnoreCase("99999999")).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName("ROLE_SCHOOL_ADMIN")).thenReturn(Optional.of(schoolAdminRole));
        when(roleRepository.findByName("ROLE_TEACHER")).thenReturn(Optional.of(role("ROLE_TEACHER")));
        when(roleRepository.findByName("ROLE_SCHOOL_STUDENT")).thenReturn(Optional.of(role("ROLE_SCHOOL_STUDENT")));
        when(userRepository.findByEmailIgnoreCase("schooladmin@edurite.com")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmailIgnoreCase("teacher@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("schoolstudent@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("arnold.student@edurite.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("Admin@123", "encoded-admin")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(schoolUserProfileRepository.findByUserIdAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(schoolUserProfileRepository.save(any(SchoolUserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolRegistrationRequestRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase("99999999")).thenReturn(Optional.of(existingRegistration));
        when(schoolRegistrationRequestRepository.save(any(SchoolRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = seeder.schoolSeedRunner(
                schoolRepository,
                schoolRegistrationRequestRepository,
                schoolUserProfileRepository,
                districtRepository,
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

        ArgumentCaptor<SchoolRegistrationRequest> registrationCaptor = ArgumentCaptor.forClass(SchoolRegistrationRequest.class);
        verify(schoolRegistrationRequestRepository).save(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().getId()).isEqualTo(registrationId);
        assertThat(registrationCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(registrationCaptor.getValue().getDistrictId()).isEqualTo(district.getId());
        assertThat(registrationCaptor.getValue().getDistrictName()).isEqualTo("Kgale");
        assertThat(registrationCaptor.getValue().getProvince()).isEqualTo("Gauteng");
    }

    @Test
    void schoolSeederCreatesNewRegistrationOnlyWhenNoExistingRecordExists() throws Exception {
        SchoolSeedDataSeeder seeder = new SchoolSeedDataSeeder();
        UUID schoolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        District district = district();

        School school = new School();
        school.setId(schoolId);
        school.setRegistrationNumber("99999999");

        Role schoolAdminRole = role("ROLE_SCHOOL_ADMIN");
        User existingAdmin = new User();
        existingAdmin.setId(userId);
        existingAdmin.setEmail("schooladmin@edurite.com");
        existingAdmin.setPasswordHash("encoded-admin");
        existingAdmin.setStatus(UserStatus.ACTIVE);
        existingAdmin.setEmailVerified(true);
        existingAdmin.getRoles().add(schoolAdminRole);

        when(districtRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(district));
        when(schoolRepository.findByRegistrationNumberIgnoreCase("99999999")).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName("ROLE_SCHOOL_ADMIN")).thenReturn(Optional.of(schoolAdminRole));
        when(roleRepository.findByName("ROLE_TEACHER")).thenReturn(Optional.of(role("ROLE_TEACHER")));
        when(roleRepository.findByName("ROLE_SCHOOL_STUDENT")).thenReturn(Optional.of(role("ROLE_SCHOOL_STUDENT")));
        when(userRepository.findByEmailIgnoreCase("schooladmin@edurite.com")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmailIgnoreCase("teacher@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("schoolstudent@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("arnold.student@edurite.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("Admin@123", "encoded-admin")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(schoolUserProfileRepository.findByUserIdAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(schoolUserProfileRepository.save(any(SchoolUserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolRegistrationRequestRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase("99999999")).thenReturn(Optional.empty());
        when(schoolRegistrationRequestRepository.save(any(SchoolRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = seeder.schoolSeedRunner(
                schoolRepository,
                schoolRegistrationRequestRepository,
                schoolUserProfileRepository,
                districtRepository,
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

        ArgumentCaptor<SchoolRegistrationRequest> registrationCaptor = ArgumentCaptor.forClass(SchoolRegistrationRequest.class);
        verify(schoolRegistrationRequestRepository).save(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().getId()).isNull();
        assertThat(registrationCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(registrationCaptor.getValue().getDistrictId()).isEqualTo(district.getId());
    }

    @Test
    void schoolSeederConsolidatesConflictingRegistrationRowsByEmis() throws Exception {
        SchoolSeedDataSeeder seeder = new SchoolSeedDataSeeder();
        UUID schoolId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        District district = district();

        School school = new School();
        school.setId(schoolId);
        school.setRegistrationNumber("99999999");

        Role schoolAdminRole = role("ROLE_SCHOOL_ADMIN");
        User existingAdmin = new User();
        existingAdmin.setId(currentUserId);
        existingAdmin.setEmail("schooladmin@edurite.com");
        existingAdmin.setPasswordHash("encoded-admin");
        existingAdmin.setStatus(UserStatus.ACTIVE);
        existingAdmin.setEmailVerified(true);
        existingAdmin.getRoles().add(schoolAdminRole);

        SchoolRegistrationRequest byUser = new SchoolRegistrationRequest();
        byUser.setId(UUID.randomUUID());
        byUser.setUserId(currentUserId);
        byUser.setEmisNumber("OLD-EMIS");

        SchoolRegistrationRequest byEmis = new SchoolRegistrationRequest();
        byEmis.setId(UUID.randomUUID());
        byEmis.setUserId(UUID.randomUUID());
        byEmis.setEmisNumber("99999999");

        when(districtRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(district));
        when(schoolRepository.findByRegistrationNumberIgnoreCase("99999999")).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName("ROLE_SCHOOL_ADMIN")).thenReturn(Optional.of(schoolAdminRole));
        when(roleRepository.findByName("ROLE_TEACHER")).thenReturn(Optional.of(role("ROLE_TEACHER")));
        when(roleRepository.findByName("ROLE_SCHOOL_STUDENT")).thenReturn(Optional.of(role("ROLE_SCHOOL_STUDENT")));
        when(userRepository.findByEmailIgnoreCase("schooladmin@edurite.com")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmailIgnoreCase("teacher@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("schoolstudent@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("arnold.student@edurite.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("Admin@123", "encoded-admin")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(schoolUserProfileRepository.findByUserIdAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(schoolUserProfileRepository.save(any(SchoolUserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolRegistrationRequestRepository.findByUserId(currentUserId)).thenReturn(Optional.of(byUser));
        when(schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase("99999999")).thenReturn(Optional.of(byEmis));
        when(schoolRegistrationRequestRepository.save(any(SchoolRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = seeder.schoolSeedRunner(
                schoolRepository,
                schoolRegistrationRequestRepository,
                schoolUserProfileRepository,
                districtRepository,
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

        verify(schoolRegistrationRequestRepository).delete(byUser);
        ArgumentCaptor<SchoolRegistrationRequest> registrationCaptor = ArgumentCaptor.forClass(SchoolRegistrationRequest.class);
        verify(schoolRegistrationRequestRepository).save(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().getUserId()).isEqualTo(currentUserId);
        assertThat(registrationCaptor.getValue().getEmisNumber()).isEqualTo("99999999");
        assertThat(registrationCaptor.getValue().getStatus()).isEqualTo(SchoolStatus.ACTIVE);
        assertThat(registrationCaptor.getValue().getDistrictId()).isEqualTo(district.getId());
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private District district() {
        District district = new District();
        district.setId(UUID.randomUUID());
        district.setDistrictName("Kgale");
        district.setProvince("Gauteng");
        return district;
    }
}
