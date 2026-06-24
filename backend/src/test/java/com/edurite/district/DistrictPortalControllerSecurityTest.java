package com.edurite.district;

import com.edurite.district.controller.DistrictPortalController;
import com.edurite.district.dto.DistrictDtos;
import com.edurite.district.service.DistrictAccessService;
import com.edurite.district.service.DistrictPortalService;
import com.edurite.district.service.DistrictSchoolRegistrationService;
import com.edurite.security.config.RestAccessDeniedHandler;
import com.edurite.security.config.RestAuthenticationEntryPoint;
import com.edurite.security.config.SecurityConfig;
import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.security.service.CustomUserDetailsService;
import com.edurite.security.service.JwtService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DistrictPortalController.class, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
class DistrictPortalControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DistrictAccessService districtAccessService;
    @MockitoBean
    private DistrictPortalService districtPortalService;
    @MockitoBean
    private DistrictSchoolRegistrationService districtSchoolRegistrationService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void unauthenticatedDistrictDashboardRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/district/dashboard"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(districtAccessService, districtPortalService);
    }

    @Test
    @WithMockUser(username = "school@edurite.com", authorities = "ROLE_SCHOOL_ADMIN")
    void nonDistrictUserCannotOpenDistrictDashboard() throws Exception {
        mockMvc.perform(get("/api/district/dashboard"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(districtAccessService, districtPortalService);
    }

    @Test
    @WithMockUser(username = "district@edurite.com", authorities = "ROLE_DISTRICT_ADMIN")
    void districtAdminCanOpenScopedDistrictDashboard() throws Exception {
        UUID districtId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(districtAccessService.requireDistrictContext(any(), any()))
                .thenReturn(new DistrictAccessService.AccessContext(actorId, districtId, DistrictAccessService.ROLE_DISTRICT_ADMIN));
        when(districtPortalService.dashboard(eq(districtId), eq(actorId)))
                .thenReturn(new DistrictDtos.DistrictDashboardResponse(
                        "North District",
                        "ND-01",
                        "Gauteng",
                        "ACTIVE",
                        "Scoped district dashboard",
                        List.of(new DistrictDtos.MetricCardDto("Total schools", "3", "Schools assigned", "neutral")),
                        List.of(new DistrictDtos.TrendPointDto("School A", BigDecimal.valueOf(29.5), "positive")),
                        List.of(new DistrictDtos.DistributionItemDto("High risk", 1, "warning")),
                        List.of(new DistrictDtos.DistributionItemDto("Complete", 2, "positive")),
                        List.of(new DistrictDtos.InsightItemDto("School A", "APS 29.5", "positive")),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(get("/api/district/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.districtName").value("North District"))
                .andExpect(jsonPath("$.metrics[0].label").value("Total schools"));
    }

    @Test
    @WithMockUser(username = "circuit@edurite.com", authorities = "ROLE_CIRCUIT_MANAGER")
    void circuitManagerCanApproveSchoolRegistrationRequest() throws Exception {
        UUID districtId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(districtAccessService.requireDistrictContext(any(), any()))
                .thenReturn(new DistrictAccessService.AccessContext(actorId, districtId, DistrictAccessService.ROLE_CIRCUIT_MANAGER));
        when(districtSchoolRegistrationService.approve(eq(districtId), eq(requestId), eq(actorId)))
                .thenReturn(new DistrictDtos.SchoolRegistrationRequestItemDto(
                        requestId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Kgale Secondary",
                        "EMIS-001",
                        "South East",
                        "Gaborone",
                        "Central Circuit",
                        "Secondary",
                        "Principal One",
                        "principal@example.com",
                        "school@example.com",
                        "+26770000000",
                        "Plot 1",
                        "ACTIVE",
                        null,
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        null
                ));

        mockMvc.perform(post("/api/district/school-registration-requests/{requestId}/approve", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
