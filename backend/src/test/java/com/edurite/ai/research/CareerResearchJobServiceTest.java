package com.edurite.ai.research;

import static com.edurite.ai.research.CareerResearchDtos.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.subscription.service.AiUsageService;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class CareerResearchJobServiceTest {
    private final StudentProfileRepository profiles = mock(StudentProfileRepository.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final AiUsageService usage = mock(AiUsageService.class);
    private final CareerResearchReportBuilder builder = mock(CareerResearchReportBuilder.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final UUID owner = UUID.randomUUID(), reservation = UUID.randomUUID();
    private final Principal principal = () -> "student@example.test";
    private StudentProfile profile;
    private CareerResearchJobService service;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setId(owner);
        user.setEmail(principal.getName());
        when(users.requireUser(principal)).thenReturn(user);
        when(users.requireUserById(owner)).thenReturn(user);
        profile = new StudentProfile();
        profile.setUserId(owner);
        profile.setPreferencesJson("{\"studyMode\":\"part-time\"}");
        profile.setSubjectAchievementsJson("[]");
        profile.setInterests("Accounting");
        when(profiles.findByUserIdForUpdate(owner)).thenReturn(Optional.of(profile));
        when(profiles.findByUserId(owner)).thenReturn(Optional.of(profile));
        when(usage.reserve(owner)).thenReturn(reservation);
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any()))
                .thenAnswer(invocation -> new SimpleTransactionStatus());
        service =
                new CareerResearchJobService(profiles, users, usage, builder, mapper, transactions);
    }

    @AfterEach
    void cleanup() {
        service.close();
    }

    @Test
    void initialGetDoesNotReserveCreditsOrWritePreferences() {
        assertEquals("IDLE", service.status(principal).status());
        verifyNoInteractions(usage, builder);
        verify(profiles, never()).save(any());
    }

    @Test
    void duplicateStartUsesOneReservationAndOneBackgroundJob() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        when(builder.build(any(), any()))
                .thenAnswer(
                        invocation -> {
                            entered.countDown();
                            assertTrue(release.await(3, TimeUnit.SECONDS));
                            return report();
                        });
        String id = service.start(principal).jobId();
        assertTrue(entered.await(3, TimeUnit.SECONDS));
        assertEquals(id, service.start(principal).jobId());
        verify(usage).reserve(owner);
        verify(builder).build(any(), any());
        release.countDown();
        await("SUCCEEDED");
        verify(usage).finish(reservation, true);
        assertTrue(mapper.readTree(profile.getPreferencesJson()).has("careerResearchReport"));
        assertEquals(
                "part-time",
                mapper.readTree(profile.getPreferencesJson()).path("studyMode").asText());
    }

    @Test
    void failureReleasesReservationAndRetainsPreviousSuccessfulReport() throws Exception {
        profile.setPreferencesJson("{\"careerResearchReport\":{\"previous\":true}}");
        when(builder.build(any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid AI response"));
        service.start(principal);
        assertNull(await("FAILED").report());
        verify(usage).finish(reservation, false);
        verify(usage, never()).finish(reservation, true);
        assertTrue(
                mapper.readTree(profile.getPreferencesJson())
                        .path("careerResearchReport")
                        .path("previous")
                        .asBoolean());
    }

    @Test
    void incompleteReportIsNeverPublishedOrCharged() throws Exception {
        when(builder.build(any(), any())).thenReturn(null);
        service.start(principal);
        assertNull(await("FAILED").report());
        verify(usage).finish(reservation, false);
    }

    @Test
    void changedEvidenceHidesSavedReportWithoutGeneratingAgain() throws Exception {
        when(builder.build(any(), any())).thenReturn(report());
        service.start(principal);
        assertNotNull(await("SUCCEEDED").report());
        profile.setInterests("Engineering");
        assertEquals("STALE", service.status(principal).status());
        assertNull(service.status(principal).report());
        verify(builder).build(any(), any());
    }

    @Test
    void expiredPersistedJobReleasesCreditAndRequiresExplicitRetry() throws Exception {
        profile.setPreferencesJson(
                "{\"careerResearchJob\":{\"jobId\":\"interrupted\",\"status\":\"RUNNING\",\"reservation\":\""
                        + reservation
                        + "\",\"createdAt\":\""
                        + Instant.now().minusSeconds(700)
                        + "\"}}");
        assertEquals("FAILED", service.status(principal).status());
        service.status(principal);
        verify(usage).finish(reservation, false);
        verify(usage, never()).reserve(any());
        verifyNoInteractions(builder);
    }

    @Test
    void anotherAuthenticatedStudentCannotReadOwnersReport() {
        User other = new User();
        other.setId(UUID.randomUUID());
        Principal otherPrincipal = () -> "other@example.test";
        when(users.requireUser(otherPrincipal)).thenReturn(other);
        when(profiles.findByUserIdForUpdate(other.getId())).thenReturn(Optional.empty());
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.status(otherPrincipal));
        verifyNoInteractions(usage, builder);
    }

    @Test
    void completedRequestReplayDoesNotConsumeAnotherInteraction() throws Exception {
        when(builder.build(any(), any())).thenReturn(report());
        String requestKey = UUID.randomUUID().toString();
        String id = service.start(principal, requestKey).jobId();
        await("SUCCEEDED");
        assertEquals(id, service.start(principal, requestKey).jobId());
        verify(usage).reserve(owner);
        verify(builder).build(any(), any());
    }

    private JobStatus await(String expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        JobStatus state;
        do {
            Thread.sleep(10);
            state = service.status(principal);
        } while (!state.status().equals(expected) && System.nanoTime() < deadline);
        assertEquals(expected, state.status());
        return state;
    }

    private Report report() {
        var academic =
                new AcademicAnalysis(
                        "Grade 12",
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("Accounting"),
                        List.of(),
                        null,
                        List.of());
        var career =
                new CareerFinding(
                        UUID.randomUUID().toString(),
                        "Accountant",
                        "Finance",
                        "",
                        20,
                        "Evidence-aligned exploration",
                        "Your declared Accounting interest aligns with this catalogue career.",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of());
        return new Report(
                "career-research-v1",
                Instant.now().toString(),
                academic,
                List.of(career),
                List.of(),
                List.of(),
                List.of(),
                new Narrative(
                        "Academic interests support exploration.",
                        "No programme was verified.",
                        List.of("Review official programme requirements."),
                        List.of()),
                List.of(),
                List.of("Interests"),
                List.of(),
                "Internal ordering, not probability.");
    }
}
