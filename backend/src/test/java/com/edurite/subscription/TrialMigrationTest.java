package com.edurite.subscription;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;

/** Runs only against an explicitly supplied disposable LOCAL database. */
@EnabledIfEnvironmentVariable(named = "EDURITE_TRIAL_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
class TrialMigrationTest {
    @Test void backfillPreservesPaidPaymentsAndLearnerDataAndNeverRestartsTrial() throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("EDURITE_TRIAL_TEST_DB_URL"), "postgres", "")) {
            connection.setAutoCommit(false);
            try (var sql = connection.createStatement()) {
                // Temporary tables shadow public tables; rollback also removes all fixtures.
                sql.execute("CREATE TEMP TABLE users (id uuid PRIMARY KEY, created_at timestamptz, profile text)");
                sql.execute("CREATE TEMP TABLE roles (id uuid PRIMARY KEY, name text)");
                sql.execute("CREATE TEMP TABLE user_roles (user_id uuid, role_id uuid)");
                sql.execute("CREATE TEMP TABLE subscriptions (id uuid PRIMARY KEY, user_id uuid, plan_code text, status text, provider text, start_date date, end_date date, trial_start_date timestamptz, trial_end_date timestamptz, trial_used boolean, premium_until timestamptz, created_at timestamptz, updated_at timestamptz)");
                sql.execute("CREATE TEMP TABLE payments (subscription_id uuid, status text, amount numeric)");
                sql.execute("CREATE TEMP TABLE pricing_plans (code text, name text, description text, billing_interval text)");
                UUID role = UUID.randomUUID();
                sql.execute("INSERT INTO roles VALUES ('" + role + "', 'ROLE_STUDENT')");
                for (int i = 1; i <= 4; i++) {
                    String id = String.format("00000000-0000-0000-0000-%012d", i);
                    sql.execute("INSERT INTO users VALUES ('" + id + "', '2026-01-01T12:00:00Z', 'saved marks and profile')");
                    sql.execute("INSERT INTO user_roles VALUES ('" + id + "', '" + role + "')");
                    if (i == 4) continue; // Historical account without a subscription.
                    String code = i == 3 ? "PLAN_PREMIUM" : "PLAN_BASIC";
                    sql.execute("INSERT INTO subscriptions (id,user_id,plan_code,status,trial_start_date,trial_end_date,created_at) VALUES ('" + id + "','" + id + "','" + code + "','ACTIVE'," + (i == 1 ? "'2026-01-02T12:00:00Z','2026-02-02T12:00:00Z'" : "NULL,NULL") + ",'2026-01-01T12:00:00Z')");
                    if (i == 3) sql.execute("INSERT INTO payments VALUES ('" + id + "','COMPLETED',59)");
                }
                String migration = Files.readString(Path.of("src/main/resources/db/migration/V71__fourteen_day_basic_trial.sql"));
                sql.execute(migration);
                // Flyway normally runs once. Even an accidental re-execution cannot issue another trial.
                sql.execute(migration);
                try (var result = sql.executeQuery("SELECT trial_start_date,trial_end_date FROM subscriptions WHERE plan_code='PLAN_BASIC'")) {
                    int count = 0;
                    while (result.next()) {
                        assertThat(result.getObject(1, OffsetDateTime.class).toInstant()).isEqualTo(OffsetDateTime.parse("2026-01-01T12:00:00Z").toInstant());
                        assertThat(result.getObject(2, OffsetDateTime.class).toInstant()).isEqualTo(OffsetDateTime.parse("2026-01-15T12:00:00Z").toInstant());
                        count++;
                    }
                    assertThat(count).isEqualTo(3);
                }
                try (var result = sql.executeQuery("SELECT count(*) FROM subscriptions WHERE plan_code='PLAN_PREMIUM' AND status='ACTIVE' AND trial_start_date IS NULL")) { result.next(); assertThat(result.getInt(1)).isEqualTo(1); }
                try (var result = sql.executeQuery("SELECT count(*) FROM payments WHERE status='COMPLETED' AND amount=59")) { result.next(); assertThat(result.getInt(1)).isEqualTo(1); }
                try (var result = sql.executeQuery("SELECT count(*) FROM users WHERE profile='saved marks and profile'")) { result.next(); assertThat(result.getInt(1)).isEqualTo(4); }
                sql.execute("UPDATE subscriptions SET trial_end_date='2026-01-05T12:00:00Z' WHERE plan_code='PLAN_BASIC'");
                sql.execute(migration);
                try (var result = sql.executeQuery("SELECT count(*) FROM subscriptions WHERE plan_code='PLAN_BASIC' AND trial_end_date='2026-01-05T12:00:00Z'")) { result.next(); assertThat(result.getInt(1)).isEqualTo(3); }
            } finally { connection.rollback(); }
        }
    }
}
