package com.edurite.security.config;

import com.edurite.progress.controller.ProgressScoreController;
import com.edurite.progress.dto.ProgressScoreDtos.ProgressScoreResponse;
import com.edurite.progress.service.ProgressScoreService;
import com.edurite.psychometric.controller.PsychometricController;
import com.edurite.psychometric.dto.PsychometricSubmissionResponse;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.recommendation.controller.RecommendationController;
import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.service.RecommendationService;
import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.security.service.CustomUserDetailsService;
import com.edurite.security.service.JwtService;
import com.edurite.student.controller.StudentController;
import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.service.StudentPreferenceService;
import com.edurite.student.service.StudentService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                StudentController.class,
                ProgressScoreController.class,
                RecommendationController.class,
                PsychometricController.class
        },
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
@SuppressWarnings("unused")
class CriticalApiRouteWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;
    @MockitoBean
    private StudentPreferenceService studentPreferenceService;
    @MockitoBean
    private ProgressScoreService progressScoreService;
    @MockitoBean
    private RecommendationService recommendationService;
    @MockitoBean
    private PsychometricService psychometricService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "student@example.com", authorities = "ROLE_STUDENT")
    void criticalUnversionedFrontendRoutesAreMapped() throws Exception {
        when(studentService.dashboard(any())).thenReturn(Map.of("profileCompleteness", 0));
        when(studentService.getProfile(any())).thenReturn(emptyProfile());
        when(progressScoreService.score(any())).thenReturn(new ProgressScoreResponse(0, "red", List.of(), List.of()));
        when(recommendationService.generateForStudent(any())).thenReturn(
                new RecommendationResultDto(List.of(), List.of(), List.of(), List.of(), "test", "PLAN_BASIC", false, 3, false, null)
        );
        when(psychometricService.listAssessments()).thenReturn(List.of());
        when(psychometricService.latestForStudent(any())).thenReturn(emptyPsychometricResponse());

        mockMvc.perform(get("/api/student/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/api/student/profile")).andExpect(status().isOk());
        mockMvc.perform(get("/api/student/progress-score")).andExpect(status().isOk());
        mockMvc.perform(get("/api/recommendations/me")).andExpect(status().isOk());
        mockMvc.perform(get("/api/student/psychometric/assessments")).andExpect(status().isOk());
        mockMvc.perform(get("/api/student/psychometric/latest")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "student@example.com", authorities = "ROLE_STUDENT")
    void criticalVersionedRoutesRemainMapped() throws Exception {
        when(studentService.dashboard(any())).thenReturn(Map.of("profileCompleteness", 0));
        when(studentService.getProfile(any())).thenReturn(emptyProfile());
        when(progressScoreService.score(any())).thenReturn(new ProgressScoreResponse(0, "red", List.of(), List.of()));
        when(recommendationService.generateForStudent(any())).thenReturn(
                new RecommendationResultDto(List.of(), List.of(), List.of(), List.of(), "test", "PLAN_BASIC", false, 3, false, null)
        );

        mockMvc.perform(get("/api/v1/student/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/student/profile")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/student/progress-score")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/recommendations/me")).andExpect(status().isOk());
    }

    @Test
    void criticalApiRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/student/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/student/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/student/progress-score")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/recommendations/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void doubleApiPrefixIsNotAValidEndpoint() throws Exception {
        mockMvc.perform(get("/api/api/student/dashboard")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/api/recommendations/me")).andExpect(status().isNotFound());
    }

    private StudentProfileDto emptyProfile() {
        return new StudentProfileDto(
                UUID.randomUUID(),
                "Test",
                "Student",
                "student@example.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                false,
                0
        );
    }

    private PsychometricSubmissionResponse emptyPsychometricResponse() {
        return new PsychometricSubmissionResponse(
                UUID.randomUUID(),
                "student",
                Map.of(),
                List.of(),
                List.of(),
                "No psychometric submission has been recorded.",
                "2026-05-01T00:00:00Z"
        );
    }
}

