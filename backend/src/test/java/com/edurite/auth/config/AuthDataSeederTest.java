package com.edurite.auth.config;

import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthDataSeederTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void seedEnsuresAdminAndCompanyPhoneNumbersWithoutOverwritingExistingPasswords() {
        Role studentRole = role("ROLE_STUDENT");
        Role companyRole = role("ROLE_COMPANY");
        Role adminRole = role("ROLE_ADMIN");

        User adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@edurite.com");
        adminUser.setPasswordHash("$2a$10$existing-admin-hash");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser.getRoles().add(adminRole);

        User companyUser = new User();
        companyUser.setId(UUID.randomUUID());
        companyUser.setEmail("company@edurite.com");
        companyUser.setPasswordHash("$2a$10$existing-company-hash");
        companyUser.setStatus(UserStatus.ACTIVE);
        companyUser.setEmailVerified(true);
        companyUser.getRoles().add(companyRole);

        CompanyProfile companyProfile = new CompanyProfile();
        companyProfile.setUserId(companyUser.getId());
        companyProfile.setCompanyName("EduRite Company");
        companyProfile.setRegistrationNumber("EDURITE-COMPANY-001");
        companyProfile.setOfficialEmail("company@edurite.com");
        companyProfile.setContactPersonName("Company Admin");
        companyProfile.setStatus(CompanyApprovalStatus.PENDING);

        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("ROLE_COMPANY")).thenReturn(Optional.of(companyRole));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmail("admin@edurite.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail("company@edurite.com")).thenReturn(Optional.of(companyUser));
        when(userRepository.findByPhoneNumber("+26776821110")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("+26772523672")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("Company@123", "$2a$10$existing-company-hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(companyUser.getId())).thenReturn(Optional.of(companyProfile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthDataSeeder seeder = new AuthDataSeeder();
        seeder.seed(
                roleRepository,
                userRepository,
                companyProfileRepository,
                passwordEncoder,
                "admin@edurite.com",
                "Admin@123",
                "System",
                "Admin",
                "+26776821110",
                "company@edurite.com",
                "Company@123",
                "+26772523672",
                "EduRite Company",
                "EDURITE-COMPANY-001",
                "Company Admin",
                "PENDING"
        );

        assertThat(adminUser.getPhoneNumber()).isEqualTo("+26776821110");
        assertThat(adminUser.getPasswordHash()).isEqualTo("$2a$10$existing-admin-hash");
        assertThat(companyUser.getPhoneNumber()).isEqualTo("+26772523672");
        assertThat(companyUser.getPasswordHash()).isEqualTo("$2a$10$existing-company-hash");
        assertThat(companyProfile.getMobileNumber()).isEqualTo("+26772523672");
    }

    @Test
    void seedDoesNotAssignPhoneIfOwnedByAnotherUser() {
        Role studentRole = role("ROLE_STUDENT");
        Role companyRole = role("ROLE_COMPANY");
        Role adminRole = role("ROLE_ADMIN");

        User adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@edurite.com");
        adminUser.setPasswordHash("$2a$10$existing-admin-hash");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser.getRoles().add(adminRole);
        adminUser.setPhoneNumber("+26770000001");

        User companyUser = new User();
        companyUser.setId(UUID.randomUUID());
        companyUser.setEmail("company@edurite.com");
        companyUser.setPasswordHash("$2a$10$existing-company-hash");
        companyUser.setStatus(UserStatus.ACTIVE);
        companyUser.setEmailVerified(true);
        companyUser.getRoles().add(companyRole);
        companyUser.setPhoneNumber("+26770000002");

        User conflictingPhoneOwner = new User();
        conflictingPhoneOwner.setId(UUID.randomUUID());
        conflictingPhoneOwner.setEmail("other@edurite.com");

        CompanyProfile companyProfile = new CompanyProfile();
        companyProfile.setUserId(companyUser.getId());
        companyProfile.setCompanyName("EduRite Company");
        companyProfile.setRegistrationNumber("EDURITE-COMPANY-001");
        companyProfile.setOfficialEmail("company@edurite.com");
        companyProfile.setContactPersonName("Company Admin");
        companyProfile.setStatus(CompanyApprovalStatus.PENDING);
        companyProfile.setMobileNumber("+26770000002");

        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("ROLE_COMPANY")).thenReturn(Optional.of(companyRole));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmail("admin@edurite.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail("company@edurite.com")).thenReturn(Optional.of(companyUser));
        when(userRepository.findByPhoneNumber("+26776821110")).thenReturn(Optional.of(conflictingPhoneOwner));
        when(userRepository.findByPhoneNumber("+26772523672")).thenReturn(Optional.of(conflictingPhoneOwner));
        when(passwordEncoder.matches("Company@123", "$2a$10$existing-company-hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(companyUser.getId())).thenReturn(Optional.of(companyProfile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthDataSeeder seeder = new AuthDataSeeder();
        seeder.seed(
                roleRepository,
                userRepository,
                companyProfileRepository,
                passwordEncoder,
                "admin@edurite.com",
                "Admin@123",
                "System",
                "Admin",
                "+26776821110",
                "company@edurite.com",
                "Company@123",
                "+26772523672",
                "EduRite Company",
                "EDURITE-COMPANY-001",
                "Company Admin",
                "PENDING"
        );

        assertThat(adminUser.getPhoneNumber()).isEqualTo("+26770000001");
        assertThat(companyUser.getPhoneNumber()).isEqualTo("+26770000002");
        assertThat(companyProfile.getMobileNumber()).isEqualTo("+26770000002");
        verify(userRepository, never()).save(argThat(user ->
                "admin@edurite.com".equals(user.getEmail()) && "+26776821110".equals(user.getPhoneNumber())
        ));
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}

