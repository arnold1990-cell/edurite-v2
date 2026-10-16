package com.edurite.config;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Reconciles one known local schema drift without changing an applied SQL migration. */
@Component
@Profile("dev & !prod & !production")
public class LocalV77Reconciliation implements Callback {
    private static final Logger log = LoggerFactory.getLogger(LocalV77Reconciliation.class);
    private static final String PRESERVED = "qualification_type_pre_v77";
    private static final String ADDED = "qualification_type_v77_added";

    @Override
    public boolean supports(Event event, Context context) {
        return (event == Event.BEFORE_EACH_MIGRATE || event == Event.AFTER_EACH_MIGRATE)
                && context.getMigrationInfo() != null
                && "77".equals(String.valueOf(context.getMigrationInfo().getVersion()))
                && "V77__career_programme_scoring_metadata.sql".equals(context.getMigrationInfo().getScript());
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        try {
            Connection connection = context.getConnection();
            if (connection.getAutoCommit()) {
                throw new FlywayException("Local V77 reconciliation requires the migration transaction");
            }
            String schema;
            try (var statement = connection.createStatement(); var row = statement.executeQuery("SELECT current_schema()")) {
                row.next();
                schema = row.getString(1);
            }
            String table = quote(schema) + ".career_program_requirements";
            try (var statement = connection.createStatement()) {
                statement.execute("LOCK TABLE " + table + " IN ACCESS EXCLUSIVE MODE");
            }
            Map<String, Column> columns = columns(connection, schema);
            if (event == Event.BEFORE_EACH_MIGRATE) {
                if (columns.containsKey(PRESERVED) || columns.containsKey(ADDED)) {
                    throw new FlywayException("Local V77 reconciliation reserved column already exists; manual review required");
                }
                Column existing = columns.get("qualification_type");
                if (existing == null) return; // Fresh database: run the historical migration normally.
                requireLocal(connection);
                if (!new Column("character varying", 120, "YES", null, "NEVER", "NO").equals(existing)
                        || columns.containsKey("aps_rule")) {
                    throw new FlywayException("Incompatible local V77 schema drift; no changes made");
                }
                rename(connection, table, "qualification_type", PRESERVED);
            } else if (columns.containsKey(PRESERVED)) {
                requireLocal(connection);
                // Keep both columns. The original column (including values and dependencies) regains its name.
                rename(connection, table, "qualification_type", ADDED);
                rename(connection, table, PRESERVED, "qualification_type");
                log.warn("Reconciled local V77 drift in schema {}. Original qualification_type retained; "
                        + "V77's empty column preserved as {}. Historical migration unchanged.", schema, ADDED);
            }
        } catch (SQLException ex) {
            throw new FlywayException("Local V77 reconciliation failed; migration transaction must roll back", ex);
        }
    }

    static void requireLocal(Connection connection) throws SQLException {
        URI uri = URI.create(connection.getMetaData().getURL().substring("jdbc:".length()));
        if (!"postgresql".equals(uri.getScheme())
                || !java.util.Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(uri.getHost())) {
            throw new FlywayException("V77 reconciliation is restricted to loopback PostgreSQL in the dev profile");
        }
    }

    private static Map<String, Column> columns(Connection connection, String schema) throws SQLException {
        Map<String, Column> columns = new HashMap<>();
        try (var statement = connection.prepareStatement("""
                SELECT column_name, data_type, character_maximum_length, is_nullable, column_default,
                       is_generated, is_identity
                FROM information_schema.columns WHERE table_schema=? AND table_name='career_program_requirements'
                """)) {
            statement.setString(1, schema);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    columns.put(rows.getString(1), new Column(rows.getString(2), (Integer) rows.getObject(3),
                            rows.getString(4), rows.getString(5), rows.getString(6), rows.getString(7)));
                }
            }
        }
        return columns;
    }

    private static void rename(Connection connection, String table, String from, String to) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " RENAME COLUMN " + quote(from) + " TO " + quote(to));
        }
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String getCallbackName() {
        return "local-v77-reconciliation";
    }

    private record Column(String type, Integer length, String nullable, String defaultValue,
                          String generated, String identity) {}
}
