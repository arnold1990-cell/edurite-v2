package com.edurite.ai.service;

import com.edurite.admin.service.PlatformSettingsService;
import com.edurite.ai.dto.AiDashboardSummaryResponse;
import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.service.BursaryRecommendationService;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.service.StudentPlanAccessService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StudentAiGuidanceService {

    private final StudentService studentService;
    private final GeminiService geminiService;
    private final BursaryRecommendationService bursaryRecommendationService;
    private final PlatformSettingsService platformSettingsService;
    private final StudentPlanAccessService studentPlanAccessService;

    public StudentAiGuidanceService(StudentService studentService,
                                    GeminiService geminiService,
                                    BursaryRecommendationService bursaryRecommendationService,
                                    PlatformSettingsService platformSettingsService,
                                    StudentPlanAccessService studentPlanAccessService) {
        this.studentService = studentService;
        this.geminiService = geminiService;
        this.bursaryRecommendationService = bursaryRecommendationService;
        this.platformSettingsService = platformSettingsService;
        this.studentPlanAccessService = studentPlanAccessService;
    }

    public CareerAdviceResponse careerAdviceForStudent(Principal principal) {
        ensureAiGuidanceEnabled();
        StudentProfile profile = studentService.getProfileEntity(principal);
        StudentPlanAccessService.StudentPlanAccess planAccess = studentPlanAccessService.resolveByUserId(profile.getUserId());
        CareerAdviceResponse baseResponse = geminiService.getCareerAdvice(new CareerAdviceRequest(
                safe(profile.getQualificationLevel()),
                safe(profile.getInterests()),
                safe(profile.getSkills()),
                safe(profile.getLocation())
        ));
        List<CareerAdviceResponse.RecommendedCareer> careers = baseResponse.recommendedCareers() == null
                ? List.of()
                : baseResponse.recommendedCareers();
        Integer limit = planAccess.careerSuggestionLimit();
        List<CareerAdviceResponse.RecommendedCareer> visibleCareers = limit == null
                ? careers
                : careers.stream().limit(limit).toList();
        boolean limited = visibleCareers.size() < careers.size();
        return new CareerAdviceResponse(
                visibleCareers,
                planAccess.planCode(),
                planAccess.premium(),
                limit,
                limited,
                planAccess.upgradeMessage()
        );
    }

    public List<BursaryResultDto> bursaryGuidanceForStudent(Principal principal) {
        ensureAiGuidanceEnabled();
        return bursaryRecommendationService.recommendForStudent(principal);
    }

    public AiDashboardSummaryResponse dashboardSummary(Principal principal) {
        ensureAiGuidanceEnabled();
        Map<String, Object> dashboard = studentService.dashboard(principal);
        CareerAdviceResponse careers = careerAdviceForStudent(principal);
        List<BursaryResultDto> bursaries = bursaryGuidanceForStudent(principal).stream().limit(5).toList();
        List<String> insights = List.of(
                "Saved opportunities and application progress are sourced from your EduRite profile.",
                bursaries.stream().anyMatch(BursaryResultDto::officialSource)
                        ? "Bursary guidance prioritised official provider records where available."
                        : "No official provider bursary was available, so trusted public fallback sources were used.",
                "Career guidance uses your stored profile and AI when live guidance is configured."
        );
        return new AiDashboardSummaryResponse(dashboard, insights, bursaries, careers.recommendedCareers());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "not provided" : value;
    }

    private void ensureAiGuidanceEnabled() {
        if (!platformSettingsService.getCurrentSettingsEntity().isAiGuidanceEnabled()) {
            throw new ResourceConflictException("AI guidance is currently disabled by system settings");
        }
    }
}

