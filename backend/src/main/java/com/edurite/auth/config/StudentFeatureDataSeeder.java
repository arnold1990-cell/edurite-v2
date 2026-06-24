package com.edurite.auth.config;

import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.notification.repository.UserNotificationRepository;
import com.edurite.notification.service.NotificationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds optional demonstration data for student-facing feature modules.
 */
@Configuration
public class StudentFeatureDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(StudentFeatureDataSeeder.class);

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    @Order(1)
    @ConditionalOnProperty(prefix = "edurite.seed", name = "enabled", havingValue = "true")
    ApplicationRunner studentFeatureSeedRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            CompanyProfileRepository companyProfileRepository,
            CareerRepository careerRepository,
            BursaryRepository bursaryRepository,
            NotificationService notificationService,
            UserNotificationRepository userNotificationRepository,
            PasswordEncoder passwordEncoder,
            @Value("${edurite.auth.seed.student.email}") String studentEmail,
            @Value("${edurite.auth.seed.student.password}") String studentPassword,
            @Value("${edurite.auth.seed.student.first-name:Arnold}") String studentFirstName,
            @Value("${edurite.auth.seed.student.last-name:Madaz}") String studentLastName,
            @Value("${edurite.auth.seed.student.phone-number:+26775314557}") String studentPhoneNumber,
            @Value("${edurite.auth.seed.company.email}") String companyEmail,
            @Value("${edurite.auth.seed.company.password}") String companyPassword,
            @Value("${edurite.auth.seed.company.name:EduRite Company}") String companyName,
            @Value("${edurite.auth.seed.company.contact-person:Company Admin}") String companyContactPerson,
            @Value("${edurite.auth.seed.company.registration-number:EDURITE-COMPANY-001}") String companyRegistrationNumber,
            @Value("${edurite.auth.seed.company.phone-number:+26772523672}") String companyPhoneNumber
    ) {
        return args -> {
            Role studentRole = roleRepository.findByName("ROLE_STUDENT").orElseGet(() -> createRole(roleRepository, "ROLE_STUDENT"));
            Role companyRole = roleRepository.findByName("ROLE_COMPANY").orElseGet(() -> createRole(roleRepository, "ROLE_COMPANY"));

            String normalizedStudentEmail = normalizeEmail(studentEmail);
            String normalizedStudentPhone = normalizePhoneNumber(studentPhoneNumber);
            User studentUser = userRepository.findByEmail(normalizedStudentEmail).map(existing -> {
                boolean changed = false;
                existing.setEmail(normalizedStudentEmail);
                if (isBlank(existing.getFirstName())) {
                    existing.setFirstName(studentFirstName);
                    changed = true;
                }
                if (isBlank(existing.getLastName())) {
                    existing.setLastName(studentLastName);
                    changed = true;
                }
                changed |= assignSeedPhoneNumberIfAvailable(userRepository, existing, normalizedStudentPhone, normalizedStudentEmail);
                if (isMissingOrInvalidPasswordHash(existing.getPasswordHash())) {
                    existing.setPasswordHash(passwordEncoder.encode(studentPassword));
                    changed = true;
                }
                if (existing.getStatus() != UserStatus.ACTIVE) {
                    existing.setStatus(UserStatus.ACTIVE);
                    changed = true;
                }
                if (!existing.isEmailVerified()) {
                    existing.setEmailVerified(true);
                    changed = true;
                }
                if (existing.getRoles().add(studentRole)) {
                    changed = true;
                }
                return changed ? userRepository.save(existing) : existing;
            }).orElseGet(() -> {
                User user = new User();
                user.setEmail(normalizedStudentEmail);
                user.setFirstName(studentFirstName);
                user.setLastName(studentLastName);
                assignSeedPhoneNumberIfAvailable(userRepository, user, normalizedStudentPhone, normalizedStudentEmail);
                user.setStatus(UserStatus.ACTIVE);
                user.setEmailVerified(true);
                user.setPasswordHash(passwordEncoder.encode(studentPassword));
                user.getRoles().add(studentRole);
                return userRepository.save(user);
            });

            String normalizedCompanyEmail = normalizeEmail(companyEmail);
            String normalizedCompanyPhone = normalizePhoneNumber(companyPhoneNumber);
            User companyUser = userRepository.findByEmail(normalizedCompanyEmail).map(existing -> {
                boolean changed = false;
                existing.setEmail(normalizedCompanyEmail);
                if (isBlank(existing.getFirstName())) {
                    existing.setFirstName(companyContactPerson);
                    changed = true;
                }
                if (isBlank(existing.getLastName())) {
                    existing.setLastName(companyName);
                    changed = true;
                }
                changed |= assignSeedPhoneNumberIfAvailable(userRepository, existing, normalizedCompanyPhone, normalizedCompanyEmail);
                if (isMissingOrInvalidPasswordHash(existing.getPasswordHash())) {
                    existing.setPasswordHash(passwordEncoder.encode(companyPassword));
                    changed = true;
                }
                if (existing.getStatus() != UserStatus.ACTIVE) {
                    existing.setStatus(UserStatus.ACTIVE);
                    changed = true;
                }
                if (!existing.isEmailVerified()) {
                    existing.setEmailVerified(true);
                    changed = true;
                }
                if (existing.getRoles().removeIf(role -> "ROLE_STUDENT".equals(role.getName()) || "ROLE_ADMIN".equals(role.getName()))) {
                    changed = true;
                }
                if (existing.getRoles().add(companyRole)) {
                    changed = true;
                }
                return changed ? userRepository.save(existing) : existing;
            }).orElseGet(() -> {
                User user = new User();
                user.setEmail(normalizedCompanyEmail);
                user.setFirstName(companyContactPerson);
                user.setLastName(companyName);
                assignSeedPhoneNumberIfAvailable(userRepository, user, normalizedCompanyPhone, normalizedCompanyEmail);
                user.setStatus(UserStatus.ACTIVE);
                user.setEmailVerified(true);
                user.setPasswordHash(passwordEncoder.encode(companyPassword));
                user.getRoles().add(companyRole);
                return userRepository.save(user);
            });

            CompanyProfile company = companyProfileRepository.findByUserId(companyUser.getId()).map(existing -> {
                if (companyUser.getPhoneNumber() != null && !companyUser.getPhoneNumber().equals(existing.getMobileNumber())) {
                    existing.setMobileNumber(companyUser.getPhoneNumber());
                }
                if (isBlank(existing.getOfficialEmail())) {
                    existing.setOfficialEmail(normalizedCompanyEmail);
                }
                if (isBlank(existing.getContactPersonName())) {
                    existing.setContactPersonName(companyContactPerson);
                }
                return companyProfileRepository.save(existing);
            }).orElseGet(() -> {
                CompanyProfile profile = new CompanyProfile();
                profile.setUserId(companyUser.getId());
                profile.setCompanyName(companyName);
                profile.setRegistrationNumber(companyRegistrationNumber);
                profile.setOfficialEmail(normalizedCompanyEmail);
                profile.setContactPersonName(companyContactPerson);
                profile.setMobileNumber(companyUser.getPhoneNumber());
                profile.setStatus(com.edurite.company.entity.CompanyApprovalStatus.APPROVED);
                profile.setIndustry("Education");
                return companyProfileRepository.save(profile);
            });

            studentProfileRepository.findByUserId(studentUser.getId()).map(existing -> {
                if (isBlank(existing.getPhone())) {
                    existing.setPhone(studentUser.getPhoneNumber());
                }
                if (isBlank(existing.getFirstName())) {
                    existing.setFirstName(studentUser.getFirstName());
                }
                if (isBlank(existing.getLastName())) {
                    existing.setLastName(studentUser.getLastName());
                }
                return studentProfileRepository.save(existing);
            }).orElseGet(() -> {
                StudentProfile profile = new StudentProfile();
                profile.setUserId(studentUser.getId());
                profile.setFirstName(studentUser.getFirstName());
                profile.setLastName(studentUser.getLastName());
                profile.setLocation("Johannesburg");
                profile.setQualificationLevel("Undergraduate");
                profile.setInterests("Technology,Data Science");
                profile.setSkills("Java,Python");
                profile.setPhone(studentUser.getPhoneNumber());
                profile.setProfileCompleted(false);
                return studentProfileRepository.save(profile);
            });

            if (careerRepository.count() == 0) {
                Career c1 = new Career();
                c1.setTitle("Software Engineer");
                c1.setDescription("Build scalable applications.");
                c1.setIndustry("Technology");
                c1.setQualificationLevel("Undergraduate");
                c1.setLocation("Remote");
                c1.setDemandLevel("High");
                c1.setSalaryRange("R350k-R700k");
                careerRepository.save(c1);

                Career c2 = new Career();
                c2.setTitle("Data Analyst");
                c2.setDescription("Interpret data and derive insights.");
                c2.setIndustry("Technology");
                c2.setQualificationLevel("Diploma");
                c2.setLocation("Cape Town");
                c2.setDemandLevel("High");
                c2.setSalaryRange("R280k-R500k");
                careerRepository.save(c2);
            }

            if (bursaryRepository.count() == 0) {
                Bursary b1 = new Bursary();
                b1.setCompanyId(company.getId());
                b1.setTitle("STEM Excellence Bursary");
                b1.setQualificationLevel("Undergraduate");
                b1.setLocation("Gauteng");
                b1.setEligibility("Maths 70%+, South African citizen");
                b1.setFundingAmount(new BigDecimal("50000.00"));
                b1.setStatus("OPEN");
                b1.setApplicationEndDate(LocalDate.now().plusMonths(2));
                bursaryRepository.save(b1);
            }

            if (userNotificationRepository.countByUserId(studentUser.getId()) == 0) {
                notificationService.createInApp(studentUser.getId(), "BURSARY_ALERT", "New bursary available", "STEM Excellence Bursary matches your profile.");
                notificationService.createInApp(studentUser.getId(), "DEADLINE_REMINDER", "Deadline reminder", "Complete your application before Friday.");
            }
        };
    }

    private Role createRole(RoleRepository roleRepository, String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return roleRepository.save(role);
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
                    "[student-feature-seed] skipping phone assignment email={} phone={} reason=owned-by-other-user ownerId={}",
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

