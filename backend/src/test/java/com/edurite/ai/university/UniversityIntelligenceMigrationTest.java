package com.edurite.ai.university;

import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="EDURITE_TEST_DB_URL",matches="jdbc:postgresql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/.*")
class UniversityIntelligenceMigrationTest {
    @Test void additiveUpgradePreservesExistingProgrammeRowsAndHistory() throws Exception {
        String url=System.getenv("EDURITE_TEST_DB_URL"),user=System.getenv("EDURITE_TEST_DB_USERNAME"),password=System.getenv("EDURITE_TEST_DB_PASSWORD");
        String schema="university_upgrade_"+UUID.randomUUID().toString().replace("-","");
        try(var db=DriverManager.getConnection(url,user,password);var statement=db.createStatement()) {
            statement.execute("CREATE SCHEMA "+schema);
            try {
                var base=Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").target("82").load();
                base.migrate();
                String table=schema+".university_programmes";
                UUID institution=UUID.randomUUID();
                statement.execute("INSERT INTO "+schema+".institutions(id,name) VALUES ('"+institution+"','Synthetic migration fixture')");
                statement.execute("INSERT INTO "+table+"(institution_id,name,source_url) VALUES ('"+institution+"','Synthetic qualification','https://fixture.example/programme')");
                String beforeHash;try(var rs=statement.executeQuery("SELECT md5(string_agg((to_jsonb(p)-'published_evidence_json')::text,'|' ORDER BY id)) FROM "+table+" p")) {rs.next();beforeHash=rs.getString(1);}
                long before;try(var rs=statement.executeQuery("SELECT count(*) FROM "+table)) {rs.next();before=rs.getLong(1);}
                var upgrade=Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").load();
                upgrade.migrate();upgrade.validate();assertThat(upgrade.migrate().migrationsExecuted).isZero();
                try(var rs=statement.executeQuery("SELECT count(*) FROM "+table)) {rs.next();assertThat(rs.getLong(1)).isEqualTo(before);}
                try(var rs=statement.executeQuery("SELECT md5(string_agg((to_jsonb(p)-'published_evidence_json')::text,'|' ORDER BY id)) FROM "+table+" p")) {rs.next();assertThat(rs.getString(1)).isEqualTo(beforeHash);}
                try(var rs=statement.executeQuery("SELECT count(*) FROM "+schema+".flyway_schema_history WHERE success")) {rs.next();assertThat(rs.getInt(1)).isEqualTo(83);}
                statement.executeQuery("SELECT published_evidence_json FROM "+table+" LIMIT 1").close();
                statement.executeQuery("SELECT metadata_json FROM "+schema+".university_directory_records").close();
            } finally {statement.execute("DROP SCHEMA "+schema+" CASCADE");}
        }
    }
}
