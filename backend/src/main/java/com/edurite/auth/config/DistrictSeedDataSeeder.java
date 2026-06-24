package com.edurite.auth.config;

import com.edurite.district.entity.District;
import com.edurite.district.entity.DistrictAdminProfile;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DistrictSeedDataSeeder {

    @Bean
    @Order(2)
    @ConditionalOnProperty(prefix = "edurite.seed", name = "enabled", havingValue = "true")
    ApplicationRunner districtSeedRunner(
            DistrictRepository districtRepository,
            DistrictAdminProfileRepository districtAdminProfileRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${edurite.auth.seed.district-admin.email:districtadmin@edurite.com}") String districtAdminEmail,
            @Value("${edurite.auth.seed.district-admin.password:DistrictAdmin@123}") String districtAdminPassword
    ) {
        return args -> {
            List<School> schools = schoolRepository.findAll();
            District district = districtRepository.findAll().stream().findFirst().orElseGet(() -> {
                if (schools.isEmpty()) {
                    return null;
                }
                School firstSchool = schools.getFirst();
                District created = new District();
                created.setDistrictName(firstSchool.getDistrict() == null || firstSchool.getDistrict().isBlank() ? "EduRite District" : firstSchool.getDistrict());
                created.setDistrictCode("EDURITE-DISTRICT");
                created.setProvince(firstSchool.getProvince());
                created.setContactEmail(firstSchool.getContactEmail());
                created.setLicensingStatus("ACTIVE");
                return districtRepository.save(created);
            });

            if (district != null) {
                for (School school : schools) {
                    if (school.getDistrictId() == null) {
                        school.setDistrictId(district.getId());
                        schoolRepository.save(school);
                    }
                }
            }

            Role role = roleRepository.findByName("ROLE_DISTRICT_ADMIN").orElseGet(() -> {
                Role created = new Role();
                created.setName("ROLE_DISTRICT_ADMIN");
                return roleRepository.save(created);
            });

            User user = userRepository.findByEmailIgnoreCase(districtAdminEmail.trim().toLowerCase(Locale.ROOT)).orElseGet(() -> {
                User created = new User();
                created.setEmail(districtAdminEmail.trim().toLowerCase(Locale.ROOT));
                created.setUsername("districtadmin");
                created.setFirstName("District");
                created.setLastName("Admin");
                created.setPasswordHash(passwordEncoder.encode(districtAdminPassword));
                created.setStatus(UserStatus.ACTIVE);
                created.setEmailVerified(true);
                return userRepository.save(created);
            });

            if (user.getRoles().stream().noneMatch(existing -> "ROLE_DISTRICT_ADMIN".equalsIgnoreCase(existing.getName()))) {
                user.getRoles().add(role);
                userRepository.save(user);
            }

            if (district != null) {
                districtAdminProfileRepository.findByUserIdAndDeletedFalse(user.getId()).orElseGet(() -> {
                    DistrictAdminProfile profile = new DistrictAdminProfile();
                    profile.setDistrictId(district.getId());
                    profile.setUserId(user.getId());
                    profile.setTitle("District Administrator");
                    profile.setActive(true);
                    profile.setDeleted(false);
                    return districtAdminProfileRepository.save(profile);
                });
            }

            ensureDistrictAdminAccount(districtRepository, districtAdminProfileRepository, userRepository, role, passwordEncoder,
                    "DC12", "Amathole", "Amathole District Director", "Amathole District Admin", "amathole.admin@edurite.com", "+27110000012");
            ensureDistrictAdminAccount(districtRepository, districtAdminProfileRepository, userRepository, role, passwordEncoder,
                    "BUF", "Buffalo City", "Buffalo City Director", "Buffalo City District Admin", "buffalocity.admin@edurite.com", "+27110000013");
            ensureDistrictAdminAccount(districtRepository, districtAdminProfileRepository, userRepository, role, passwordEncoder,
                    "DC13", "Chris Hani", "Chris Hani District Director", "Chris Hani District Admin", "chrishani.admin@edurite.com", "+27110000014");
        };
    }

    private void ensureDistrictAdminAccount(
            DistrictRepository districtRepository,
            DistrictAdminProfileRepository districtAdminProfileRepository,
            UserRepository userRepository,
            Role role,
            PasswordEncoder passwordEncoder,
            String districtCode,
            String districtName,
            String directorName,
            String adminName,
            String adminEmail,
            String phoneNumber
    ) {
        District district = districtRepository.findByDistrictCodeIgnoreCase(districtCode)
                .or(() -> districtRepository.findAll().stream().filter(item -> districtName.equalsIgnoreCase(item.getDistrictName())).findFirst())
                .orElse(null);
        if (district == null) {
            return;
        }

        if (district.getDirectorName() == null || district.getDirectorName().isBlank()) {
            district.setDirectorName(directorName);
        }
        if (district.getAdminName() == null || district.getAdminName().isBlank()) {
            district.setAdminName(adminName);
        }
        if (district.getAdminEmail() == null || district.getAdminEmail().isBlank()) {
            district.setAdminEmail(adminEmail);
            district.setContactEmail(adminEmail);
        }
        if (district.getPhoneNumber() == null || district.getPhoneNumber().isBlank()) {
            district.setPhoneNumber(phoneNumber);
            district.setContactPhone(phoneNumber);
        }
        if (district.getStatus() == null || district.getStatus().isBlank()) {
            district.setStatus("ACTIVE");
        }
        district.setActive(true);
        district.setLicensingStatus("ACTIVE");
        districtRepository.save(district);

        if (!districtAdminProfileRepository.findByDistrictIdAndActiveTrueAndDeletedFalse(district.getId()).isEmpty()) {
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(adminEmail.trim().toLowerCase(Locale.ROOT)).orElseGet(() -> {
            User created = new User();
            created.setEmail(adminEmail.trim().toLowerCase(Locale.ROOT));
            created.setUsername(districtName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".").replaceAll("^\\.+|\\.+$", "") + ".admin");
            created.setFirstName(adminName);
            created.setLastName("District Admin");
            created.setPhoneNumber(phoneNumber);
            created.setPasswordHash(passwordEncoder.encode("Temp@12345"));
            created.setStatus(UserStatus.ACTIVE);
            created.setEmailVerified(true);
            created.setMustChangePassword(true);
            return userRepository.save(created);
        });
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(districtName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".").replaceAll("^\\.+|\\.+$", "") + ".admin");
        }
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(phoneNumber);
        }
        user.setMustChangePassword(true);

        if (user.getRoles().stream().noneMatch(existing -> "ROLE_DISTRICT_ADMIN".equalsIgnoreCase(existing.getName()))) {
            user.getRoles().add(role);
        }
        userRepository.save(user);

        DistrictAdminProfile profile = new DistrictAdminProfile();
        profile.setDistrictId(district.getId());
        profile.setUserId(user.getId());
        profile.setTitle("District Administrator");
        profile.setActive(true);
        profile.setDeleted(false);
        districtAdminProfileRepository.save(profile);
    }
}
