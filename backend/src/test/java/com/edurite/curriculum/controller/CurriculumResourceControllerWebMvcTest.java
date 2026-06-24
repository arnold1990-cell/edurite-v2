package com.edurite.curriculum.controller;

import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.service.CurriculumResourceService;
import com.edurite.curriculum.service.CurriculumService;
import com.edurite.school.service.SchoolAccessService;
import com.edurite.security.config.RestAccessDeniedHandler;
import com.edurite.security.config.RestAuthenticationEntryPoint;
import com.edurite.security.config.SecurityConfig;
import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.security.service.CustomUserDetailsService;
import com.edurite.security.service.JwtService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CurriculumResourceController.class, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
class CurriculumResourceControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchoolAccessService schoolAccessService;
    @MockitoBean
    private CurriculumService curriculumService;
    @MockitoBean
    private CurriculumResourceService curriculumResourceService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void unauthenticatedTeacherCurriculumResourcesRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/teacher/curriculum/resources"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(schoolAccessService, curriculumResourceService);
    }

    @Test
    @WithMockUser(username = "teacher@edurite.com", authorities = "ROLE_TEACHER")
    void teacherCurriculumResourcesEndpointReturnsDistrictResourceThatMatchesGradeRange() throws Exception {
        UUID schoolId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        when(schoolAccessService.requireSchoolContext(any(), any()))
                .thenReturn(new SchoolAccessService.AccessContext(teacherUserId, schoolId, SchoolAccessService.ROLE_TEACHER));
        when(curriculumResourceService.getDistrictResourcesForTeacher(eq(schoolId), eq(teacherUserId), any(CurriculumDtos.CurriculumResourceQuery.class)))
                .thenReturn(List.of(new CurriculumDtos.CurriculumAssetDto(
                        assetId,
                        "SYLLABUS",
                        "OFFICIAL",
                        "DISTRICT",
                        "DISTRICT_WIDE",
                        "ACTIVE",
                        "PENDING",
                        null,
                        "District Approved",
                        "Grade 12 Physical Sciences Syllabus",
                        "Physical Sciences",
                        "Grade 10-12",
                        "FET",
                        2026,
                        "Gauteng",
                        "v1.0",
                        null,
                        "Term 1",
                        null,
                        "District Admin",
                        OffsetDateTime.parse("2026-06-23T10:15:30Z"),
                        null,
                        false,
                        true,
                        false,
                        true,
                        false,
                        false
                )));

        mockMvc.perform(get("/api/teacher/curriculum/resources")
                        .param("type", "SYLLABUS")
                        .param("grade", "Grade 12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assetId.toString()))
                .andExpect(jsonPath("$[0].repositoryType").value("SYLLABUS"))
                .andExpect(jsonPath("$[0].grade").value("Grade 10-12"))
                .andExpect(jsonPath("$[0].subject").value("Physical Sciences"))
                .andExpect(jsonPath("$[0].badge").value("District Approved"))
                .andExpect(jsonPath("$[0].pdfAvailable").value(true));
    }
}
