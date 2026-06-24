package com.edurite.recommendation;

import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.service.RecommendationService;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.service.StudentPlanAccessService;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    StudentService studentService;
    @Mock
    PsychometricService psychometricService;
    @Mock
    StudentPlanAccessService studentPlanAccessService;

    private RecommendationService recommendationService;
    private Principal principal;
    private StudentProfile profile;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(studentService, studentPlanAccessService, psychometricService);
        principal = () -> "student@example.com";

        profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(UUID.randomUUID());
        profile.setInterests(null);
        profile.setSkills(null);
        profile.setExperience(null);
        profile.setQualificationLevel(null);
        profile.setProfileCompleted(false);
    }

    @Test
    void generateForStudentHandlesNullProfileFieldsSafely() {
        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(studentPlanAccessService.resolveByUserId(profile.getUserId())).thenReturn(
                new StudentPlanAccessService.StudentPlanAccess("PLAN_BASIC", "ACTIVE", false, 3, "Upgrade to Premium")
        );
        when(psychometricService.findGrowthAreasByStudentProfileId(profile.getId())).thenReturn(java.util.List.of());

        RecommendationResultDto result = recommendationService.generateForStudent(principal);

        assertThat(result.suggestedCareers()).isNotEmpty();
        assertThat(result.suggestedBursaries()).isNotEmpty();
        assertThat(result.suggestedCoursesOrImprovements()).isNotEmpty();
        assertThat(result.profileImprovementTips()).isNotEmpty();
        assertThat(result.modelVersion()).isEqualTo("rule-engine-v4");
        assertThat(result.planCode()).isEqualTo("PLAN_BASIC");
        assertThat(result.careerSuggestionLimit()).isEqualTo(3);
    }

    @Test
    void generateForStudentCapsCareerSuggestionsForBasicPlan() {
        profile.setInterests("technology,business,science,design");
        profile.setSkills("java,leadership,communication");
        profile.setQualificationLevel("Undergraduate");
        profile.setCareerGoals("Product management");

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(studentPlanAccessService.resolveByUserId(profile.getUserId())).thenReturn(
                new StudentPlanAccessService.StudentPlanAccess("PLAN_BASIC", "ACTIVE", false, 3, "Upgrade to Premium")
        );
        when(psychometricService.findGrowthAreasByStudentProfileId(profile.getId())).thenReturn(java.util.List.of("analytical"));

        RecommendationResultDto result = recommendationService.generateForStudent(principal);

        assertThat(result.suggestedCareers()).hasSizeLessThanOrEqualTo(3);
        assertThat(result.careerSuggestionsLimited()).isTrue();
        assertThat(result.upgradeMessage()).isEqualTo("Upgrade to Premium");
    }
}


