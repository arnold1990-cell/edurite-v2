package com.edurite.institution;

import com.edurite.institution.controller.InstitutionController;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.repository.InstitutionRepository;
import com.edurite.security.config.RestAccessDeniedHandler;
import com.edurite.security.config.RestAuthenticationEntryPoint;
import com.edurite.security.config.SecurityConfig;
import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.security.service.CustomUserDetailsService;
import com.edurite.security.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {InstitutionController.class},
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class InstitutionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstitutionRepository institutionRepository;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void listFiltersByTypeProvinceAndSearch() throws Exception {
        when(institutionRepository.findByActiveTrueOrderByFeaturedDescNameAsc())
                .thenReturn(List.of(
                        institution("False Bay TVET College", "Western Cape", "TVET"),
                        institution("Motheo TVET College", "Free State", "TVET"),
                        institution("Stellenbosch University", "Western Cape", "Traditional")
                ));

        mockMvc.perform(get("/api/v1/institutions")
                        .param("type", "TVET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/institutions")
                        .param("type", "TVET")
                        .param("province", "Western Cape"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("False Bay TVET College"));

        mockMvc.perform(get("/api/v1/institutions")
                        .param("q", "stellenbosch")
                        .param("type", "COLLEGE")
                        .param("province", "Western Cape"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Stellenbosch University"));
    }

    private static Institution institution(String name, String province, String category) {
        Institution institution = new Institution();
        institution.setId(UUID.randomUUID());
        institution.setName(name);
        institution.setProvince(province);
        institution.setLocation(province + ", South Africa");
        institution.setCategory(category);
        institution.setActive(true);
        institution.setFeatured(false);
        return institution;
    }
}

