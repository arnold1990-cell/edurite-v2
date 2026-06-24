package com.edurite.auth.config;

import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.Locale;
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
            SchoolUserProfileRepository schoolUserProfileRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${edurite.auth.seed.school-admin.email:schooladmin@edurite.com}") String schoolAdminEmail,
            @Value("${edurite.auth.seed.school-admin.password:SchoolAdmin@123}") String schoolAdminPassword,
            @Value("${edurite.auth.seed.teacher.email:teacher@edurite.com}") String teacherEmail,
            @Value("${edurite.auth.seed.teacher.password:Teacher@123}") String teacherPassword,
            @Value("${edurite.auth.seed.school-student.email:schoolstudent@edurite.com}") String learnerEmail,
            @Value("${edurite.auth.seed.school-student.password:SchoolStudent@123}") String learnerPassword,
            @Value("${edurite.auth.seed.demo-learner.email:arnold.student@edurite.com}") String demoLearnerEmail,
            @Value("${edurite.auth.seed.demo-learner.password:Student@123}") String demoLearnerPassword
    ) {
        return args -> {
            School school = schoolRepository.findAll().stream().findFirst().orElseGet(() -> {
                School created = new School();
                created.setSchoolName("EduRite Secondary School");
                created.setSchoolCode("ESS");
                created.setStatus("ACTIVE");
                created.setProvince("Gauteng");
                created.setDistrict("Johannesburg");
                created.setContactEmail("school@edurite.com");
                return schoolRepository.save(created);
            });
            if (school.getSchoolCode() == null || school.getSchoolCode().isBlank()) {
                school.setSchoolCode("ESS");
            }
            if (school.getStatus() == null || school.getStatus().isBlank()) {
                school.setStatus("ACTIVE");
            }
            schoolRepository.save(school);

            ensureUserMapped(school, schoolUserProfileRepository, userRepository, roleRepository, passwordEncoder,
                    schoolAdminEmail, schoolAdminPassword, "ROLE_SCHOOL_ADMIN", "School", "Admin");
            ensureUserMapped(school, schoolUserProfileRepository, userRepository, roleRepository, passwordEncoder,
                    teacherEmail, teacherPassword, "ROLE_TEACHER", "EduRite", "Teacher");
            ensureUserMapped(school, schoolUserProfileRepository, userRepository, roleRepository, passwordEncoder,
                    learnerEmail, learnerPassword, "ROLE_SCHOOL_STUDENT", "School", "Learner");
            ensureUserMapped(school, schoolUserProfileRepository, userRepository, roleRepository, passwordEncoder,
                    demoLearnerEmail, demoLearnerPassword, "ROLE_SCHOOL_STUDENT", "Arnold Tyvern", "Madamombe");
        };
    }

    private void ensureUserMapped(
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

        if (user.getRoles().stream().noneMatch(r -> roleName.equalsIgnoreCase(r.getName()))) {
            user.getRoles().add(role);
            userRepository.save(user);
        }

        schoolUserProfileRepository.findByUserIdAndDeletedFalse(user.getId()).orElseGet(() -> {
            SchoolUserProfile profile = new SchoolUserProfile();
            profile.setSchoolId(school.getId());
            profile.setUserId(user.getId());
            profile.setRoleName(roleName);
            profile.setActive(true);
            profile.setDeleted(false);
            return schoolUserProfileRepository.save(profile);
        });
    }
}


