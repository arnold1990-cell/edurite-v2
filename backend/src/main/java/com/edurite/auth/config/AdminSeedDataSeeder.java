package com.edurite.auth.config;

import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeedDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedDataSeeder.class);

    @Bean
    @Order(-1)
    @ConditionalOnProperty(prefix = "edurite.admin.seed", name = "enabled", havingValue = "true")
    ApplicationRunner adminSeedRunner(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${edurite.admin.email:}") String adminEmail,
            @Value("${edurite.admin.password:}") String adminPassword,
            @Value("${edurite.admin.first-name:}") String adminFirstName,
            @Value("${edurite.admin.last-name:}") String adminLastName
    ) {
        return createAdminSeedRunner(
                true,
                roleRepository,
                userRepository,
                passwordEncoder,
                adminEmail,
                adminPassword,
                adminFirstName,
                adminLastName
        );
    }

    ApplicationRunner createAdminSeedRunner(
            boolean enabled,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String adminEmail,
            String adminPassword,
            String adminFirstName,
            String adminLastName
    ) {
        return args -> {
            if (!enabled) {
                return;
            }

            seed(
                roleRepository,
                userRepository,
                passwordEncoder,
                adminEmail,
                adminPassword,
                adminFirstName,
                adminLastName
            );
        };
    }

    void seed(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String adminEmail,
            String adminPassword,
            String adminFirstName,
            String adminLastName
    ) {
        String normalizedEmail = normalizeEmail(requireValue(adminEmail, "EDURITE_ADMIN_EMAIL"));
        String rawPassword = requireValue(adminPassword, "EDURITE_ADMIN_PASSWORD");
        String firstName = requireValue(adminFirstName, "EDURITE_ADMIN_FIRST_NAME");
        String lastName = requireValue(adminLastName, "EDURITE_ADMIN_LAST_NAME");

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_ADMIN");
            return roleRepository.save(role);
        });

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).map(existing -> {
            boolean changed = false;

            if (!normalizedEmail.equals(normalizeEmail(existing.getEmail()))) {
                existing.setEmail(normalizedEmail);
                changed = true;
            }

            if (!firstName.equals(existing.getFirstName())) {
                existing.setFirstName(firstName);
                changed = true;
            }

            if (!lastName.equals(existing.getLastName())) {
                existing.setLastName(lastName);
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

            if (existing.getPasswordHash() == null || existing.getPasswordHash().isBlank()
                    || !passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
                existing.setPasswordHash(passwordEncoder.encode(rawPassword));
                changed = true;
            }

            if (existing.getRoles().stream().noneMatch(role -> "ROLE_ADMIN".equalsIgnoreCase(role.getName()))) {
                existing.getRoles().add(adminRole);
                changed = true;
            }

            User saved = changed ? userRepository.save(existing) : existing;
            log.info("[admin-seed] ensured admin user email={} userId={} roles={}",
                    saved.getEmail(),
                    saved.getId(),
                    saved.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
            return saved;
        }).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(normalizedEmail);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setPasswordHash(passwordEncoder.encode(rawPassword));
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setEmailVerified(true);
            newUser.getRoles().add(adminRole);

            User saved = userRepository.save(newUser);
            log.info("[admin-seed] created admin user email={} userId={} roles={}",
                    saved.getEmail(),
                    saved.getId(),
                    saved.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
            return saved;
        });
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String requireValue(String value, String envName) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new IllegalStateException(envName + " must be set when admin seeding is enabled.");
        }
        return trimmed;
    }
}

