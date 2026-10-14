package com.edurite.subscription;

import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.entity.PlanType;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.subscription.service.*;
import com.edurite.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated, rolled-back schema; no real accounts or subscriptions are modified. */
@EnabledIfEnvironmentVariable(named = "EDURITE_TRIAL_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
class ComplimentaryProGrantIntegrationTest {
    Connection connection;
    JdbcTemplate jdbc;
    String schema;
    boolean committedFixture;
    final UUID userId = UUID.randomUUID();
    final String recipient = "s.mngaza07@gmail.com";

    @BeforeEach void setup() throws Exception {
        connection = DriverManager.getConnection(System.getenv("EDURITE_TRIAL_TEST_DB_URL"),
                System.getenv().getOrDefault("EDURITE_TRIAL_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("EDURITE_TRIAL_TEST_DB_PASSWORD", ""));
        connection.setAutoCommit(false);
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        schema = "grant_test_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE SCHEMA " + schema);
        jdbc.execute("SET LOCAL search_path TO " + schema);
        jdbc.execute("CREATE TABLE users (id uuid PRIMARY KEY, email text UNIQUE NOT NULL, email_verified boolean NOT NULL, email_verification_required boolean NOT NULL, status text NOT NULL, deleted_at timestamptz, plan_type text DEFAULT 'BASIC', updated_at timestamptz)");
        jdbc.execute("CREATE TABLE subscriptions (id uuid PRIMARY KEY, user_id uuid REFERENCES users, plan_code text, status text, provider text, start_date date, end_date date, trial_used boolean, created_at timestamptz, updated_at timestamptz)");
        jdbc.execute("CREATE TABLE payments (id uuid PRIMARY KEY, subscription_id uuid REFERENCES subscriptions, status text, amount numeric)");
        jdbc.execute("CREATE TABLE student_ai_usage (id uuid PRIMARY KEY, user_id uuid, period_start date, status text, created_at timestamptz)");
    }

    @AfterEach void cleanup() throws Exception {
        if (connection != null) {
            connection.rollback();
            if (committedFixture) { jdbc.execute("DROP SCHEMA " + schema + " CASCADE"); connection.commit(); }
            connection.close();
        }
    }
    void migrate() throws Exception { jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V76__pending_complimentary_pro_grants.sql"))); }
    void account(UUID id, String email, boolean verified) {
        jdbc.update("INSERT INTO users(id,email,email_verified,email_verification_required,status) VALUES (?,?,?,?,'ACTIVE')", id, email, verified, !verified);
    }
    void verifyEmail() { jdbc.update("UPDATE users SET email_verified=true,email_verification_required=false WHERE id=?", userId); }
    int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class); }
    String state() { return jdbc.queryForObject("SELECT status FROM complimentary_pro_grants", String.class); }

    @Test void futureRegistrationWaitsForVerificationAndNormalizesEmail() throws Exception {
        migrate();
        assertThat(state()).isEqualTo("PENDING");
        account(userId, " \t S.MNGAZA07@GMAIL.COM \r\n ", false);
        assertThat(count("subscriptions")).isZero();
        assertThat(state()).isEqualTo("PENDING");
        verifyEmail();
        assertThat(state()).isEqualTo("ACTIVE");
        assertThat(count("subscriptions")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT user_id FROM complimentary_pro_grants", UUID.class)).isEqualTo(userId);
        assertThat(jdbc.queryForObject("SELECT activated_at IS NOT NULL FROM complimentary_pro_grants", Boolean.class)).isTrue();
    }

    @Test void existingVerifiedAccountReceivesAllProFeaturesAndAllowanceWithoutResettingUsageOrPaidSubscription() throws Exception {
        account(userId, recipient, true);
        UUID paid = UUID.randomUUID();
        jdbc.update("INSERT INTO subscriptions(id,user_id,plan_code,status,provider,start_date,end_date) VALUES (?,?,'PLAN_PREMIUM','ACTIVE','payfast',CURRENT_DATE,CURRENT_DATE+30)", paid, userId);
        jdbc.update("INSERT INTO payments VALUES (?,?,'COMPLETED',59)", UUID.randomUUID(), paid);
        for (int i = 0; i < 3; i++) jdbc.update("INSERT INTO student_ai_usage VALUES (?,?,date_trunc('month',now())::date,'SUCCEEDED',now())", UUID.randomUUID(), userId);
        migrate();
        var activation = jdbc.queryForMap("SELECT subscription_id,activated_at FROM complimentary_pro_grants");
        for (int i = 0; i < 4; i++) verifyEmail();
        assertThat(jdbc.queryForMap("SELECT subscription_id,activated_at FROM complimentary_pro_grants")).isEqualTo(activation);
        assertThat(count("subscriptions")).isEqualTo(2);
        assertThat(count("payments")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM subscriptions WHERE id=?", String.class, paid)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT amount FROM payments", Integer.class)).isEqualTo(59);
        assertThat(count("student_ai_usage")).isEqualTo(3);

        SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
        when(subscriptions.findByUserIdOrderByCreatedAtDesc(userId)).thenAnswer(invocation -> jdbc.query("SELECT * FROM subscriptions WHERE user_id=?", (rs, row) -> {
            var record = new SubscriptionRecord(); record.setId(rs.getObject("id", UUID.class)); record.setUserId(userId);
            record.setPlanCode(rs.getString("plan_code")); record.setStatus(rs.getString("status")); record.setProvider(rs.getString("provider"));
            if (rs.getDate("end_date") != null) record.setEndDate(rs.getDate("end_date").toLocalDate());
            return record;
        }, userId));
        var access = new StudentPlanAccessService(subscriptions, mock(UserRepository.class));
        var entitlements = new EntitlementService(access);
        assertThat(entitlements.plan(userId)).isEqualTo(PlanType.PRO);
        for (Feature feature : Feature.values()) assertThat(entitlements.hasFeature(userId, feature)).isTrue();
        var usage = new AiUsageService(jdbc, entitlements).current(userId);
        assertThat(usage.allowance()).isEqualTo(100);
        assertThat(usage.used()).isEqualTo(3);
        assertThat(usage.remaining()).isEqualTo(97);
        jdbc.update("UPDATE complimentary_pro_grants SET status='REVOKED',revoked_by='test-admin',revocation_reason='End testing'");
        verifyEmail();
        assertThat(state()).isEqualTo("REVOKED");
        assertThat(entitlements.plan(userId)).isEqualTo(PlanType.PREMIUM);
        assertThat(count("student_ai_usage")).isEqualTo(3);
        assertThat(count("subscriptions")).isEqualTo(2);
    }

    @Test void otherEmailsIncludingSimilarAddressesAreUnaffected() throws Exception {
        migrate();
        account(userId, "s.mngaza07+other@gmail.com", true);
        account(UUID.randomUUID(), "other@example.com", true);
        assertThat(state()).isEqualTo("PENDING");
        assertThat(count("subscriptions")).isZero();
    }

    @Test void existingUnverifiedAndAmbiguousAccountsRemainPending() throws Exception {
        account(userId, recipient, false);
        account(UUID.randomUUID(), "S.MNGAZA07@gmail.com", true);
        migrate();
        verifyEmail();
        assertThat(state()).isEqualTo("PENDING");
        assertThat(count("subscriptions")).isZero();
    }

    @Test void verifiedRegistrationActivatesAndRevokedPendingInvitationCannotReactivate() throws Exception {
        migrate();
        jdbc.update("UPDATE complimentary_pro_grants SET status='REVOKED',revoked_by='test-admin',revocation_reason='Withdraw invitation'");
        account(userId, recipient, true);
        verifyEmail();
        assertThat(state()).isEqualTo("REVOKED");
        assertThat(count("subscriptions")).isZero();
    }

    @Test void verifiedGoogleStyleRegistrationActivatesImmediately() throws Exception {
        migrate(); account(userId, recipient, true);
        assertThat(state()).isEqualTo("ACTIVE");
        assertThat(count("subscriptions")).isEqualTo(1);
    }

    @Test void verificationAndGrantRollBackTogether() throws Exception {
        migrate(); account(userId, recipient, false);
        Savepoint beforeVerification = connection.setSavepoint();
        verifyEmail(); assertThat(state()).isEqualTo("ACTIVE");
        connection.rollback(beforeVerification);
        assertThat(state()).isEqualTo("PENDING");
        assertThat(count("subscriptions")).isZero();
        assertThat(jdbc.queryForObject("SELECT email_verified FROM users WHERE id=?", Boolean.class, userId)).isFalse();
    }

    @Test void concurrentVerificationTransactionsCreateOnlyOneGrant() throws Exception {
        migrate(); account(userId, recipient, false);
        connection.commit(); committedFixture = true;
        jdbc.execute("SET search_path TO " + schema);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var tasks = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 2; i++) tasks.add(pool.submit(() -> {
                try (var concurrent = DriverManager.getConnection(System.getenv("EDURITE_TRIAL_TEST_DB_URL"),
                        System.getenv().getOrDefault("EDURITE_TRIAL_TEST_DB_USER", "postgres"),
                        System.getenv().getOrDefault("EDURITE_TRIAL_TEST_DB_PASSWORD", ""))) {
                    concurrent.setAutoCommit(false);
                    try (var sql = concurrent.createStatement()) {
                        sql.execute("SET LOCAL search_path TO " + schema);
                        sql.execute("SET LOCAL statement_timeout='10s'");
                        start.await();
                        sql.executeUpdate("UPDATE users SET email_verified=true,email_verification_required=false WHERE id='" + userId + "'");
                        concurrent.commit();
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
            }));
            start.countDown();
            for (var task : tasks) task.get(15, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(state()).isEqualTo("ACTIVE");
        assertThat(count("subscriptions")).isEqualTo(1);
        assertThat(count("payments")).isZero();
    }
}
