package com.edurite.security.config;

import com.edurite.discovery.controller.PublicDiscoveryController;
import com.edurite.discovery.dto.PublicDiscoveryInsightDto;
import com.edurite.discovery.service.PublicDiscoveryInsightService;
import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.security.service.CustomUserDetailsService;
import com.edurite.security.service.JwtService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PublicDiscoveryController.class,
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class PublicDiscoverySecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicDiscoveryInsightService publicDiscoveryInsightService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void publicDiscoveryEndpointsAreAccessibleWithoutAuth() throws Exception {
        when(publicDiscoveryInsightService.careersInsight(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new PublicDiscoveryInsightDto("Live insight", false, List.of(), 0));
        when(publicDiscoveryInsightService.coursesInsight(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new PublicDiscoveryInsightDto("Live insight", false, List.of(), 0));
        when(publicDiscoveryInsightService.bursariesInsight(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new PublicDiscoveryInsightDto("Live insight", false, List.of(), 0));

        mockMvc.perform(get("/api/v1/public/discovery/careers/insight"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/discovery/courses/insight"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/discovery/bursaries/insight"))
                .andExpect(status().isOk());
    }
}

