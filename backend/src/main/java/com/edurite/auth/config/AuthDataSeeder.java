package com.edurite.auth.config;

import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Seeds baseline auth roles and optional development users when seeding is enabled.
 */
@Configuration
public class AuthDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(AuthDataSeeder.class);

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    @Order(0)
    @ConditionalOnProperty(prefix = "edurite.seed", name = "enabled", havingValue = "true")
    ApplicationRunner authSeedRunner(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CompanyProfileRepository companyProfileRepository,
            PasswordEncoder passwordEncoder,
            @Value("${edurite.auth.seed.admin.email}") String adminEmail,
            @Value("${edurite.auth.seed.admin.password}") String adminPassword,
            @Value("${edurite.auth.seed.admin.first-name:System}") String firstName,
            @Value("${edurite.auth.seed.admin.last-name:Admin}") String lastName,
            @Value("${edurite.auth.seed.admin.phone-number:+26776821110}") String adminPhoneNumber,
            @Value("${edurite.auth.seed.company.email}") String companyEmail,
            @Value("${edurite.auth.seed.company.password}") String companyPassword,
            @Value("${edurite.auth.seed.company.phone-number:+26772523672}") String companyPhoneNumber,
            @Value("${edurite.auth.seed.company.name:EduRite Company}") String companyName,
            @Value("${edurite.auth.seed.company.registration-number:EDURITE-COMPANY-001}") String companyRegistrationNumber,
            @Value("${edurite.auth.seed.company.contact-person:Company Admin}") String companyContactPerson,
            @Value("${edurite.auth.seed.company.approval-status:PENDING}") String companyApprovalStatus,
            TransactionTemplate transactionTemplate
    ) {
        return args -> transactionTemplate.executeWithoutResult(status -> seed(
                        roleRepository,
                        userRepository,
                        companyProfileRepository,
                        passwordEncoder,
                        adminEmail,
                        adminPassword,
                        firstName,
                        lastName,
                        adminPhoneNumber,
                        companyEmail,
                        companyPassword,
                        companyPhoneNumber,
                        companyName,
                        companyRegistrationNumber,
                        companyContactPerson,
                        companyApprovalStatus
                )
        );
    }

    void seed(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CompanyProfileRepository companyProfileRepository,
            PasswordEncoder passwordEncoder,
            String adminEmail,
            String adminPassword,
            String firstName,
            String lastName,
            String adminPhoneNumber,
            String companyEmail,
            String companyPassword,
            String companyPhoneNumber,
            String companyName,
            String companyRegistrationNumber,
            String companyContactPerson,
            String companyApprovalStatus
    ) {
        String normalizedAdminEmail = normalizeEmail(adminEmail);
        String normalizedAdminPhone = normalizePhoneNumber(adminPhoneNumber);
        List<String> roles = List.of(
                "ROLE_STUDENT",
                "ROLE_COMPANY",
                "ROLE_ADMIN",
                "ROLE_DISTRICT_ADMIN",
                "ROLE_DISTRICT_DIRECTOR",
                "ROLE_CIRCUIT_MANAGER",
                "ROLE_SUBJECT_ADVISOR",
                "ROLE_SCHOOL_ADMIN",
                "ROLE_TEACHER",
                "ROLE_SCHOOL_STUDENT"
        );
        for (String roleName : roles) {
            roleRepository.findByName(roleName).orElseGet(() -> {
                Role role = new Role();
                role.setName(roleName);
                log.info("[auth-seed] creating missing role={}", roleName);
                return roleRepository.save(role);
            });
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        userRepository.findByEmail(normalizedAdminEmail).ifPresentOrElse(existingUser -> {
            boolean changed = false;
            changed |= assignSeedPhoneNumberIfAvailable(userRepository, existingUser, normalizedAdminPhone, normalizedAdminEmail);
            if (!existingUser.isEmailVerified()) {
                existingUser.setEmailVerified(true);
                changed = true;
            }
            if (existingUser.getStatus() != UserStatus.ACTIVE) {
                existingUser.setStatus(UserStatus.ACTIVE);
                changed = true;
            }
            if (isMissingOrInvalidPasswordHash(existingUser.getPasswordHash())) {
                existingUser.setPasswordHash(passwordEncoder.encode(adminPassword));
                changed = true;
            }
            if (isBlank(existingUser.getFirstName())) {
                existingUser.setFirstName(firstName);
                changed = true;
            }
            if (isBlank(existingUser.getLastName())) {
                existingUser.setLastName(lastName);
                changed = true;
            }
            if (existingUser.getRoles().stream().noneMatch(role -> "ROLE_ADMIN".equals(role.getName()))) {
                existingUser.getRoles().add(adminRole);
                changed = true;
            }
            User savedUser = changed ? userRepository.save(existingUser) : existingUser;
            Set<String> existingRoles = savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
            log.info(
                    "[auth-seed] ensured admin user email={} phone={} roles={} status={} updated={}",
                    savedUser.getEmail(),
                    savedUser.getPhoneNumber(),
                    existingRoles,
                    savedUser.getStatus(),
                    changed
            );
        }, () -> {
            String encodedPassword = passwordEncoder.encode(adminPassword);
            User user = new User();
            user.setEmail(normalizedAdminEmail);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            assignSeedPhoneNumberIfAvailable(userRepository, user, normalizedAdminPhone, normalizedAdminEmail);
            user.setPasswordHash(encodedPassword);
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            user.getRoles().add(adminRole);
            User savedUser = userRepository.save(user);
            log.info(
                    "[auth-seed] created admin user email={} phone={} roles={} status={}",
                    savedUser.getEmail(),
                    savedUser.getPhoneNumber(),
                    savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                    savedUser.getStatus()
            );
        });
        seedCompany(
                roleRepository,
                userRepository,
                companyProfileRepository,
                passwordEncoder,
                companyEmail,
                companyPassword,
                companyPhoneNumber,
                companyName,
                companyRegistrationNumber,
                companyContactPerson,
                companyApprovalStatus
        );
    }

    private void seedCompany(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CompanyProfileRepository companyProfileRepository,
            PasswordEncoder passwordEncoder,
            String companyEmail,
            String companyPassword,
            String companyPhoneNumber,
            String companyName,
            String companyRegistrationNumber,
            String companyContactPerson,
            String companyApprovalStatus
    ) {
        String normalizedCompanyEmail = normalizeEmail(companyEmail);
        String normalizedCompanyPhone = normalizePhoneNumber(companyPhoneNumber);
        Role companyRole = roleRepository.findByName("ROLE_COMPANY").orElseThrow();
        CompanyApprovalStatus approvalStatus = CompanyApprovalStatus.valueOf(companyApprovalStatus.trim().toUpperCase(Locale.ROOT));
        User companyUser = userRepository.findByEmail(normalizedCompanyEmail).map(existingUser -> {
            boolean changed = false;
            existingUser.setEmail(normalizedCompanyEmail);
            if (isBlank(existingUser.getFirstName())) {
                existingUser.setFirstName(companyContactPerson);
                changed = true;
            }
            if (isBlank(existingUser.getLastName())) {
                existingUser.setLastName(companyName);
                changed = true;
            }
            changed |= assignSeedPhoneNumberIfAvailable(userRepository, existingUser, normalizedCompanyPhone, normalizedCompanyEmail);
            if (isMissingOrInvalidPasswordHash(existingUser.getPasswordHash())
                    || !passwordEncoder.matches(companyPassword, existingUser.getPasswordHash())) {
                existingUser.setPasswordHash(passwordEncoder.encode(companyPassword));
                changed = true;
            }
            if (existingUser.getStatus() != UserStatus.ACTIVE) {
                existingUser.setStatus(UserStatus.ACTIVE);
                changed = true;
            }
            if (!existingUser.isEmailVerified()) {
                existingUser.setEmailVerified(true);
                changed = true;
            }
            if (existingUser.getRoles().removeIf(role -> Set.of("ROLE_ADMIN", "ROLE_STUDENT").contains(role.getName()))) {
                changed = true;
            }
            if (existingUser.getRoles().add(companyRole)) {
                changed = true;
            }
            User savedUser = changed ? userRepository.save(existingUser) : existingUser;
            log.info(
                    "[auth-seed] ensured company user email={} phone={} roles={} status={} approvalStatus={}",
                    savedUser.getEmail(),
                    savedUser.getPhoneNumber(),
                    savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                    savedUser.getStatus(),
                    approvalStatus
            );
            return savedUser;
        }).orElseGet(() -> {
            String encodedCompanyPassword = passwordEncoder.encode(companyPassword);
            User user = new User();
            user.setEmail(normalizedCompanyEmail);
            user.setFirstName(companyContactPerson);
            user.setLastName(companyName);
            assignSeedPhoneNumberIfAvailable(userRepository, user, normalizedCompanyPhone, normalizedCompanyEmail);
            user.setPasswordHash(encodedCompanyPassword);
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            user.getRoles().add(companyRole);
            User savedUser = userRepository.save(user);
            log.info(
                    "[auth-seed] created company user email={} phone={} roles={} status={} approvalStatus={}",
                    savedUser.getEmail(),
                    savedUser.getPhoneNumber(),
                    savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                    savedUser.getStatus(),
                    approvalStatus
            );
            return savedUser;
        });

        companyProfileRepository.findByUserId(companyUser.getId()).ifPresentOrElse(existingProfile -> {
            existingProfile.setCompanyName(companyName);
            existingProfile.setOfficialEmail(normalizedCompanyEmail);
            existingProfile.setContactPersonName(companyContactPerson);
            existingProfile.setRegistrationNumber(companyRegistrationNumber);
            if (companyUser.getPhoneNumber() != null && !companyUser.getPhoneNumber().equals(existingProfile.getMobileNumber())) {
                existingProfile.setMobileNumber(companyUser.getPhoneNumber());
            }
            existingProfile.setStatus(approvalStatus);
            companyProfileRepository.save(existingProfile);
            log.info("[auth-seed] ensured seeded company profile email={} registrationNumber={} approvalStatus={}", normalizedCompanyEmail, companyRegistrationNumber, approvalStatus);
        }, () -> {
            CompanyProfile profile = new CompanyProfile();
            profile.setUserId(companyUser.getId());
            profile.setCompanyName(companyName);
            profile.setRegistrationNumber(companyRegistrationNumber);
            profile.setOfficialEmail(normalizedCompanyEmail);
            profile.setContactPersonName(companyContactPerson);
            profile.setMobileNumber(companyUser.getPhoneNumber());
            profile.setStatus(approvalStatus);
            companyProfileRepository.save(profile);
            log.info("[auth-seed] created company profile email={} registrationNumber={} approvalStatus={}", normalizedCompanyEmail, companyRegistrationNumber, approvalStatus);
        });
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
            throw new IllegalArgumentException("Invalid seeded phone number: " + phoneNumber);
        }
        return compact;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isMissingOrInvalidPasswordHash(String passwordHash) {
        return isBlank(passwordHash) || !passwordHash.startsWith("$2");
    }

    private boolean assignSeedPhoneNumberIfAvailable(
            UserRepository userRepository,
            User user,
            String phoneNumber,
            String email
    ) {
        if (isBlank(phoneNumber)) {
            return false;
        }
        Optional<User> phoneOwner = userRepository.findByPhoneNumber(phoneNumber);
        if (phoneOwner.isPresent() && (user.getId() == null || !phoneOwner.get().getId().equals(user.getId()))) {
            log.warn(
                    "[auth-seed] skipping phone assignment email={} phone={} reason=owned-by-other-user ownerId={}",
                    email,
                    phoneNumber,
                    phoneOwner.get().getId()
            );
            return false;
        }
        if (phoneNumber.equals(user.getPhoneNumber())) {
            return false;
        }
        user.setPhoneNumber(phoneNumber);
        return true;
    }

}

