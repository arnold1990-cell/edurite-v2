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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SchoolSeedDataSeeder {
    private static final Logger log = LoggerFactory.getLogger(SchoolSeedDataSeeder.class);
    private static final String SEEDED_SCHOOL_NAME = "EduRite";
    private static final String SEEDED_EMIS_NUMBER = "99999999";
    private static final String SEEDED_SCHOOL_STATUS = "ACTIVE";
    private static final String SEEDED_SCHOOL_ADMIN_ROLE = "ROLE_SCHOOL_ADMIN";

    @Bean
    @Order(1)
    @ConditionalOnProperty(prefix = "edurite.seed", name = "enabled", havingValue = "true")
    ApplicationRunner schoolSeedRunner(
            SchoolRepository schoolRepository,
            SchoolRegistrationRequestRepository schoolRegistrationRequestRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            DistrictRepository districtRepository,
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
            District seededDistrict = districtRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
            School school = schoolRepository.findByRegistrationNumberIgnoreCase(SEEDED_EMIS_NUMBER).orElseGet(School::new);
            school.setSchoolName(SEEDED_SCHOOL_NAME);
            school.setRegistrationNumber(SEEDED_EMIS_NUMBER);
            if (school.getSchoolCode() == null || school.getSchoolCode().isBlank()) {
                school.setSchoolCode("ESS");
            }
            if (school.getStatus() == null || school.getStatus().isBlank()) {
                school.setStatus(SEEDED_SCHOOL_STATUS);
            }
            if (isBlank(school.getProvince())) {
                school.setProvince(firstNonBlank(seededDistrict == null ? null : seededDistrict.getProvince(), "Gauteng"));
            }
            if (isBlank(school.getDistrict())) {
                school.setDistrict(firstNonBlank(seededDistrict == null ? null : seededDistrict.getDistrictName(), "Johannesburg"));
            }
            if (seededDistrict != null && school.getDistrictId() == null) {
                school.setDistrictId(seededDistrict.getId());
            }
            if (school.getProvince() == null || school.getProvince().isBlank()) {
                school.setProvince("Gauteng");
            }
            if (school.getContactEmail() == null || school.getContactEmail().isBlank()) {
                school.setContactEmail("school@edurite.com");
            }
            schoolRepository.save(school);

            User schoolAdminUser = ensureUserMapped(school, schoolUserProfileRepository, userRepository, roleRepository, passwordEncoder,
                    schoolAdminEmail, schoolAdminPassword, SEEDED_SCHOOL_ADMIN_ROLE, "School", "Admin");
            ensureSchoolRegistration(school, schoolRegistrationRequestRepository, schoolAdminUser, seededDistrict);
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
            User schoolAdminUser,
            District seededDistrict
    ) {
        if (seededDistrict == null) {
            log.warn("Skipping seeded school registration because no district exists.");
            return;
        }

        Optional<SchoolRegistrationRequest> byUserId = schoolRegistrationRequestRepository.findByUserId(schoolAdminUser.getId());
        Optional<SchoolRegistrationRequest> byEmis = schoolRegistrationRequestRepository.findByEmisNumberIgnoreCase(SEEDED_EMIS_NUMBER);

        if (byUserId.isPresent() && byEmis.isPresent() && !byUserId.get().getId().equals(byEmis.get().getId())) {
            schoolRegistrationRequestRepository.delete(byUserId.get());
        }

        SchoolRegistrationRequest registration = byEmis
                .orElseGet(() -> byUserId.orElseGet(SchoolRegistrationRequest::new));

        registration.setUserId(schoolAdminUser.getId());
        registration.setSchoolId(school.getId());
        registration.setSchoolName(SEEDED_SCHOOL_NAME);
        registration.setEmisNumber(SEEDED_EMIS_NUMBER);
        registration.setDistrictId(seededDistrict.getId());
        registration.setProvinceId(seededDistrict.getProvinceId());
        registration.setProvince(firstNonBlank(seededDistrict.getProvince(), school.getProvince(), "Gauteng"));
        registration.setDistrictName(firstNonBlank(seededDistrict.getDistrictName(), school.getDistrict(), "Johannesburg"));
        registration.setSchoolType("Public");
        registration.setPrincipalName("EduRite Admin");
        registration.setPrincipalEmail(schoolAdminUser.getEmail());
        registration.setSchoolEmail(firstNonBlank(school.getContactEmail(), "school@edurite.com"));
        registration.setPhoneNumber(firstNonBlank(school.getContactPhone(), "+26770000100"));
        registration.setPhysicalAddress(firstNonBlank(school.getAddress(), "EduRite Campus"));
        registration.setStatus(SchoolStatus.ACTIVE);
        if (registration.getSubmittedAt() == null) {
            registration.setSubmittedAt(OffsetDateTime.now());
        }
        if (registration.getApprovedAt() == null) {
            registration.setApprovedAt(OffsetDateTime.now());
        }
        schoolRegistrationRequestRepository.save(registration);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


