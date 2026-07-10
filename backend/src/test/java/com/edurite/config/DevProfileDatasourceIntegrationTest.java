package com.edurite.config;

import com.edurite.institution.universityinfo.repository.UniversityAdmissionRequirementRepository;
import com.edurite.institution.universityinfo.repository.UniversityProgrammeRepository;
import com.edurite.institution.universityinfo.repository.UniversityRetrievalLogRepository;
import com.edurite.learning.repository.LearningResourceRepository;
import com.edurite.user.repository.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LearningResourceRepository learningResourceRepository;

    @Autowired
    private UniversityProgrammeRepository universityProgrammeRepository;

    @Autowired
    private UniversityAdmissionRequirementRepository universityAdmissionRequirementRepository;

    @Autowired
    private UniversityRetrievalLogRepository universityRetrievalLogRepository;

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
        assertColumnUdtName("curriculum_assets", "pdf_bytes", "bytea");
        assertColumnUdtName("curriculum_assets", "docx_bytes", "bytea");
        assertColumnUdtName("curriculum_assets", "excel_bytes", "bytea");
    }

    @Test
    void flywayAppliesLearningCentreAndUniversityInformationMigrations() {
        List<String> appliedVersions = Arrays.stream(flyway.info().applied())
                .map(info -> info.getVersion().toString())
                .toList();

        assertThat(appliedVersions).contains("62", "63");
    }

    @Test
    void learningCentreAndUniversitySchemaMatchesRepositories() {
        assertTableExists("learning_resources");
        assertTableExists("university_programmes");
        assertTableExists("university_admission_requirements");
        assertTableExists("university_retrieval_logs");
        assertTableExists("user_roles");

        assertColumnUdtName("learning_resources", "course_url", "text");
        assertColumnUdtName("learning_resources", "thumbnail_url", "text");
        assertColumnUdtName("university_programmes", "institution_id", "uuid");
        assertColumnUdtName("university_admission_requirements", "institution_id", "uuid");
        assertColumnUdtName("university_admission_requirements", "programme_id", "uuid");
        assertColumnUdtName("university_retrieval_logs", "institution_id", "uuid");

        assertThat(learningResourceRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(universityProgrammeRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(universityAdmissionRequirementRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(universityRetrievalLogRepository.count()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void notificationFilterSchoolQueryMatchesStudentForeignKeySchema() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        String email = "schema-check-" + userId + "@example.com";

        jdbcTemplate.update(
                """
                INSERT INTO public.users (id, email, password_hash, first_name, last_name, status, email_verified, must_change_password, plan_type, created_at, updated_at)
                VALUES (?, ?, 'hash', 'Schema', 'Check', 'ACTIVE', TRUE, FALSE, 'BASIC', now(), now())
                """,
                userId,
                email
        );

        jdbcTemplate.update(
                """
                INSERT INTO public.students (id, user_id, first_name, last_name, selected_grade, created_at, updated_at)
                VALUES (?, ?, 'Schema', 'Student', 'Grade 12', now(), now())
                """,
                studentId,
                userId
        );

        jdbcTemplate.update(
                """
                INSERT INTO public.school_profiles (id, school_name, created_at, updated_at)
                VALUES (?, 'Schema School', now(), now())
                """,
                schoolId
        );

        jdbcTemplate.update(
                """
                INSERT INTO public.school_students (id, school_id, student_id, created_at, updated_at)
                VALUES (?, ?, ?, now(), now())
                """,
                UUID.randomUUID(),
                schoolId,
                studentId
        );

        List<UUID> matchedUserIds = userRepository.findUserIdsByNotificationFilter(
                null,
                null,
                null,
                null,
                schoolId,
                email,
                true,
                PageRequest.of(0, 10)
        );

        long matchedCount = userRepository.countByNotificationFilter(
                null,
                null,
                null,
                null,
                schoolId,
                email,
                true
        );

        assertThat(matchedUserIds).contains(userId);
        assertThat(matchedCount).isEqualTo(1);
    }

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name = ?
                """,
                Integer.class,
                tableName
        );
        assertThat(count).isEqualTo(1);
    }

    private void assertColumnUdtName(String tableName, String columnName, String expectedUdtName) {
        String udtName = jdbcTemplate.queryForObject(
                """
                select udt_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                  and column_name = ?
                """,
                String.class,
                tableName,
                columnName
        );
        assertThat(udtName).isEqualTo(expectedUdtName);
    }
}