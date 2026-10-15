import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;

/** Invoked only by validate-restore.py against its newly initialized loopback cluster. */
public class RestoreValidation {
    public static void main(String[] args) throws Exception {
        String url = args[0], migrations = args[1];
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/restore_validation"))
            throw new IllegalArgumentException("Isolated restore database required");
        String password = System.getenv("PGPASSWORD");
        try (var connection = DriverManager.getConnection(url, "restore_validator", password);
             var sql = connection.createStatement()) {
            try (var row = sql.executeQuery("SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1")) {
                if (!row.next() || !row.getString(1).equals("74"))
                    throw new IllegalStateException("Expected a production backup at V74");
            }
            List<String> tables = new ArrayList<>();
            try (var rows = sql.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename <> 'flyway_schema_history' ORDER BY tablename")) {
                while (rows.next()) tables.add(rows.getString(1));
            }
            Map<String, String> projections = new LinkedHashMap<>();
            int index = 0;
            for (String table : tables) {
                List<String> columns = new ArrayList<>();
                try (var columnsQuery = connection.prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name=? ORDER BY ordinal_position")) {
                    columnsQuery.setString(1, table);
                    try (var rows = columnsQuery.executeQuery()) { while (rows.next()) columns.add(quote(rows.getString(1))); }
                }
                String projection = "to_jsonb(t)";
                // Only the explicitly authorized V75/V76 changes may alter old rows.
                if (table.equals("users")) projection = "CASE WHEN lower(btrim(t.email))='s.mngaza07@gmail.com' AND t.email_verified AND NOT t.email_verification_required AND t.status='ACTIVE' AND t.deleted_at IS NULL THEN to_jsonb(t) - 'plan_type' - 'updated_at' ELSE to_jsonb(t) END";
                if (table.equals("pricing_plans")) projection = "CASE WHEN t.code='PLAN_BASIC' THEN to_jsonb(t) - 'name' - 'description' - 'billing_interval' ELSE to_jsonb(t) END";
                String query = "SELECT " + projection + " AS row FROM (SELECT " + String.join(",", columns) + " FROM public." + quote(table) + ") t";
                projections.put(table, query);
                sql.execute("CREATE TEMP TABLE restore_snapshot_" + index++ + " AS " + query);
            }
            var flyway = Flyway.configure().dataSource(url, "restore_validator", password)
                    .locations("filesystem:" + migrations).validateOnMigrate(true).cleanDisabled(true).load();
            flyway.migrate();
            flyway.validate();
            if (!flyway.info().current().getVersion().getVersion().equals("79"))
                throw new IllegalStateException("Unexpected target migration version");
            if (flyway.migrate().migrationsExecuted != 0)
                throw new IllegalStateException("Migrations were not a no-op on the second run");
            index = 0;
            for (var entry : projections.entrySet()) {
                try (var rows = sql.executeQuery("SELECT count(*) FROM (SELECT row FROM restore_snapshot_" + index++ + " EXCEPT ALL " + entry.getValue() + ") missing")) {
                    rows.next();
                    if (rows.getLong(1) != 0) throw new IllegalStateException("Existing rows changed in " + entry.getKey());
                }
            }
            System.out.println("RESTORE_DATA_PRESERVED tables=" + tables.size());
        }
    }
    private static String quote(String name) { return "\"" + name.replace("\"", "\"\"") + "\""; }
}
