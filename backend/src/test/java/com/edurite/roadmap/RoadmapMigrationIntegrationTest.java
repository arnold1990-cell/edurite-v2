package com.edurite.roadmap;

import java.sql.*;
import java.util.Arrays;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="EDURITE_TRIAL_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:.*")
class RoadmapMigrationIntegrationTest {
    @Test void allMigrationsApplyAndValidateInAnIsolatedSchema() throws Exception {
        String schema="roadmap_migration_test_"+UUID.randomUUID().toString().replace("-", "");
        String url=System.getenv("EDURITE_TRIAL_TEST_DB_URL");
        String user=System.getenv("EDURITE_TRIAL_TEST_DB_USER");
        String password=System.getenv("EDURITE_TRIAL_TEST_DB_PASSWORD");
        try(var connection=DriverManager.getConnection(url,user,password); var statement=connection.createStatement()) {
            statement.execute("CREATE SCHEMA "+schema);
            try {
                var flyway=Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").load();
                var latestVersion = Arrays.stream(flyway.info().pending())
                        .map(migration -> migration.getVersion()).filter(java.util.Objects::nonNull)
                        .max(java.util.Comparator.naturalOrder()).orElseThrow();
                flyway.migrate(); flyway.validate();
                assertThat(flyway.info().current().getVersion()).isEqualTo(latestVersion);
                try(var rows=statement.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_schema='"+schema+"' AND table_name='saved_career_roadmaps' AND column_name IN ('request_key','study_plan','completed_steps_json')")) {
                    rows.next(); assertThat(rows.getInt(1)).isEqualTo(3);
                }
            } finally { statement.execute("DROP SCHEMA "+schema+" CASCADE"); }
        }
    }
}
