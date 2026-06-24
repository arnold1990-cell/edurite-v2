package com.edurite.config;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIf(expression = "#{T(com.edurite.config.DevProfileDatasourceIntegrationTest).canRunWithDatabase()}")
@SuppressWarnings("resource")
class DevProfileDatasourceIntegrationTest {

    private static final String EXTERNAL_DB_URL = System.getenv("EDURITE_TEST_DB_URL");
    private static final boolean USE_EXTERNAL_DB = EXTERNAL_DB_URL != null && !EXTERNAL_DB_URL.isBlank();
    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DevProfileDatasourceIntegrationTest::jdbcUrl);
        registry.add("spring.datasource.username", DevProfileDatasourceIntegrationTest::jdbcUsername);
        registry.add("spring.datasource.password", DevProfileDatasourceIntegrationTest::jdbcPassword);
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.data.redis.repositories.enabled", () -> false);
        registry.add("spring.task.scheduling.enabled", () -> false);
        registry.add("security.jwt.secret", () -> "dev-profile-test-jwt-secret-32-bytes-minimum");
        registry.add("edurite.auth.seed.admin.email", () -> "admin@dev-profile.test");
        registry.add("edurite.auth.seed.admin.password", () -> "AdminPass@123");
    }

    @SuppressWarnings("unused")
    public static boolean canRunWithDatabase() {
        return USE_EXTERNAL_DB || dockerAvailable();
    }

    private static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static synchronized PostgreSQLContainer<?> postgres() {
        if (postgres == null) {
            postgres = startPostgresContainer();
        }
        return postgres;
    }

    private static PostgreSQLContainer<?> startPostgresContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("edurite_dev_profile")
                .withUsername("edurite")
                .withPassword("edurite");
        container.start();
        return container;
    }

    private static String jdbcUrl() {
        return USE_EXTERNAL_DB ? EXTERNAL_DB_URL : postgres().getJdbcUrl();
    }

    private static String jdbcUsername() {
        return USE_EXTERNAL_DB ? envOrDefault("EDURITE_TEST_DB_USERNAME", "postgres") : postgres().getUsername();
    }

    private static String jdbcPassword() {
        return USE_EXTERNAL_DB ? envOrDefault("EDURITE_TEST_DB_PASSWORD", "") : postgres().getPassword();
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devProfileUsesSingleDatasourceForFlywayAndJpa() {
        assertThat(dataSourceProperties.getUrl()).isEqualTo(jdbcUrl());
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);

        HikariDataSource hikari = (HikariDataSource) dataSource;
        assertThat(hikari.getJdbcUrl()).isEqualTo(jdbcUrl());

        assertThat(flyway.getConfiguration().getDataSource()).isSameAs(dataSource);
        assertThat(flyway.getConfiguration().getUrl()).isNull();
    }

    @Test
    void publicInstitutionSearchEndpointRemainsAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/institutions"))
                .andExpect(status().isOk());
    }

    @Test
    void curriculumAssetBinaryColumnsMapToPostgresBytea() {
        assertColumnIsBytea("pdf_bytes");
        assertColumnIsBytea("docx_bytes");
        assertColumnIsBytea("excel_bytes");
    }

    private void assertColumnIsBytea(String columnName) {
        String udtName = jdbcTemplate.queryForObject(
                """
                select udt_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'curriculum_assets'
                  and column_name = ?
                """,
                String.class,
                columnName
        );
        assertThat(udtName).isEqualTo("bytea");
    }
}

