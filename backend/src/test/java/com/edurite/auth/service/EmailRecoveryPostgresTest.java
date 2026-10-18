package com.edurite.auth.service;

import com.edurite.auth.entity.EmailPasswordReset;
import com.edurite.auth.exception.InvalidOtpException;
import com.edurite.auth.repository.EmailPasswordResetRepository;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

// Opt-in: this test creates fixtures only in the dedicated, disposable local QA database.
@EnabledIfEnvironmentVariable(named = "RECOVERY_TEST_JDBC_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:55439/recovery_qa(_[a-z0-9]+)?")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EmailRecoveryPostgresTest.Config.class)
class EmailRecoveryPostgresTest {
    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = {UserRepository.class, EmailPasswordResetRepository.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.edurite.auth.repository.SchoolPasswordResetOtpRepository.class))
    static class Config {
        @Bean DataSource dataSource() {
            var source = new DriverManagerDataSource(System.getenv("RECOVERY_TEST_JDBC_URL"), "recovery_qa", "");
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("""
                CREATE TABLE users (
                    id UUID PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE, phone_number VARCHAR(30), username VARCHAR(255),
                    password_hash VARCHAR(255) NOT NULL, first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL,
                    status VARCHAR(255) NOT NULL, email_verified BOOLEAN NOT NULL, email_verification_required BOOLEAN NOT NULL DEFAULT FALSE,
                    email_verification_hash VARCHAR(64), email_verification_expires_at TIMESTAMPTZ, email_verification_sent_at TIMESTAMPTZ,
                    must_change_password BOOLEAN NOT NULL, deleted_at TIMESTAMPTZ, deletion_reason VARCHAR(255), last_login_at TIMESTAMPTZ,
                    plan_type VARCHAR(255) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
                )
                """);
            jdbc.execute("CREATE TABLE roles (id UUID PRIMARY KEY, name VARCHAR(255) NOT NULL UNIQUE, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)");
            jdbc.execute("CREATE TABLE user_roles (user_id UUID REFERENCES users(id), role_id UUID REFERENCES roles(id))");
            new ResourceDatabasePopulator(new ClassPathResource("db/migration/V81__email_password_recovery.sql"),
                new ClassPathResource("db/migration/V82__password_recovery_token_revocation.sql")).execute(source);
            return source;
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source); factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setManagedTypes(org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes.of(
                User.class.getName(), com.edurite.user.entity.Role.class.getName(), EmailPasswordReset.class.getName()));
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate",
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean SesV2Client ses() { return mock(SesV2Client.class); }
        @Bean EmailPasswordRecoveryService service(UserRepository users, EmailPasswordResetRepository resets, SesV2Client ses) {
            return new EmailPasswordRecoveryService(users, resets, new BCryptPasswordEncoder(4), ses, "info@edurite.co.za");
        }
    }
    @Autowired UserRepository users;
    @Autowired EmailPasswordResetRepository resets;
    @Autowired EmailPasswordRecoveryService service;
    @Autowired SesV2Client ses;
    @Autowired DataSource dataSource;

    @Test void migrationAttemptPersistenceAndConcurrentSingleUse() throws Exception {
        var encoder = new BCryptPasswordEncoder(4);
        User user = new User(); user.setEmail("postgres-fixture@example.com"); user.setFirstName("Recovery"); user.setLastName("Fixture");
        user.setStatus(UserStatus.ACTIVE); user.setEmailVerified(true); user.setPasswordHash(encoder.encode("Original@123"));
        user = users.save(user);
        service.requestOtp(user.getEmail());
        var sent = org.mockito.ArgumentCaptor.forClass(SendEmailRequest.class); verify(ses).sendEmail(sent.capture());
        String code = sent.getValue().content().simple().body().text().data().split("code is: ")[1].substring(0, 6);
        String wrong = code.equals("000000") ? "111111" : "000000";
        String email = user.getEmail();
        for (int i = 1; i <= 5; i++) {
            assertThatThrownBy(() -> service.resetWithOtp(email, wrong, "Changed@123")).isInstanceOf(InvalidOtpException.class);
            assertThat(resets.findByUserId(user.getId()).orElseThrow().getAttempts()).isEqualTo(i);
        }
        assertThatThrownBy(() -> service.verify(email, code)).isInstanceOf(InvalidOtpException.class);
        // Simulate time passing in this fixture without modifying any existing account.
        new JdbcTemplate(dataSource).update("UPDATE email_password_resets SET requested_at = now() - interval '2 minutes' WHERE user_id = ?", user.getId());
        service.requestOtp(email); verify(ses, times(2)).sendEmail(sent.capture());
        String fresh = sent.getValue().content().simple().body().text().data().split("code is: ")[1].substring(0, 6);
        String token = service.verify(email, fresh);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
            Callable<Boolean> reset = () -> { ready.countDown(); start.await();
                try { service.reset(email, token, "Changed@123"); return true; }
                catch (InvalidOtpException ex) { return false; }
            };
            var first = executor.submit(reset); var second = executor.submit(reset);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(encoder.matches("Changed@123", users.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
        assertThat(resets.findByUserId(user.getId()).orElseThrow().getResetTokenHash()).isNull();
        assertThat(users.findById(user.getId()).orElseThrow().getTokenVersion()).isEqualTo(1);
        assertThat(users.findById(user.getId()).orElseThrow().getPhoneNumber()).isNull();
    }
}
