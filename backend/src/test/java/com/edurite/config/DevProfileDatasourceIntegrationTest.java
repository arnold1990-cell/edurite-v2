package com.edurite.config;

import com.edurite.institution.universityinfo.repository.UniversityAdmissionRequirementRepository;
import com.edurite.institution.universityinfo.repository.UniversityProgrammeRepository;
import com.edurite.institution.universityinfo.repository.UniversityRetrievalLogRepository;
import com.edurite.learning.repository.LearningResourceRepository;
import com.edurite.user.repository.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
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

        insertUser(userId, email, "Schema", "Check", OffsetDateTime.parse("2026-01-10T09:00:00Z"));
        insertStudent(studentId, userId, "Schema", "Student", "Grade 12");
        insertSchool(schoolId, "Schema School");
        linkStudentToSchool(schoolId, studentId);

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

        assertThat(matchedUserIds).containsExactly(userId);
        assertThat(matchedCount).isEqualTo(1);
    }

    @Test
    void notificationFilterDeduplicatesJoinedRowsAndKeepsNewestFirstOrderingWithPagination() {
        UUID schoolId = UUID.randomUUID();
        String searchToken = "notif-filter-" + UUID.randomUUID();

        UUID oldestUserId = UUID.randomUUID();
        UUID middleUserId = UUID.randomUUID();
        UUID newestUserId = UUID.randomUUID();

        UUID oldestStudentId = UUID.randomUUID();
        UUID middleStudentId = UUID.randomUUID();
        UUID newestStudentId = UUID.randomUUID();

        UUID roleA = UUID.randomUUID();
        UUID roleB = UUID.randomUUID();
        UUID roleC = UUID.randomUUID();

        insertSchool(schoolId, "Ordering School");

        insertUser(oldestUserId, searchToken + "-old@example.com", "Order", "Old", OffsetDateTime.parse("2026-01-03T08:00:00Z"));
        insertUser(middleUserId, searchToken + "-mid@example.com", "Order", "Middle", OffsetDateTime.parse("2026-01-03T09:00:00Z"));
        insertUser(newestUserId, searchToken + "-new@example.com", "Order", "Newest", OffsetDateTime.parse("2026-01-03T10:00:00Z"));

        insertStudent(oldestStudentId, oldestUserId, "Order", "Old", "Grade 12");
        insertStudent(middleStudentId, middleUserId, "Order", "Middle", "Grade 12");
        insertStudent(newestStudentId, newestUserId, "Order", "Newest", "Grade 12");

        linkStudentToSchool(schoolId, oldestStudentId);
        linkStudentToSchool(schoolId, middleStudentId);
        linkStudentToSchool(schoolId, newestStudentId);

        insertRole(roleA, "ROLE_NOTIF_A_" + UUID.randomUUID());
        insertRole(roleB, "ROLE_NOTIF_B_" + UUID.randomUUID());
        insertRole(roleC, "ROLE_NOTIF_C_" + UUID.randomUUID());

        assignRole(oldestUserId, roleA);
        assignRole(middleUserId, roleB);
        assignRole(newestUserId, roleA);
        assignRole(newestUserId, roleC);

        List<UUID> firstPage = userRepository.findUserIdsByNotificationFilter(
                null,
                null,
                null,
                "Grade 12",
                schoolId,
                searchToken,
                true,
                PageRequest.of(0, 2)
        );
        List<UUID> secondPage = userRepository.findUserIdsByNotificationFilter(
                null,
                null,
                null,
                "Grade 12",
                schoolId,
                searchToken,
                true,
                PageRequest.of(1, 2)
        );
        long matchedCount = userRepository.countByNotificationFilter(
                null,
                null,
                null,
                "Grade 12",
                schoolId,
                searchToken,
                true
        );

        assertThat(firstPage).containsExactly(newestUserId, middleUserId);
        assertThat(firstPage).doesNotHaveDuplicates();
        assertThat(secondPage).containsExactly(oldestUserId);
        assertThat(secondPage).doesNotHaveDuplicates();
        assertThat(matchedCount).isEqualTo(3);
    }

    private void insertUser(UUID userId, String email, String firstName, String lastName, OffsetDateTime createdAt) {
        //noinspection SqlNoDataSourceInspection
        jdbcTemplate.update(
                """
                INSERT INTO public.users (id, email, password_hash, first_name, last_name, status, email_verified, must_change_password, plan_type, created_at, updated_at)
                VALUES (?, ?, 'hash', ?, ?, 'ACTIVE', TRUE, FALSE, 'BASIC', ?, ?)
                """,
                userId,
                email,
                firstName,
                lastName,
                createdAt,
                createdAt
        );
    }

    private void insertStudent(UUID studentId, UUID userId, String firstName, String lastName, String selectedGrade) {
        //noinspection SqlNoDataSourceInspection
        jdbcTemplate.update(
                """
                INSERT INTO public.students (id, user_id, first_name, last_name, selected_grade, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, now(), now())
                """,
                studentId,
                userId,
                firstName,
                lastName,
                selectedGrade
        );
    }

    private void insertSchool(UUID schoolId, String schoolName) {
        //noinspection SqlNoDataSourceInspection
        jdbcTemplate.update(
                """
                INSERT INTO public.school_profiles (id, school_name, created_at, updated_at)
                VALUES (?, ?, now(), now())
                """,
                schoolId,
                schoolName
        );
    }

    private void linkStudentToSchool(UUID schoolId, UUID studentId) {
        //noinspection SqlNoDataSourceInspection
        jdbcTemplate.update(
                """
                INSERT INTO public.school_students (id, school_id, student_id, created_at, updated_at)
                VALUES (?, ?, ?, now(), now())
                """,
                UUID.randomUUID(),
                schoolId,
                studentId
        );
    }

    private void insertRole(UUID roleId, String roleName) {
        //noinspection SqlNoDataSourceInspection
        jdbcTemplate.update(
                """
                INSERT INTO public.roles (id, name, created_at, updated_at)
                VALUES (?, ?, now(), now())
                """,
                roleId,
                roleName
        );
    }

    private void assignRole(UUID userId, UUID roleId) {
        //noinspection SqlNoDataSourceInspection
        jdbcTemplate.update(
                """
                INSERT INTO public.user_roles (user_id, role_id)
                VALUES (?, ?)
                """,
                userId,
                roleId
        );
    }

    private void assertTableExists(String tableName) {
        //noinspection SqlNoDataSourceInspection
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
        //noinspection SqlNoDataSourceInspection
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
