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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIf(expression = "#{T(com.edurite.config.DevProfileDatasourceIntegrationTest).canRunWithDatabase()}")
@SuppressWarnings("resource")
class DevProfileDatasourceIntegrationTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private software.amazon.awssdk.services.sesv2.SesV2Client verificationSes;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.edurite.ai.service.AiProviderOrchestratorService journeyAi;

    @Test
    void registrationVerificationLoginAndAcademicProfilePersistThroughHttp() throws Exception {
        String email = "verification-" + UUID.randomUUID() + "@example.test";
        String password = "Isolated-Learner-2026!";
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        org.mockito.Mockito.when(verificationSes.sendEmail(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.sesv2.model.SendEmailRequest.class)))
                .thenReturn(software.amazon.awssdk.services.sesv2.model.SendEmailResponse.builder().messageId("isolated-test").build());
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of(
                "firstName", "Synthetic", "lastName", "Learner", "email", email,
                "phoneNumber", "+278" + String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100000000)),
                "password", password, "popiaConsentAccepted", true))))
                .andExpect(status().isCreated());
        String login = mapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(login))
                .andExpect(status().isForbidden());
        var captured = org.mockito.ArgumentCaptor.forClass(software.amazon.awssdk.services.sesv2.model.SendEmailRequest.class);
        org.mockito.Mockito.verify(verificationSes).sendEmail(captured.capture());
        String body = captured.getValue().content().simple().body().text().data();
        String token = body.split("token=")[1].split("\\s")[0];
        assertThat(body).contains("https://edurite.co.za/verify-email?");
        String storedHash = jdbcTemplate.queryForObject("SELECT email_verification_hash FROM users WHERE email=?", String.class, email);
        assertThat(storedHash).hasSize(64).isNotEqualTo(token);
        mockMvc.perform(post("/api/auth/email-verification/resend").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("email", email)))).andExpect(status().isOk());
        org.mockito.Mockito.verify(verificationSes, org.mockito.Mockito.times(1)).sendEmail(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.sesv2.model.SendEmailRequest.class));
        String verification = mapper.writeValueAsString(java.util.Map.of("email", email, "token", token));
        mockMvc.perform(post("/api/auth/email-verification/verify").contentType("application/json").content(verification)).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/email-verification/verify").contentType("application/json").content(verification)).andExpect(status().isConflict());
        String session = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(login))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + mapper.readTree(session).path("accessToken").asText();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/student/profile")
                .header("Authorization", bearer).contentType("application/json")
                .content("{\"selectedGrade\":\"Grade 12\",\"subjectAchievements\":[{\"subjectName\":\"Mathematics\",\"markPercentage\":63}]}"))
                .andExpect(status().isOk());
        String nextSession = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(login))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String nextBearer = "Bearer " + mapper.readTree(nextSession).path("accessToken").asText();
        mockMvc.perform(get("/api/student/profile").header("Authorization", nextBearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedGrade").value("Grade 12"))
                .andExpect(jsonPath("$.subjectAchievements[0].markPercentage").value(63));
        assertThat(jdbcTemplate.queryForObject("SELECT email_verified AND NOT email_verification_required AND email_verification_hash IS NULL FROM users WHERE email=?", Boolean.class, email)).isTrue();

        // Synthetic entitlement fixture only; no real invitation or purchased access is changed.
        jdbcTemplate.update("INSERT INTO subscriptions(user_id,plan_code,status,provider,start_date,end_date,trial_used) SELECT id,'PLAN_PRO','ACTIVE','integration-test',current_date,current_date+30,true FROM users WHERE email=?", email);
        mockMvc.perform(get("/api/recommendations/me").header("Authorization", nextBearer)).andExpect(status().isOk());
        String catalogue = mockMvc.perform(get("/api/careers?size=1").header("Authorization", nextBearer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String career = mapper.readTree(catalogue).path("content").get(0).path("title").asText();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/student/career-roadmaps/selection")
                .header("Authorization", nextBearer).contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("careerName", career)))).andExpect(status().isOk());
        String aiRoadmap = mapper.writeValueAsString(java.util.Map.of("careerName", career,
                "overview", java.util.Map.of("description", "Synthetic educational roadmap"),
                "universityPathway", List.of(java.util.Map.of("title", "Research", "description", "Review institution sources")),
                "roadmapTimeline", List.of(java.util.Map.of("stage", 1, "title", "Prepare", "description", "Practise saved subjects"))));
        String step = "{\"title\":\"Weekly practice\",\"focus\":\"Mathematics diagnostics\",\"actions\":[\"Monday 25 minutes\",\"Solve five equations\",\"Review errors on Friday\"]}";
        org.mockito.Mockito.when(journeyAi.generateContent(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            if (prompt.contains("Create a complete, realistic weekly study plan")) return "[" + String.join(",", java.util.Collections.nCopies(4, step)) + "]";
            if (prompt.contains("learner career roadmap")) return aiRoadmap;
            return "Step 1: Subtract 2 from both sides. Step 2: Divide by 3. The solution is x = 4.";
        });
        String roadmap = mockMvc.perform(post("/api/student/career-roadmaps/generate").header("Authorization", nextBearer)
                .contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of("careerName", career))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/student/career-roadmaps/save").header("Authorization", nextBearer)
                .contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of("careerName", career, "roadmap", mapper.readTree(roadmap)))))
                .andExpect(status().isOk());
        String planRequest = mapper.writeValueAsString(java.util.Map.of("requestKey", UUID.randomUUID(), "careerName", career));
        String plan = mockMvc.perform(post("/api/student/career-roadmaps/study-plans").header("Authorization", nextBearer)
                .contentType("application/json").content(planRequest)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String planId = mapper.readTree(plan).path("id").asText();
        mockMvc.perform(post("/api/student/career-roadmaps/study-plans").header("Authorization", nextBearer)
                .contentType("application/json").content(planRequest)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(planId));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/student/career-roadmaps/study-plans/" + planId + "/progress")
                .header("Authorization", nextBearer).contentType("application/json").content("{\"completedSteps\":[0]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedSteps[0]").value(0));
        mockMvc.perform(get("/api/student/career-roadmaps/study-plans").header("Authorization", nextBearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(planId));
        String tutor = mockMvc.perform(post("/api/student/tutor/ask").header("Authorization", nextBearer).contentType("application/json")
                .content("{\"subject\":\"MATHEMATICS\",\"question\":\"Solve 3x + 2 = 14\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("x = 4")))
                .andReturn().getResponse().getContentAsString();
        String sessionId = mapper.readTree(tutor).path("sessionId").asText();
        mockMvc.perform(get("/api/student/tutor/sessions/" + sessionId).header("Authorization", nextBearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.messages.length()").value(2));
    }

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
        registry.add("PERF_TEST_SEED_ENABLED", () -> false);
        registry.add("spring.jpa.show-sql", () -> false);
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

    @Test
    void adminLoginOverridesHistoryAndEntitlementsPersistAcrossSessions() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String password = "Synthetic-Admin-Login-2026!";
        UUID adminId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        String adminEmail = "admin-subscriptions-" + adminId + "@example.test";
        String learnerEmail = "managed-" + learnerId + "@example.test";
        insertUser(adminId, adminEmail, "Subscription", "Administrator", OffsetDateTime.now());
        insertUser(learnerId, learnerEmail, "Managed", "Learner", OffsetDateTime.now().minusDays(60));
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password);
        jdbcTemplate.update("UPDATE users SET password_hash=? WHERE id IN (?,?)", hash, adminId, learnerId);
        jdbcTemplate.update("UPDATE users SET username=? WHERE id=?", "managed-username-" + learnerId, learnerId);
        UUID adminRole = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name='ROLE_ADMIN'", UUID.class);
        UUID studentRole = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name='ROLE_STUDENT'", UUID.class);
        assignRole(adminId, adminRole);
        assignRole(learnerId, studentRole);
        insertStudent(UUID.randomUUID(), learnerId, "Managed", "Learner", "Grade 12");

        String loginRequest = mapper.writeValueAsString(java.util.Map.of("email", adminEmail, "password", password));
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("email", adminEmail, "password", "Incorrect-password!"))))
                .andExpect(status().isUnauthorized());
        String session = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(loginRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + mapper.readTree(session).path("accessToken").asText();
        mockMvc.perform(get("/api/admin/subscriptions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/subscriptions").with(user(learnerEmail).roles("STUDENT"))).andExpect(status().isForbidden());
        // Even a forged/stale ADMIN authority cannot bypass the database role check.
        mockMvc.perform(post("/api/admin/subscriptions/" + learnerId + "/override").with(user(learnerEmail).roles("ADMIN"))
                .contentType("application/json").content("{\"plan\":\"PRO\",\"reason\":\"Forged role\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/subscriptions").header("Authorization", bearer)
                .param("search", "managed-username-" + learnerId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].userId").value(learnerId.toString()));
        for (String invalid : new String[]{"{\"plan\":\"ENTERPRISE\",\"reason\":\"Invalid\"}",
                "{\"plan\":\"PRO\",\"reason\":\"   \"}"}) {
            mockMvc.perform(post("/api/admin/subscriptions/" + learnerId + "/override").header("Authorization", bearer)
                    .contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
        }
        // Every supported upgrade and downgrade, including BASIC -> PRO and PRO -> BASIC.
        String[] plans = {"PREMIUM", "PRO", "BASIC", "PRO", "PREMIUM", "BASIC"};
        long payments = jdbcTemplate.queryForObject("SELECT count(*) FROM payments", Long.class);
        for (String plan : plans) {
            mockMvc.perform(post("/api/v1/admin/subscriptions/" + learnerId + "/override").header("Authorization", bearer)
                    .contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of("plan", plan, "reason", "Synthetic " + plan + " access review"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.currentPlan").value(plan))
                    .andExpect(jsonPath("$.source").value("ADMIN_OVERRIDE"));
            mockMvc.perform(get("/api/subscriptions/entitlements").with(user(learnerEmail).roles("STUDENT")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.plan").value(plan))
                    .andExpect(jsonPath("$.source").value("ADMIN_OVERRIDE"))
                    .andExpect(jsonPath("$.aiUsage.allowance").value(plan.equals("PRO") ? 100 : plan.equals("PREMIUM") ? 30 : 5));
            mockMvc.perform(get("/api/student/career-roadmaps/saved").with(user(learnerEmail).roles("STUDENT")))
                    .andExpect(plan.equals("BASIC") ? status().isForbidden() : status().isOk());
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM subscriptions WHERE user_id=? AND provider='ADMIN_OVERRIDE' AND status='ACTIVE'", Long.class, learnerId)).isEqualTo(1);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM payments", Long.class)).isEqualTo(payments);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM audit_logs WHERE action='ADMIN_SUBSCRIPTION_OVERRIDE' AND entity_id=?", Long.class, learnerId)).isEqualTo(plans.length);
        // Paid access and its billing agreement survive an administrator downgrade unchanged.
        UUID paid = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO subscriptions(id,user_id,plan_code,status,provider,provider_subscription_id,start_date,end_date,created_at,updated_at) VALUES (?,?,'PLAN_PRO','ACTIVE','payfast','synthetic-existing-billing-agreement',current_date,current_date+30,now(),now())", paid, learnerId);
        mockMvc.perform(get("/api/admin/subscriptions/" + learnerId).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentPlan").value("BASIC"));
        // A failed audit write must roll back the replacement override and cached user plan together.
        jdbcTemplate.execute("ALTER TABLE admin_subscription_changes ADD CONSTRAINT admin_fixture_reject CHECK (reason <> 'Synthetic rollback fixture') NOT VALID");
        try {
            mockMvc.perform(post("/api/admin/subscriptions/" + learnerId + "/override").header("Authorization", bearer)
                    .contentType("application/json").content("{\"plan\":\"PRO\",\"reason\":\"Synthetic rollback fixture\"}"))
                    .andExpect(status().isInternalServerError());
            mockMvc.perform(get("/api/admin/subscriptions/" + learnerId).header("Authorization", bearer))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.currentPlan").value("BASIC"));
            assertThat(jdbcTemplate.queryForObject("SELECT plan_type FROM users WHERE id=?", String.class, learnerId)).isEqualTo("BASIC");
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM admin_subscription_changes WHERE user_id=?", Long.class, learnerId)).isEqualTo(plans.length);
        } finally {
            jdbcTemplate.execute("ALTER TABLE admin_subscription_changes DROP CONSTRAINT admin_fixture_reject");
        }
        assertThat(jdbcTemplate.queryForObject("SELECT status || ':' || provider_subscription_id FROM subscriptions WHERE id=?", String.class, paid))
                .isEqualTo("ACTIVE:synthetic-existing-billing-agreement");
        mockMvc.perform(get("/api/admin/subscriptions/" + learnerId + "/history").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(plans.length))
                .andExpect(jsonPath("$.content[0].administratorId").value(adminId.toString()))
                .andExpect(jsonPath("$.content[0].previousPlan").value("PREMIUM"))
                .andExpect(jsonPath("$.content[0].newPlan").value("BASIC"));
        String learnerLogin = mapper.writeValueAsString(java.util.Map.of("email", learnerEmail, "password", password));
        String learnerSession = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(learnerLogin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.planType").value("BASIC"))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/subscriptions/me").header("Authorization", "Bearer " + mapper.readTree(learnerSession).path("accessToken").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.provider").value("ADMIN_OVERRIDE"));
        // SUPER_ADMIN uses the existing admin portal and the same service permissions.
        UUID superRole = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO roles(id,name,created_at,updated_at) VALUES (?,'ROLE_SUPER_ADMIN',now(),now()) ON CONFLICT (name) DO NOTHING", superRole);
        superRole = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name='ROLE_SUPER_ADMIN'", UUID.class);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id=? AND role_id=?", adminId, adminRole);
        assignRole(adminId, superRole);
        mockMvc.perform(get("/api/admin/subscriptions/" + learnerId).with(user(adminEmail).roles("SUPER_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentPlan").value("BASIC"));
        String superSession = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(loginRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/admin/subscriptions").header("Authorization", "Bearer " + mapper.readTree(superSession).path("accessToken").asText()))
                .andExpect(status().isOk());
    }

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
    void persistedSubscriptionsControlRealStudentApisAndQuota() throws Exception {
        UUID id = UUID.randomUUID();
        String email = "subscription-integration-" + id + "@example.com";
        insertUser(id, email, "Subscription", "Test", OffsetDateTime.now());
        insertStudent(UUID.randomUUID(), id, "Subscription", "Test", "Grade 12");
        UUID studentRole = jdbcTemplate.queryForObject("select id from roles where name in ('STUDENT','ROLE_STUDENT') limit 1", UUID.class);
        assignRole(id, studentRole);
        var authenticated = user(email).roles("STUDENT");
        // A forged cached user plan cannot grant paid access.
        jdbcTemplate.update("update users set plan_type='PRO' where id=?", id);
        mockMvc.perform(get("/api/subscriptions/entitlements").with(authenticated))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plan").value("BASIC"))
                .andExpect(jsonPath("$.aiUsage.allowance").value(5));
        mockMvc.perform(get("/api/student/career-roadmaps/saved").with(authenticated).param("plan", "PRO"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/student/aps/calculate").with(authenticated)
                .contentType("application/json").content("{\"subjects\":[]}"))
                .andExpect(status().isOk());
        UUID subscription = UUID.randomUUID();
        jdbcTemplate.update("insert into subscriptions(id,user_id,plan_code,status,start_date,end_date,created_at,updated_at) values (?,?,'PLAN_PREMIUM','ACTIVE',current_date,current_date+30,now(),now())", subscription, id);
        mockMvc.perform(get("/api/v1/subscriptions/entitlements").with(authenticated))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plan").value("PREMIUM"))
                .andExpect(jsonPath("$.aiUsage.allowance").value(30));
        mockMvc.perform(get("/api/student/career-roadmaps/saved").with(authenticated)).andExpect(status().isOk());
        mockMvc.perform(get("/api/student/university-applications").with(authenticated)).andExpect(status().isForbidden());
        jdbcTemplate.update("update subscriptions set plan_code='PLAN_PRO' where id=?", subscription);
        mockMvc.perform(get("/api/student/university-applications").with(authenticated)).andExpect(status().isOk());
        mockMvc.perform(get("/api/subscriptions/entitlements").with(authenticated))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aiUsage.allowance").value(100));
        jdbcTemplate.update("update subscriptions set end_date=current_date where id=?", subscription);
        mockMvc.perform(get("/api/student/university-applications").with(authenticated)).andExpect(status().isForbidden());
        for (int i=0; i<5; i++) jdbcTemplate.update("insert into student_ai_usage(id,user_id,period_start,status) values (?,?,date_trunc('month',current_timestamp at time zone 'UTC')::date,'SUCCEEDED')", UUID.randomUUID(), id);
        mockMvc.perform(post("/api/student/tutor/ask").with(authenticated)
                .contentType("application/json").content("{\"subject\":\"MATHEMATICS\",\"question\":\"Explain fractions\"}"))
                .andExpect(status().isTooManyRequests());
        assertThat(jdbcTemplate.queryForObject("select count(*) from student_ai_usage where user_id=?", Integer.class, id)).isEqualTo(5);
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
