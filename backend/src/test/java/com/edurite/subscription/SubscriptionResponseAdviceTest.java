package com.edurite.subscription;

import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.service.*;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.databind.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.http.server.*;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SubscriptionResponseAdviceTest {
    final ObjectMapper mapper = new ObjectMapper();
    final CurrentUserService users = mock(CurrentUserService.class);
    final EntitlementService entitlements = mock(EntitlementService.class);
    final AiUsageService usage = mock(AiUsageService.class);
    final SubscriptionResponseAdvice advice = new SubscriptionResponseAdvice(mapper, users, entitlements, usage);
    final User user = new User();

    Object project(String path, Object body, boolean authenticated, UUID reservation) {
        user.setId(UUID.randomUUID());
        when(users.requireUser(any())).thenReturn(user);
        when(entitlements.isStudent(user)).thenReturn(true);
        when(usage.current(user.getId())).thenReturn(new AiUsageService.Usage(5, 1, 0, 4, LocalDate.of(2026,1,1), LocalDate.of(2026,2,1)));
        var request = new MockHttpServletRequest("GET", path);
        if (authenticated) request.setUserPrincipal(() -> "student@example.com");
        if (reservation != null) request.setAttribute("aiReservation", reservation);
        return advice.beforeBodyWrite(body, null, MediaType.APPLICATION_JSON, null,
                new ServletServerHttpRequest(request), new ServletServerHttpResponse(new MockHttpServletResponse()));
    }
    @Test void anonymousCatalogueCannotBypassBasicPreview() {
        var result = (JsonNode) project("/api/institutions", List.of(1,2,3,4,5,6,7), false, null);
        assertThat(result.size()).isEqualTo(5);
    }
    @Test void basicUniversityProgrammePreviewIsLimitedOnVersionedAlias() {
        var result = (JsonNode) project("/api/v1/student/universities/example/programmes", List.of(1,2,3,4,5,6), true, null);
        assertThat(result.size()).isEqualTo(5);
    }
    @Test void premiumRoadmapDoesNotExposeProAnalysisOrMutateSavedData() {
        Map<String,Object> stored = Map.of("gapAnalysis", Map.of("improvementSuggestions", List.of("private analysis")),
                "studyPlan", List.of("advanced plan"), "careerName", "Engineer");
        var result = (JsonNode) project("/api/student/career-roadmaps/saved", List.of(stored), true, null);
        assertThat(result.get(0).path("studyPlan").size()).isZero();
        assertThat(result.toString()).doesNotContain("private analysis").contains("Engineer");
        assertThat(stored.get("studyPlan")).isEqualTo(List.of("advanced plan"));
    }
    @Test void unavailableAiResponseReleasesReservationDespiteHttp200() {
        UUID reservation = UUID.randomUUID();
        project("/api/ai/analyse-university-sources", Map.of("available", false, "status", "ERROR"), true, reservation);
        verify(usage).finish(reservation, false);
    }
    @Test void successfulAiResponseCommitsReservation() {
        UUID reservation = UUID.randomUUID();
        project("/api/student/tutor/ask", Map.of("messages", List.of("answer")), true, reservation);
        verify(usage).finish(reservation, true);
    }
}
