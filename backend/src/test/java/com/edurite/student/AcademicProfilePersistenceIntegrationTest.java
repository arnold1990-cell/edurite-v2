package com.edurite.student;

import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.*;
import com.edurite.student.controller.StudentController;
import com.edurite.security.service.CurrentUserService;
import com.edurite.user.entity.User;
import com.edurite.upload.service.StorageService;
import com.edurite.common.exception.ApiExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Path;
import java.security.Principal;
import java.sql.DriverManager;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Hibernate/JSONB storage in a disposable schema; HTTP controller/service boundary with test principals. */
@EnabledIfEnvironmentVariable(named = "EDURITE_ACADEMIC_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
class AcademicProfilePersistenceIntegrationTest {
    @TempDir Path storageDirectory;
    SessionFactory sessions;
    MockMvc mvc;
    String schema;
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final Principal alice = () -> "alice@example.test";
    final Principal bob = () -> "bob@example.test";
    String url() { return System.getenv("EDURITE_ACADEMIC_TEST_DB_URL"); }
    String username() { return System.getenv().getOrDefault("EDURITE_ACADEMIC_TEST_DB_USER", "postgres"); }
    String password() { return System.getenv().getOrDefault("EDURITE_ACADEMIC_TEST_DB_PASSWORD", ""); }

    @BeforeEach void setup() throws Exception {
        schema = "academic_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(url(), username(), password()); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        sessions = new Configuration().addAnnotatedClass(StudentProfile.class)
                .setProperty("hibernate.connection.url", url()).setProperty("hibernate.connection.username", username())
                .setProperty("hibernate.connection.password", password()).setProperty("hibernate.default_schema", schema)
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .buildSessionFactory();
        var repository = mock(StudentProfileRepository.class);
        when(repository.findByUserId(any())).thenAnswer(i -> {
            try (var session = sessions.openSession()) {
                return session.createSelectionQuery("from StudentProfile where userId = :user", StudentProfile.class)
                        .setParameter("user", i.getArgument(0)).uniqueResultOptional();
            }
        });
        when(repository.save(any())).thenAnswer(i -> {
            try (var session = sessions.openSession()) {
                var tx = session.beginTransaction();
                var saved = session.merge((StudentProfile) i.getArgument(0));
                tx.commit(); return saved;
            }
        });
        var current = mock(CurrentUserService.class);
        var users = new HashMap<String, User>();
        for (var principal : List.of(alice, bob)) { var user = new User(); user.setId(UUID.randomUUID()); user.setEmail(principal.getName()); users.put(principal.getName(), user); }
        when(current.requireUser(any())).thenAnswer(i -> users.get(((Principal) i.getArgument(0)).getName()));
        var storage = new StorageService(); ReflectionTestUtils.setField(storage, "directory", storageDirectory.toString());
        var service = new StudentService(repository, new StudentProfileCompletionService(), current, storage,
                mock(com.edurite.student.repository.SavedCareerRepository.class), mock(com.edurite.student.repository.SavedBursaryRepository.class),
                mock(com.edurite.application.repository.ApplicationRepository.class), mock(com.edurite.notification.repository.UserNotificationRepository.class),
                mock(com.edurite.bursary.repository.BursaryRepository.class), mock(com.edurite.gamification.service.GamificationService.class),
                mock(com.edurite.psychometric.service.PsychometricService.class), mock(com.edurite.student.repository.StudentSavedProfileRepository.class),
                mapper, mock(com.edurite.subscription.service.StudentPlanAccessService.class));
        var catalogue = mock(StudentSubjectCatalogue.class);
        when(catalogue.subjects()).thenReturn(List.of(new StudentSubjectCatalogue.Subject("Mathematics", "FET", false, null), new StudentSubjectCatalogue.Subject("Physical Sciences", "FET", false, null)));
        when(catalogue.canonicalize(anyString())).thenAnswer(i -> i.getArgument(0));
        ReflectionTestUtils.setField(service, "transcriptExtraction", new TranscriptExtractionService(catalogue));
        mvc = MockMvcBuilders.standaloneSetup(new StudentController(service, mock(StudentPreferenceService.class)))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }
    @AfterEach void cleanup() throws Exception {
        if (sessions != null) sessions.close();
        if (schema != null) try (var connection = DriverManager.getConnection(url(), username(), password()); var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
    String profile(Principal principal) throws Exception {
        return mvc.perform(get("/api/v1/student/profile").principal(principal)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    @Test void manualUploadReviewCombinedSaveReloadAndLearnerIsolation() throws Exception {
        mvc.perform(put("/api/v1/student/profile").principal(alice).contentType("application/json")
                .content("{\"selectedGrade\":\"Grade 12\",\"expectedSubjectAchievements\":[],\"subjectAchievements\":[{\"subjectName\":\"Maths\",\"markPercentage\":70}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subjectAchievements[0].markPercentage").value(70));
        var baseline = mapper.readTree(profile(alice)).get("subjectAchievements");
        var output = new java.io.ByteArrayOutputStream();
        try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage(); pdf.addPage(page);
            try (var content = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                content.beginText(); content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700); content.showText("Physical Sciences 75%"); content.endText();
            }
            pdf.save(output);
        }
        mvc.perform(multipart("/api/v1/student/profile/transcript").file(new MockMultipartFile("file", "report.pdf", "application/pdf", output.toByteArray())).principal(alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subjectAchievements.length()").value(1));
        var history = mvc.perform(get("/api/v1/student/transcripts").principal(alice)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var extracted = mapper.readTree(history).get(0).get("extraction").get("subjects").get(0).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) extracted).put("markPercentage", 76).put("academicYear", "2026").put("examination", "Final");
        var combined = mapper.createArrayNode().add(baseline.get(0)).add(extracted);
        var payload = mapper.createObjectNode(); payload.set("subjectAchievements", combined); payload.set("expectedSubjectAchievements", baseline);
        mvc.perform(put("/api/v1/student/profile").principal(alice).contentType("application/json").content(payload.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subjectAchievements.length()").value(2));
        // A fresh principal and persistence session model a later authenticated request; JWT login itself is outside this fixture.
        var reloaded = mapper.readTree(profile(() -> "alice@example.test"));
        assertThat(reloaded.get("subjectAchievements").get(1).get("markPercentage").asInt()).isEqualTo(76);
        assertThat(reloaded.get("subjectAchievements").get(1).get("academicYear").asText()).isEqualTo("2026");
        assertThat(mapper.readTree(profile(bob)).get("subjectAchievements").size()).isZero();
        mvc.perform(put("/api/v1/student/profile").principal(alice).contentType("application/json").content(payload.toString()))
                .andExpect(status().isConflict());
        try (var connection = DriverManager.getConnection(url(), username(), password()); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT jsonb_typeof(subject_achievements_json), jsonb_array_length(subject_achievements_json) FROM " + schema + ".students WHERE transcript_file_url IS NOT NULL")) {
            assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("array"); assertThat(rows.getInt(2)).isEqualTo(2);
        }
    }
}
