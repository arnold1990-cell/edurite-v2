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
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SchoolSeedDataSeeder {

    @Bean
    @Order(1)
    @ConditionalOnProperty(prefix = "edurite.seed", name = "enabled", havingValue = "true")
    ApplicationRunner schoolSeedRunner(
            SchoolRepository schoolRepository,
            SchoolRegistrationRequestRepository schoolRegistrationRequestRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${edurite.auth.seed.school-admin.email:schooladmin@edurite.com}") String schoolAdminEmail,
            @Value("${edurite.auth.seed.school-admin.password:Admin@123}") String schoolAdminPassword,
            @Value("${edurite.auth.seed.teacher.email:teacher@edurite.com}") String teacherEmail,
            @Value("${edurite.auth.seed.teacher.password:Teacher@123}") String teacherPassword,
            @Value("${edurite.auth.seed.school-student.email:schoolstudent@edurite.com}") String learnerEmail,
            @Value("${edurite.auth.seed.school-student.password:SchoolStudent@123}") String learnerPassword,
            @Value("${edurite.auth.seed.demo-learner.email:arnold.student@edurite.com}") String demoLearnerEmail,
            @Value("${edurite.auth.seed.demo-learner.password:Student@123}") String demoLearnerPassword
    ) {
        return args -> {
            School school = schoolRepository.findByRegistrationNumberIgnoreCase("050020").orElseGet(School::new);
            school.setSchoolName("EduRite");
            school.setRegistrationNumber("050020");
            if (school.getSchoolCode() == null || school.getSchoolCode().isBlank()) {
                school.setSchoolCode("ESS");
            }
            if (school.getStatus() == null || school.getStatus().isBlank()) {
                school.setStatus("ACTIVE");
            }
            if (school.getProvince() == null || school.getProvince().isBlank()) {
                school.setProvince("Gauteng");
            }
            if (school.getDistrict() == null || school.getDistrict().isBlank()) {
                school.setDistrict("Johannesburg");
            }
            if (school.getContactEmail() == null || school.getContactEmail().isBlank()) {
                school.setContactEmail("school@edurite.com");
            }
            schoolRepository.save(school);

            User schoolAdminUser = ensureUserMapped(school, schoolUserProfileRepository, userRepository, roleRepository, passwordEncoder,
                    schoolAdminEmail, schoolAdminPassword, "ROLE_SCHOOL_ADMIN", "School", "Admin");
            ensureSchoolRegistration(school, schoolRegistrationRequestRepository, schoolAdminUser);
            ensureUserMapped(school, schoolUserProfileRepository, userRepository, roleRepository, passwordEncoder,
                    teacherEmail, teacherPassword, "ROLE_TEACHER", "EduRite", "Teacher");
            ensureUserMapped(school, schoolUserProfileRepository, userRepository, roleRepository, passwordEncoder,
                    learnerEmail, learnerPassword, "ROLE_SCHOOL_STUDENT", "School", "Learner");
            ensureUserMapped(school, schoolUserProfileRepository, userRepository, roleRepository, passwordEncoder,
                    demoLearnerEmail, demoLearnerPassword, "ROLE_SCHOOL_STUDENT", "Arnold Tyvern", "Madamombe");
        };
    }

    private User ensureUserMapped(
            School school,
            SchoolUserProfileRepository schoolUserProfileRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String password,
            String roleName,
            String firstName,
            String lastName
    ) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> {
            Role created = new Role();
            created.setName(roleName);
            return roleRepository.save(created);
        });

        User user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT)).orElseGet(() -> {
            User created = new User();
            created.setEmail(email.trim().toLowerCase(Locale.ROOT));
            created.setFirstName(firstName);
            created.setLastName(lastName);
            created.setPasswordHash(passwordEncoder.encode(password));
            created.setEmailVerified(true);
            created.setStatus(UserStatus.ACTIVE);
            return userRepository.save(created);
        });

        boolean userUpdated = false;
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(password));
            userUpdated = true;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            userUpdated = true;
        }
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userUpdated = true;
        }
        if (userUpdated) {
            userRepository.save(user);
        }

        if (user.getRoles().stream().noneMatch(r -> roleName.equalsIgnoreCase(r.getName()))) {
            user.getRoles().add(role);
            userRepository.save(user);
        }

        SchoolUserProfile profile = schoolUserProfileRepository.findByUserIdAndDeletedFalse(user.getId()).orElseGet(() -> {
            SchoolUserProfile created = new SchoolUserProfile();
            created.setUserId(user.getId());
            return created;
        });
        profile.setSchoolId(school.getId());
        profile.setRoleName(roleName);
        profile.setActive(true);
        profile.setDeleted(false);
        schoolUserProfileRepository.save(profile);
        return user;
    }

    private void ensureSchoolRegistration(
            School school,
            SchoolRegistrationRequestRepository schoolRegistrationRequestRepository,
            User schoolAdminUser
    ) {
        Optional<SchoolRegistrationRequest> byUserId = schoolRegistrationRequestRepository.findByUserId(schoolAdminUser.getId());
        Optional<SchoolRegistrationRequest> byEmis = schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase("050020");

        if (byUserId.isPresent() && byEmis.isPresent() && !byUserId.get().getId().equals(byEmis.get().getId())) {
            schoolRegistrationRequestRepository.delete(byUserId.get());
        }

        SchoolRegistrationRequest registration = byEmis
                .or(() -> byUserId.filter(existing -> byEmis.isEmpty()))
                .orElseGet(SchoolRegistrationRequest::new);

        registration.setUserId(schoolAdminUser.getId());
        registration.setSchoolId(school.getId());
        registration.setSchoolName("EduRite");
        registration.setEmisNumber("050020");
        registration.setProvince(Optional.ofNullable(school.getProvince()).filter(value -> !value.isBlank()).orElse("Gauteng"));
        registration.setDistrictName(Optional.ofNullable(school.getDistrict()).filter(value -> !value.isBlank()).orElse("Johannesburg"));
        registration.setSchoolType("Public");
        registration.setPrincipalName("EduRite Admin");
        registration.setPrincipalEmail(schoolAdminUser.getEmail());
        registration.setSchoolEmail(Optional.ofNullable(school.getContactEmail()).filter(value -> !value.isBlank()).orElse("school@edurite.com"));
        registration.setPhoneNumber(Optional.ofNullable(school.getContactPhone()).filter(value -> !value.isBlank()).orElse("+26770000100"));
        registration.setPhysicalAddress(Optional.ofNullable(school.getAddress()).filter(value -> !value.isBlank()).orElse("EduRite Campus"));
        registration.setStatus(SchoolStatus.ACTIVE);
        if (registration.getSubmittedAt() == null) {
            registration.setSubmittedAt(OffsetDateTime.now());
        }
        if (registration.getApprovedAt() == null) {
            registration.setApprovedAt(OffsetDateTime.now());
        }
        schoolRegistrationRequestRepository.save(registration);
    }
}


