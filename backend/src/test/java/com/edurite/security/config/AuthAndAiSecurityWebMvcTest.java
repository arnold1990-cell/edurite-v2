package com.edurite.security.config;

import com.edurite.ai.controller.AiController;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.StudentAiGuidanceService;
import com.edurite.ai.service.UniversitySourcesGuidanceService;
import com.edurite.ai.university.UniversitySourceCoverageService;
import com.edurite.account.controller.AccountController;
import com.edurite.account.service.AccountService;
import com.edurite.auth.controller.AuthController;
import com.edurite.auth.controller.GoogleOAuthCompatibilityController;
import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.GoogleLoginRequest;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.service.AuthService;
import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.security.service.CustomUserDetailsService;
import com.edurite.security.service.JwtService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.edurite.auth.service.SchoolPasswordRecoveryService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {AuthController.class, AccountController.class, AiController.class, GoogleOAuthCompatibilityController.class},
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "app.frontend-url=https://edurite.net"
        }
)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
@SuppressWarnings("unused")
class AuthAndAiSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private SchoolPasswordRecoveryService schoolPasswordRecoveryService;
    @MockitoBean
    private AccountService accountService;
    @MockitoBean
    private GeminiService geminiService;
    @MockitoBean
    private UniversitySourcesGuidanceService universitySourcesGuidanceService;
    @MockitoBean
    private UniversitySourceCoverageService universitySourceCoverageService;
    @MockitoBean
    private StudentAiGuidanceService studentAiGuidanceService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void loginEndpointIsPublicIgnoresRoleTabPayloadAndReturnsJwtPayload() throws Exception {
        AuthResponse response = new AuthResponse(
                "access-token",
                "refresh-token",
                "Bearer",
                3600L,
                "ADMIN",
                "ROLE_ADMIN",
                null,
                false,
                new AuthResponse.UserSummary(
                        UUID.randomUUID(),
                        "admin@edurite.com",
                        "System Admin",
                        null,
                        null,
                        Set.of("ROLE_ADMIN"),
                        "ADMIN",
                        "ROLE_ADMIN",
                        null,
                        true,
                        "BASIC",
                        false,
                        null,
                        null
                )
        );
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@edurite.com",
                                  "password": "Admin@123",
                                  "role": "COMPANY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.email").value("admin@edurite.com"));

        ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(requestCaptor.capture());
        assertThat(requestCaptor.getValue().email()).isEqualTo("admin@edurite.com");
        assertThat(requestCaptor.getValue().password()).isEqualTo("Admin@123");
    }

    @Test
    void invalidLoginReturnsControlledUnauthorizedResponse() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.test",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void malformedLoginReturnsControlledBadRequestResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @WithMockUser(username = "student@example.com", authorities = "ROLE_STUDENT")
    void accountMeCompatibilityRouteReturnsCurrentUser() throws Exception {
        AuthResponse response = new AuthResponse(
                "access-token",
                "refresh-token",
                "Bearer",
                3600L,
                "STUDENT",
                "ROLE_STUDENT",
                null,
                false,
                new AuthResponse.UserSummary(
                        UUID.randomUUID(),
                        "student@example.com",
                        "Student User",
                        null,
                        null,
                        Set.of("ROLE_STUDENT"),
                        "STUDENT",
                        "ROLE_STUDENT",
                        null,
                        true,
                        "BASIC",
                        false,
                        true,
                        100
                )
        );
        when(authService.me(any())).thenReturn(response);

        mockMvc.perform(get("/api/account/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.email").value("student@example.com"));
    }

    @Test
    void googleLoginEndpointForwardsSelectedCompanyRole() throws Exception {
        AuthResponse response = new AuthResponse(
                "access-token",
                "refresh-token",
                "Bearer",
                3600L,
                "COMPANY",
                "ROLE_COMPANY",
                "PENDING",
                false,
                new AuthResponse.UserSummary(
                        UUID.randomUUID(),
                        "company@example.com",
                        "Example Company",
                        "Example Company",
                        null,
                        Set.of("ROLE_COMPANY"),
                        "COMPANY",
                        "ROLE_COMPANY",
                        "PENDING",
                        true,
                        "BASIC",
                        false,
                        null,
                        null
                )
        );
        when(authService.loginWithGoogle(any(GoogleLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "company-google-token",
                                  "role": "COMPANY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryRole").value("ROLE_COMPANY"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"));

        ArgumentCaptor<GoogleLoginRequest> requestCaptor = ArgumentCaptor.forClass(GoogleLoginRequest.class);
        verify(authService).loginWithGoogle(requestCaptor.capture());
        assertThat(requestCaptor.getValue().idToken()).isEqualTo("company-google-token");
        assertThat(requestCaptor.getValue().resolvedRole()).isEqualTo("COMPANY");
    }

    @Test
    void aiTestEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/ai/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ai"))
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(get("/api/v1/ai/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ai"))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void nonTestAiEndpointsRemainProtected() throws Exception {
        mockMvc.perform(get("/api/v1/ai/dashboard-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oauthCompatibilityRouteRedirectsToFrontendLogin() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound());
    }

    @Test
    void legacyProtectedStudentApiPathRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/student/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}

