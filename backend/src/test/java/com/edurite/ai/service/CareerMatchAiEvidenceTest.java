package com.edurite.ai.service;

import com.edurite.ai.config.GeminiProperties;
import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.admin.entity.PlatformSetting;
import com.edurite.admin.service.PlatformSettingsService;
import com.edurite.bursary.service.BursaryRecommendationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentPreferenceRepository;
import com.edurite.student.service.ProfileGuidanceContext;
import com.edurite.student.service.StudentService;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CareerMatchAiEvidenceTest {
    @ParameterizedTest @CsvSource({"true,false", "false,true", "true,true", "false,false"})
    void existingAiIntegrationReceivesOnlySavedEvidenceAndDropsInventedPercentages(boolean academic, boolean assessed) {
        var provider = mock(AiProviderOrchestratorService.class);
        when(provider.generateContent(anyString())).thenReturn("""
                {"recommendedCareers":[{"name":"Data analyst","matchScore":97,"reason":"Explore recorded evidence","improvements":["Review missing inputs"]}]}
                """);
        var mapper = new ObjectMapper();
        var gemini = new GeminiService(mapper, new GeminiProperties(), provider);
        var students = mock(StudentService.class);
        var access = mock(StudentPlanAccessService.class);
        var settings = mock(PlatformSettingsService.class);
        var enabled = new PlatformSetting(); enabled.setAiGuidanceEnabled(true);
        when(settings.getCurrentSettingsEntity()).thenReturn(enabled);
        var profile = new StudentProfile(); profile.setId(UUID.randomUUID()); profile.setUserId(UUID.randomUUID());
        if (academic) profile.setSubjectAchievementsJson("[{\"subjectName\":\"Mathematics\",\"markPercentage\":72}]");
        var psychometric = mock(PsychometricService.class);
        when(psychometric.hasSubmissionForStudentProfileId(profile.getId())).thenReturn(assessed);
        when(psychometric.findStrengthAreasByStudentProfileId(profile.getId())).thenReturn(assessed ? List.of("analytical") : List.of());
        Principal learner = () -> "learner@example.test";
        when(students.getProfileEntity(learner)).thenReturn(profile);
        when(access.resolveByUserId(profile.getUserId())).thenReturn(new StudentPlanAccessService.StudentPlanAccess("PLAN_PREMIUM", "ACTIVE", true, 10, null));
        var service = new StudentAiGuidanceService(students, gemini, mock(BursaryRecommendationService.class), settings, access);
        ReflectionTestUtils.setField(service, "guidanceContext", new ProfileGuidanceContext(mapper, psychometric, mock(StudentPreferenceRepository.class)));
        var response = service.careerAdviceForStudent(learner);
        assertThat(response.recommendedCareers().getFirst().matchScore()).isNull();
        var prompt = ArgumentCaptor.forClass(String.class); verify(provider).generateContent(prompt.capture());
        assertThat(prompt.getValue().contains("Mathematics")).isEqualTo(academic);
        assertThat(prompt.getValue().contains("analytical")).isEqualTo(assessed);
        assertThat(prompt.getValue()).contains("Assessment is optional", "matchScore must be null", "State missing information");
        verify(students).getProfileEntity(learner);
    }

    @Test void malformedAiResponseDoesNotBecomeFabricatedCareerResults() {
        var provider = mock(AiProviderOrchestratorService.class);
        when(provider.generateContent(anyString())).thenReturn("Some unstructured response without career evidence");
        var gemini = new GeminiService(new ObjectMapper(), new GeminiProperties(), provider);
        assertThatThrownBy(() -> gemini.getCareerAdvice(new CareerAdviceRequest("", "", "", ""))).isInstanceOf(AiServiceException.class);
    }
}
