package com.edurite.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StartupVerificationLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupVerificationLogger.class);

    private final Flyway flyway;
    private final JdbcTemplate jdbcTemplate;

    public StartupVerificationLogger(Flyway flyway, JdbcTemplate jdbcTemplate) {
        this.flyway = flyway;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupVerification() {
        logFlywayStatus();
        logSubscriptionSchemaStatus();
        log.info("Backend startup verification complete: service is ready.");
    }

    private void logFlywayStatus() {
        MigrationInfoService info = flyway.info();
        MigrationInfo current = info.current();
        int appliedCount = info.applied().length;
        int pendingCount = info.pending().length;
        String currentVersion = current == null ? "none" : current.getVersion() + " - " + current.getDescription();
        log.info("Flyway migration status: current='{}', appliedCount={}, pendingCount={}",
                currentVersion, appliedCount, pendingCount);
    }

    private void logSubscriptionSchemaStatus() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT column_name, data_type
                FROM information_schema.columns
                WHERE table_name = 'subscriptions'
                  AND column_name IN ('trial_start_date', 'trial_end_date', 'premium_until', 'trial_used')
                ORDER BY column_name
                """
        );

        Map<String, String> columns = rows.stream().collect(Collectors.toMap(
                row -> String.valueOf(row.get("column_name")),
                row -> String.valueOf(row.get("data_type"))
        ));

        boolean complete = columns.containsKey("trial_start_date")
                && columns.containsKey("trial_end_date")
                && columns.containsKey("premium_until")
                && columns.containsKey("trial_used");

        if (complete) {
            log.info("Subscription schema validation check: required trial columns present: {}", columns);
        } else {
            log.error("Subscription schema validation check failed: missing one or more trial columns. Found={}", columns);
        }
    }
}

