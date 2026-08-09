package com.edurite.auth.config;

import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("!prod")
public class PerfTestUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(PerfTestUserSeeder.class);
    private static final String ROLE_STUDENT = "ROLE_STUDENT";
    private static final String EMAIL_DOMAIN = "@example.test";
    private static final int DEFAULT_USER_COUNT = 100;
    private static final int MAX_USER_COUNT = 50_000;
    private static final int DEFAULT_BATCH_SIZE = 1_000;

    @Bean
    @Order(1)
    ApplicationRunner perfTestSeedRunner(
            RoleRepository roleRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate,
            Environment environment,
            @Value("${PERF_TEST_SEED_ENABLED:${perf.test.seed.enabled:false}}") boolean enabled,
            @Value("${PERF_TEST_USER_COUNT:${PERF_TEST_SEED_COUNT:${perf.test.seed.count:100}}}") int count,
            @Value("${PERF_TEST_SEED_EMAIL_PREFIX:${perf.test.seed.email-prefix:perf-user}}") String emailPrefix,
            @Value("${PERF_TEST_SEED_PASSWORD:${perf.test.seed.password:PerfTest@12345}}") String password,
            @Value("${PERF_TEST_SEED_BATCH_SIZE:${perf.test.seed.batch-size:1000}}") int batchSize
    ) {
        return args -> {
            if (!enabled) {
                return;
            }
            if (isProdProfileActive(environment)) {
                throw new IllegalStateException("PERF_TEST_SEED_ENABLED cannot be used with the prod profile.");
            }
            seed(
                    roleRepository,
                    userRepository,
                    studentProfileRepository,
                    passwordEncoder,
                    transactionTemplate,
                    count,
                    emailPrefix,
                    password,
                    batchSize
            );
        };
    }

    void seed(
            RoleRepository roleRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            PasswordEncoder passwordEncoder,
            int count,
            String emailPrefix,
            String password
    ) {
        seed(
                roleRepository,
                userRepository,
                studentProfileRepository,
                passwordEncoder,
                null,
                count,
                emailPrefix,
                password,
                DEFAULT_BATCH_SIZE
        );
    }

    void seed(
            RoleRepository roleRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate,
            int count,
            String emailPrefix,
            String password,
            int batchSize
    ) {
        validateSeedConfig(count, emailPrefix, password, batchSize);

        UUID studentRoleId = ensureStudentRole(roleRepository, transactionTemplate).getId();
        String encodedPassword = passwordEncoder.encode(password);

        BatchStats total = new BatchStats();
        for (int start = 1; start <= count; start += batchSize) {
            int end = Math.min(count, start + batchSize - 1);
            int batchStart = start;
            int batchEnd = end;
            BatchStats chunkStats = executeInTransaction(transactionTemplate, () -> seedChunk(
                    roleRepository,
                    userRepository,
                    studentProfileRepository,
                    studentRoleId,
                    encodedPassword,
                    batchStart,
                    batchEnd,
                    emailPrefix
            ));
            total.add(chunkStats);
            log.info(
                    "[perf-test-seed] batch ensured range={}..{} created={} updated={} profilesCreated={}",
                    start,
                    end,
                    chunkStats.created,
                    chunkStats.updated,
                    chunkStats.profilesCreated
            );
        }

        log.info(
                "[perf-test-seed] ensured synthetic users count={} created={} updated={} profilesCreated={} unchanged={} prefix={} domain={} batchSize={}",
                count,
                total.created,
                total.updated,
                total.profilesCreated,
                total.unchanged,
                emailPrefix,
                EMAIL_DOMAIN,
                batchSize
        );
    }

    private void validateSeedConfig(int count, String emailPrefix, String password, int batchSize) {
        if (count < 1) {
            throw new IllegalArgumentException("PERF_TEST_USER_COUNT must be at least 1.");
        }
        if (count > MAX_USER_COUNT) {
            throw new IllegalArgumentException("PERF_TEST_USER_COUNT must not exceed " + MAX_USER_COUNT + ".");
        }
        if (emailPrefix == null || emailPrefix.isBlank()) {
            throw new IllegalArgumentException("PERF_TEST_SEED_EMAIL_PREFIX must not be blank.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("PERF_TEST_SEED_PASSWORD must not be blank.");
        }
        if (batchSize < 1 || batchSize > MAX_USER_COUNT) {
            throw new IllegalArgumentException("PERF_TEST_SEED_BATCH_SIZE must be between 1 and " + MAX_USER_COUNT + ".");
        }
    }

    private Role ensureStudentRole(RoleRepository roleRepository, TransactionTemplate transactionTemplate) {
        return executeInTransaction(transactionTemplate, () -> roleRepository.findByName(ROLE_STUDENT).orElseGet(() -> {
            Role role = new Role();
            role.setName(ROLE_STUDENT);
            return roleRepository.save(role);
        }));
    }

    private BatchStats seedChunk(
            RoleRepository roleRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            UUID studentRoleId,
            String encodedPassword,
            int start,
            int end,
            String emailPrefix
    ) {
        Role studentRole = roleRepository.findById(studentRoleId)
                .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT disappeared during performance user seeding."));
        List<String> emails = new ArrayList<>(end - start + 1);
        for (int index = start; index <= end; index++) {
            emails.add(syntheticEmail(emailPrefix, index));
        }

        Map<String, User> existingByEmail = new LinkedHashMap<>();
        userRepository.findAllByLowerEmailIn(emails).forEach(user ->
                existingByEmail.put(user.getEmail().toLowerCase(Locale.ROOT), user));

        List<User> usersToSave = new ArrayList<>();
        List<User> chunkUsers = new ArrayList<>(emails.size());
        BatchStats stats = new BatchStats();

        for (int index = start; index <= end; index++) {
            String email = syntheticEmail(emailPrefix, index);
            User existing = existingByEmail.get(email);
            if (existing == null) {
                User createdUser = new User();
                createdUser.setEmail(email);
                createdUser.setFirstName("Perf");
                createdUser.setLastName("User " + index);
                createdUser.setPasswordHash(encodedPassword);
                createdUser.setStatus(UserStatus.ACTIVE);
                createdUser.setEmailVerified(true);
                createdUser.setMustChangePassword(false);
                createdUser.getRoles().add(studentRole);
                usersToSave.add(createdUser);
                chunkUsers.add(createdUser);
                stats.created++;
            } else {
                boolean changed = false;
                changed |= setIfDifferent(existing::getFirstName, existing::setFirstName, "Perf");
                changed |= setIfDifferent(existing::getLastName, existing::setLastName, "User " + index);
                if (existing.getStatus() != UserStatus.ACTIVE) {
                    existing.setStatus(UserStatus.ACTIVE);
                    changed = true;
                }
                if (!existing.isEmailVerified()) {
                    existing.setEmailVerified(true);
                    changed = true;
                }
                if (existing.isMustChangePassword()) {
                    existing.setMustChangePassword(false);
                    changed = true;
                }
                if (existing.getPasswordHash() == null || existing.getPasswordHash().isBlank()) {
                    existing.setPasswordHash(encodedPassword);
                    changed = true;
                }
                if (existing.getRoles().stream().noneMatch(role -> ROLE_STUDENT.equals(role.getName()))) {
                    existing.getRoles().add(studentRole);
                    changed = true;
                }
                if (changed) {
                    usersToSave.add(existing);
                    stats.updated++;
                } else {
                    stats.unchanged++;
                }
                chunkUsers.add(existing);
            }
        }

        if (!usersToSave.isEmpty()) {
            userRepository.saveAll(usersToSave);
        }

        List<UUID> userIds = chunkUsers.stream()
                .map(User::getId)
                .toList();
        Set<UUID> existingProfileUserIds = new HashSet<>();
        studentProfileRepository.findByUserIdIn(userIds).forEach(profile -> existingProfileUserIds.add(profile.getUserId()));

        List<StudentProfile> profilesToSave = new ArrayList<>();
        for (User user : chunkUsers) {
            if (!existingProfileUserIds.contains(user.getId())) {
                StudentProfile profile = new StudentProfile();
                profile.setUserId(user.getId());
                profile.setFirstName(user.getFirstName());
                profile.setLastName(user.getLastName());
                profile.setInterests("technology, software, career guidance");
                profile.setSkills("programming, communication, problem solving");
                profile.setQualificationLevel("Undergraduate");
                profile.setLocation("Gaborone");
                profile.setCareerGoals("software engineering");
                profile.setEmailNotificationsEnabled(false);
                profile.setSmsNotificationsEnabled(false);
                profilesToSave.add(profile);
            }
        }
        if (!profilesToSave.isEmpty()) {
            studentProfileRepository.saveAll(profilesToSave);
            stats.profilesCreated += profilesToSave.size();
        }

        return stats;
    }

    private <T> T executeInTransaction(TransactionTemplate transactionTemplate, java.util.function.Supplier<T> action) {
        if (transactionTemplate == null) {
            return action.get();
        }
        return transactionTemplate.execute(status -> action.get());
    }

    private boolean isProdProfileActive(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    }

    private String syntheticEmail(String emailPrefix, int index) {
        return "%s-%d%s".formatted(emailPrefix.trim().toLowerCase(Locale.ROOT), index, EMAIL_DOMAIN);
    }

    private boolean setIfDifferent(java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter, String value) {
        if (value.equals(getter.get())) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private static final class BatchStats {
        private int created;
        private int updated;
        private int unchanged;
        private int profilesCreated;

        private void add(BatchStats other) {
            created += other.created;
            updated += other.updated;
            unchanged += other.unchanged;
            profilesCreated += other.profilesCreated;
        }
    }
}
