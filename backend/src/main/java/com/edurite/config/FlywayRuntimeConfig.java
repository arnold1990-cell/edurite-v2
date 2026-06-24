package com.edurite.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRuntimeConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRuntimeConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return this::migrateWithRepairForDev;
    }

    private void migrateWithRepairForDev(Flyway flyway) {
        // TODO: REMOVE repair() before production deployment.
        log.warn("Flyway repair is running before migration (development safety mode).");
        flyway.repair();
        flyway.migrate();
        logMigrationSummary(flyway);
        verifySubscriptionTrialColumnsExist(flyway);
    }

    private void logMigrationSummary(Flyway flyway) {
        MigrationInfoService info = flyway.info();
        MigrationInfo current = info.current();
        int appliedCount = info.applied().length;
        int pendingCount = info.pending().length;
        String currentVersion = current == null ? "none" : current.getVersion() + " - " + current.getDescription();
        log.info("Flyway migration summary: current='{}', appliedCount={}, pendingCount={}",
                currentVersion, appliedCount, pendingCount);
    }

    private void verifySubscriptionTrialColumnsExist(Flyway flyway) {
        List<String> required = List.of("trial_start_date", "trial_end_date", "premium_until", "trial_used");
        List<String> missing = new ArrayList<>();
        // language=SQL
        // noinspection SqlNoDataSourceInspection,SqlResolve
        String sql = """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = 'subscriptions'
                  AND column_name IN ('trial_start_date', 'trial_end_date', 'premium_until', 'trial_used')
                """;

        try (Connection connection = flyway.getConfiguration().getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            List<String> present = new ArrayList<>();
            while (resultSet.next()) {
                present.add(resultSet.getString("column_name"));
            }
            for (String column : required) {
                if (!present.contains(column)) {
                    missing.add(column);
                }
            }
            if (!missing.isEmpty()) {
                log.error("Subscription schema verification failed after Flyway migrate. Missing columns: {}", missing);
                throw new IllegalStateException("Missing required subscriptions columns after Flyway migration: " + missing);
            }
            log.info("Subscription schema verification passed after Flyway migrate. Required columns are present: {}", required);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException) {
                throw (IllegalStateException) ex;
            }
            throw new IllegalStateException("Could not verify subscriptions trial columns after Flyway migration", ex);
        }
    }
}

