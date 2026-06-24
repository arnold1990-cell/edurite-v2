package com.edurite.security.config;

import com.edurite.bursary.controller.BursaryController;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.bursary.service.BursaryAggregationService;
import com.edurite.bursary.service.BursaryRecommendationService;
import com.edurite.career.controller.CareerController;
import com.edurite.career.repository.CareerRepository;
import com.edurite.course.controller.CourseController;
import com.edurite.course.repository.CourseRepository;
import com.edurite.institution.repository.InstitutionRepository;
import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.security.service.CustomUserDetailsService;
import com.edurite.security.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {CareerController.class, CourseController.class, BursaryController.class},
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class PublicCatalogSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CareerRepository careerRepository;
    @MockitoBean
    private CourseRepository courseRepository;
    @MockitoBean
    private InstitutionRepository institutionRepository;
    @MockitoBean
    private BursaryRepository bursaryRepository;
    @MockitoBean
    private BursaryAggregationService bursaryAggregationService;
    @MockitoBean
    private BursaryRecommendationService bursaryRecommendationService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void publicCatalogEndpointsAreAccessibleWithoutAuth() throws Exception {
        when(careerRepository.search(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Page.empty());
        when(courseRepository.search(anyString(), anyString(), anyString(), any()))
                .thenReturn(Page.empty());
        when(bursaryRepository.findByStatusIgnoreCaseAndDeletedAtIsNullAndTitleContainingIgnoreCaseAndQualificationLevelContainingIgnoreCaseAndLocationContainingIgnoreCaseAndEligibilityContainingIgnoreCase(
                anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/careers"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/bursaries"))
                .andExpect(status().isOk());
    }

    @Test
    void studentSpecificBursaryRecommendationsRemainProtected() throws Exception {
        mockMvc.perform(get("/api/v1/bursaries/recommendations/me"))
                .andExpect(status().isUnauthorized());
    }
}

