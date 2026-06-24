package com.edurite.ai.service;

import com.edurite.admin.entity.PlatformSetting;
import com.edurite.admin.service.PlatformSettingsService;
import com.edurite.ai.dto.AiDashboardSummaryResponse;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.service.BursaryRecommendationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.service.StudentPlanAccessService;
import java.util.UUID;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentAiGuidanceServiceTest {

    @Test
    void dashboardSummaryCombinesDashboardCareerAndBursarySignals() {
        StudentService studentService = mock(StudentService.class);
        GeminiService geminiService = mock(GeminiService.class);
        BursaryRecommendationService bursaryRecommendationService = mock(BursaryRecommendationService.class);
        PlatformSettingsService platformSettingsService = mock(PlatformSettingsService.class);
        StudentPlanAccessService studentPlanAccessService = mock(StudentPlanAccessService.class);
        PlatformSetting settings = new PlatformSetting();
        settings.setAiGuidanceEnabled(true);
        when(platformSettingsService.getCurrentSettingsEntity()).thenReturn(settings);
        StudentAiGuidanceService service = new StudentAiGuidanceService(studentService, geminiService, bursaryRecommendationService, platformSettingsService, studentPlanAccessService);

        StudentProfile profile = new StudentProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setQualificationLevel("Degree");
        profile.setInterests("technology");
        profile.setSkills("java");
        profile.setLocation("Gauteng");

        when(studentService.getProfileEntity(any())).thenReturn(profile);
        when(studentService.dashboard(any())).thenReturn(Map.of("savedOpportunities", 3, "skillGaps", List.of("communication")));
        when(geminiService.getCareerAdvice(any())).thenReturn(new CareerAdviceResponse(List.of(
                new CareerAdviceResponse.RecommendedCareer("Software Developer", 88, "Strong fit", List.of("Build projects"))
        )));
        when(studentPlanAccessService.resolveByUserId(profile.getUserId())).thenReturn(
                new StudentPlanAccessService.StudentPlanAccess("PLAN_PREMIUM", "ACTIVE", true, null, null)
        );
        when(bursaryRecommendationService.recommendForStudent(any())).thenReturn(List.of(
                new BursaryResultDto("1", "STEM Bursary", "Provider", "desc", "Degree", "Gauteng", "citizen", null, "https://example.org", "OFFICIAL_PROVIDER", 80, List.of("https://example.org"), true, false, null)
        ));

        AiDashboardSummaryResponse response = service.dashboardSummary((Principal) () -> "student@test");

        assertThat(response.dashboard()).containsEntry("savedOpportunities", 3);
        assertThat(response.recommendedCareers()).extracting(CareerAdviceResponse.RecommendedCareer::name)
                .containsExactly("Software Developer");
        assertThat(response.recommendedCareers()).hasSize(1);
        assertThat(response.bursarySuggestions()).hasSize(1);
        assertThat(response.dashboardInsights().get(1)).contains("official provider");
    }

    @Test
    void careerAdviceForBasicPlanIsCappedToThreeSuggestions() {
        StudentService studentService = mock(StudentService.class);
        GeminiService geminiService = mock(GeminiService.class);
        BursaryRecommendationService bursaryRecommendationService = mock(BursaryRecommendationService.class);
        PlatformSettingsService platformSettingsService = mock(PlatformSettingsService.class);
        StudentPlanAccessService studentPlanAccessService = mock(StudentPlanAccessService.class);

        PlatformSetting settings = new PlatformSetting();
        settings.setAiGuidanceEnabled(true);
        when(platformSettingsService.getCurrentSettingsEntity()).thenReturn(settings);

        StudentAiGuidanceService service = new StudentAiGuidanceService(
                studentService,
                geminiService,
                bursaryRecommendationService,
                platformSettingsService,
                studentPlanAccessService
        );

        StudentProfile profile = new StudentProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setQualificationLevel("Degree");
        profile.setInterests("technology");
        profile.setSkills("java");
        profile.setLocation("Gauteng");
        when(studentService.getProfileEntity(any())).thenReturn(profile);

        when(studentPlanAccessService.resolveByUserId(profile.getUserId())).thenReturn(
                new StudentPlanAccessService.StudentPlanAccess("PLAN_BASIC", "ACTIVE", false, 3, "Upgrade to Premium")
        );
        when(geminiService.getCareerAdvice(any())).thenReturn(new CareerAdviceResponse(List.of(
                new CareerAdviceResponse.RecommendedCareer("Software Developer", 88, "Strong fit", List.of("Build projects")),
                new CareerAdviceResponse.RecommendedCareer("Data Engineer", 86, "Strong fit", List.of("Build SQL projects")),
                new CareerAdviceResponse.RecommendedCareer("Cloud Analyst", 81, "Strong fit", List.of("Learn cloud tools")),
                new CareerAdviceResponse.RecommendedCareer("Product Analyst", 80, "Strong fit", List.of("Learn analytics"))
        )));

        CareerAdviceResponse response = service.careerAdviceForStudent((Principal) () -> "student@test");

        assertThat(response.recommendedCareers()).hasSize(3);
        assertThat(response.planCode()).isEqualTo("PLAN_BASIC");
        assertThat(response.careerSuggestionsLimited()).isTrue();
        assertThat(response.upgradeMessage()).isEqualTo("Upgrade to Premium");
    }
}

