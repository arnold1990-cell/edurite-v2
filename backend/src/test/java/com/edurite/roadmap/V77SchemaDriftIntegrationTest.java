package com.edurite.roadmap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import com.edurite.config.LocalV77Reconciliation;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.*;

/** Exercises the immutable V77 on disposable schemas, never the application schema. */
@EnabledIfEnvironmentVariable(named = "EDURITE_TRIAL_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
class V77SchemaDriftIntegrationTest {
    @Test
    void localCallbackPreservesOriginalColumnValuesAndViewDependencies() throws Exception {
        withV76((connection, schema, latestVersion) -> {
            UUID id = insertProgramme(connection, schema);
            try (var sql = connection.createStatement()) {
                sql.execute("ALTER TABLE " + schema + ".career_program_requirements ADD COLUMN qualification_type VARCHAR(120)");
                sql.execute("UPDATE " + schema + ".career_program_requirements SET qualification_type='Existing diploma'");
                sql.execute("CREATE VIEW " + schema + ".original_qualification AS SELECT qualification_type FROM "
                        + schema + ".career_program_requirements");
            }
            var flyway = localMigrations(schema, new LocalV77Reconciliation());
            flyway.migrate();
            flyway.validate();
            assertThat(version(connection, schema)).isEqualTo(latestVersion);
            try (var sql = connection.createStatement(); var row = sql.executeQuery("SELECT p.qualification_type, "
                    + "p.qualification_type_v77_added, v.qualification_type FROM " + schema
                    + ".career_program_requirements p CROSS JOIN " + schema + ".original_qualification v WHERE p.id='" + id + "'")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getString(1)).isEqualTo("Existing diploma");
                assertThat(row.getString(2)).isNull();
                assertThat(row.getString(3)).isEqualTo("Existing diploma");
            }
            try (var sql = connection.createStatement(); var row = sql.executeQuery("SELECT checksum FROM "
                    + schema + ".flyway_schema_history WHERE version='77'")) {
                row.next();
                assertThat(row.getInt(1)).isEqualTo(2050080389);
            }
            assertThat(flyway.migrate().migrationsExecuted).isZero();
        });
    }

    @Test
    void callbackAlsoSupportsFreshDatabase() throws Exception {
        withV76((connection, schema, latestVersion) -> {
            localMigrations(schema, new LocalV77Reconciliation()).migrate();
            assertThat(version(connection, schema)).isEqualTo(latestVersion);
            assertThat(columnCount(connection, schema, "qualification_type_v77_added")).isZero();
        });
    }

    @Test
    void callbackRefusesIncompatibleAndReservedColumns() throws Exception {
        for (String definition : new String[]{"qualification_type INTEGER", "qualification_type VARCHAR(80)",
                "qualification_type VARCHAR(120) NOT NULL DEFAULT 'unknown'", "qualification_type_pre_v77 VARCHAR(120)",
                "qualification_type_v77_added VARCHAR(120)"}) {
            withV76((connection, schema, latestVersion) -> {
                try (var sql = connection.createStatement()) {
                    sql.execute("ALTER TABLE " + schema + ".career_program_requirements ADD COLUMN " + definition);
                }
                assertThatThrownBy(() -> localMigrations(schema, new LocalV77Reconciliation()).migrate())
                        .hasMessageContaining("V77");
                assertThat(version(connection, schema)).isEqualTo("76");
                assertThat(columnCount(connection, schema, "aps_rule")).isZero();
            });
        }
    }

    @Test
    void failureAfterV77RollsBackBothCallbacksAndHistoricalSql() throws Exception {
        withV76((connection, schema, latestVersion) -> {
            insertProgramme(connection, schema);
            try (var sql = connection.createStatement()) {
                sql.execute("ALTER TABLE " + schema + ".career_program_requirements ADD COLUMN qualification_type VARCHAR(120)");
                sql.execute("UPDATE " + schema + ".career_program_requirements SET qualification_type='Preserve me'");
            }
            var delegate = new LocalV77Reconciliation();
            Callback failure = new Callback() {
                public boolean supports(Event event, Context context) { return delegate.supports(event, context); }
                public boolean canHandleInTransaction(Event event, Context context) { return true; }
                public String getCallbackName() { return "forced-failure"; }
                public void handle(Event event, Context context) {
                    delegate.handle(event, context);
                    if (event == Event.AFTER_EACH_MIGRATE) throw new IllegalStateException("Forced post-V77 failure");
                }
            };
            assertThatThrownBy(() -> localMigrations(schema, failure).migrate()).hasMessageContaining("Forced post-V77 failure");
            assertThat(version(connection, schema)).isEqualTo("76");
            assertThat(columnCount(connection, schema, "aps_rule")).isZero();
            assertThat(columnCount(connection, schema, "qualification_type_pre_v77")).isZero();
            assertThat(columnCount(connection, schema, "qualification_type_v77_added")).isZero();
            try (var sql = connection.createStatement(); var row = sql.executeQuery("SELECT qualification_type FROM "
                    + schema + ".career_program_requirements")) {
                row.next();
                assertThat(row.getString(1)).isEqualTo("Preserve me");
            }
            // A retry needs no repair, baseline, history edit or cleanup.
            localMigrations(schema, delegate).migrate();
            assertThat(version(connection, schema)).isEqualTo(latestVersion);
        });
    }

    private Flyway localMigrations(String schema, Callback callback) {
        return Flyway.configure().configuration(migrations(schema, null).getConfiguration()).callbacks(callback).load();
    }

    private int columnCount(Connection connection, String schema, String name) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema=? AND table_name='career_program_requirements' AND column_name=?")) {
            statement.setString(1, schema);
            statement.setString(2, name);
            try (var row = statement.executeQuery()) { row.next(); return row.getInt(1); }
        }
    }

    @Test
    void freshUpgradePreservesProgrammeAndAddsNullableMetadata() throws Exception {
        withV76((connection, schema, latestVersion) -> {
            UUID id = insertProgramme(connection, schema);
            migrations(schema, null).migrate();
            migrations(schema, null).validate();
            assertThat(version(connection, schema)).isEqualTo(latestVersion);
            try (var sql = connection.createStatement();
                 var row = sql.executeQuery("SELECT career_name,qualification_name,qualification_type,aps_rule FROM "
                         + schema + ".career_program_requirements WHERE id='" + id + "'")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getString(1)).isEqualTo("Synthetic audit career");
                assertThat(row.getString(2)).isEqualTo("Synthetic programme");
                assertThat(row.getString(3)).isNull();
                assertThat(row.getString(4)).isNull();
            }
            assertThat(migrations(schema, null).migrate().migrationsExecuted).isZero();
        });
    }

    @Test
    void preexistingCompatibleColumnFailsWithoutLosingValuesOrRecordingV77() throws Exception {
        assertDriftPreserved("VARCHAR(120)", "'Existing diploma'", "Existing diploma");
    }

    @Test
    void incompatibleExistingColumnIsNotSilentlyAcceptedOrConverted() throws Exception {
        assertDriftPreserved("INTEGER", "7", "7");
    }

    private void assertDriftPreserved(String type, String value, String expected) throws Exception {
        withV76((connection, schema, latestVersion) -> {
            UUID id = insertProgramme(connection, schema);
            try (var sql = connection.createStatement()) {
                // Simulated unmanaged DDL and synthetic data in this test's private schema only.
                sql.execute("ALTER TABLE " + schema + ".career_program_requirements ADD COLUMN qualification_type " + type);
                sql.execute("UPDATE " + schema + ".career_program_requirements SET qualification_type=" + value + " WHERE id='" + id + "'");
            }
            assertThatThrownBy(() -> migrations(schema, null).migrate()).satisfies(error -> {
                Throwable cause = error;
                while (cause != null && !(cause instanceof SQLException)) cause = cause.getCause();
                assertThat(cause).isInstanceOf(SQLException.class);
                assertThat(((SQLException) cause).getSQLState()).isEqualTo("42701");
            });
            assertThat(version(connection, schema)).isEqualTo("76");
            try (var sql = connection.createStatement()) {
                try (var row = sql.executeQuery("SELECT qualification_type FROM " + schema + ".career_program_requirements WHERE id='" + id + "'")) {
                    assertThat(row.next()).isTrue();
                    assertThat(row.getString(1)).isEqualTo(expected);
                }
                try (var row = sql.executeQuery("SELECT count(*) FROM " + schema + ".flyway_schema_history WHERE version='77'")) {
                    row.next();
                    assertThat(row.getInt(1)).isZero();
                }
                try (var row = sql.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_schema='" + schema
                        + "' AND table_name='career_program_requirements' AND column_name='aps_rule'")) {
                    row.next();
                    assertThat(row.getInt(1)).as("V77's earlier ADD COLUMN must roll back too").isZero();
                }
            }
        });
    }

    private UUID insertProgramme(Connection connection, String schema) throws SQLException {
        UUID id = UUID.randomUUID();
        try (var sql = connection.prepareStatement("INSERT INTO " + schema
                + ".career_program_requirements(id,career_name,institution_name,qualification_name) VALUES (?, ?, ?, ?)")) {
            sql.setObject(1, id);
            sql.setString(2, "Synthetic audit career");
            sql.setString(3, "Synthetic test institution");
            sql.setString(4, "Synthetic programme");
            sql.executeUpdate();
        }
        return id;
    }

    private String version(Connection connection, String schema) throws SQLException {
        try (var sql = connection.createStatement(); var row = sql.executeQuery("SELECT version FROM " + schema
                + ".flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")) {
            assertThat(row.next()).isTrue();
            return row.getString(1);
        }
    }

    private Flyway migrations(String schema, String target) {
        var configuration = Flyway.configure().dataSource(System.getenv("EDURITE_TRIAL_TEST_DB_URL"),
                System.getenv("EDURITE_TRIAL_TEST_DB_USER"), System.getenv("EDURITE_TRIAL_TEST_DB_PASSWORD"))
                .schemas(schema).defaultSchema(schema).locations("classpath:db/migration").cleanDisabled(true);
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private void withV76(Scenario scenario) throws Exception {
        String schema = "v77_regression_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(System.getenv("EDURITE_TRIAL_TEST_DB_URL"),
                System.getenv("EDURITE_TRIAL_TEST_DB_USER"), System.getenv("EDURITE_TRIAL_TEST_DB_PASSWORD"));
             var sql = connection.createStatement()) {
            sql.execute("CREATE SCHEMA " + schema);
            try {
                migrations(schema, "76").migrate();
                // Resolve the expected destination before the upgrade, independently of its result.
                String latestVersion = Arrays.stream(migrations(schema, null).info().pending())
                        .map(migration -> migration.getVersion()).filter(java.util.Objects::nonNull)
                        .max(java.util.Comparator.naturalOrder()).orElseThrow().getVersion();
                scenario.run(connection, schema, latestVersion);
            } finally {
                sql.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @FunctionalInterface
    private interface Scenario {
        void run(Connection connection, String schema, String latestVersion) throws Exception;
    }
}
