package com.edurite.district.controller;

import com.edurite.district.dto.DistrictDtos;
import com.edurite.district.service.DistrictAccessService;
import com.edurite.district.service.DistrictPortalService;
import com.edurite.district.service.DistrictSchoolRegistrationService;
import com.edurite.school.portal.dto.SchoolPortalDtos;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/district", "/api/district"})
public class DistrictPortalController {

    private final DistrictAccessService districtAccessService;
    private final DistrictPortalService districtPortalService;
    private final DistrictSchoolRegistrationService districtSchoolRegistrationService;

    public DistrictPortalController(
            DistrictAccessService districtAccessService,
            DistrictPortalService districtPortalService,
            DistrictSchoolRegistrationService districtSchoolRegistrationService
    ) {
        this.districtAccessService = districtAccessService;
        this.districtPortalService = districtPortalService;
        this.districtSchoolRegistrationService = districtSchoolRegistrationService;
    }

    @GetMapping("/dashboard")
    public DistrictDtos.DistrictDashboardResponse dashboard(Principal principal) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.dashboard(context.districtId(), context.userId());
    }

    @GetMapping("/schools")
    public DistrictDtos.DistrictSchoolsResponse schools(
            Principal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String complianceStatus
    ) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.schools(context.districtId(), search, riskLevel, complianceStatus);
    }

    @GetMapping("/school-registration-requests")
    public DistrictDtos.SchoolRegistrationRequestsResponse schoolRegistrationRequests(
            Principal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        DistrictAccessService.AccessContext context = schoolRegistrationContext(principal);
        return districtSchoolRegistrationService.requests(context.districtId(), status, search);
    }

    @PostMapping("/school-registration-requests/{requestId}/approve")
    public DistrictDtos.SchoolRegistrationRequestItemDto approveSchoolRegistrationRequest(
            Principal principal,
            @PathVariable UUID requestId
    ) {
        DistrictAccessService.AccessContext context = schoolRegistrationContext(principal);
        return districtSchoolRegistrationService.approve(context.districtId(), requestId, context.userId());
    }

    @PostMapping("/school-registration-requests/{requestId}/reject")
    public DistrictDtos.SchoolRegistrationRequestItemDto rejectSchoolRegistrationRequest(
            Principal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody DistrictDtos.SchoolRegistrationDecisionRequest request
    ) {
        DistrictAccessService.AccessContext context = schoolRegistrationContext(principal);
        return districtSchoolRegistrationService.reject(context.districtId(), requestId, context.userId(), request.rejectionReason());
    }

    @PostMapping("/school-registration-requests/{requestId}/decision")
    public DistrictDtos.SchoolRegistrationRequestItemDto decideSchoolRegistrationRequest(
            Principal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody DistrictDtos.SchoolRegistrationDecisionRequest request
    ) {
        DistrictAccessService.AccessContext context = schoolRegistrationContext(principal);
        return districtSchoolRegistrationService.decide(context.districtId(), requestId, context.userId(), request);
    }

    @GetMapping("/schools/{schoolId}")
    public DistrictDtos.DistrictSchoolDetailResponse schoolDetail(Principal principal, @PathVariable UUID schoolId) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.schoolDetail(context.districtId(), schoolId, context.userId());
    }

    @GetMapping("/schools/{schoolId}/analytics")
    public DistrictDtos.DistrictSchoolAnalyticsResponse schoolAnalytics(Principal principal, @PathVariable UUID schoolId) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.schoolAnalytics(context.districtId(), schoolId);
    }

    @GetMapping("/analytics")
    public DistrictDtos.DistrictAnalyticsResponse analytics(Principal principal) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.analytics(context.districtId());
    }

    @GetMapping("/ai-insights")
    public DistrictDtos.DistrictAiInsightsResponse aiInsights(Principal principal) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.aiInsights(context.districtId());
    }

    @GetMapping("/reports")
    public List<DistrictDtos.ReportItemDto> reports(Principal principal) {
        context(principal);
        return districtPortalService.reports();
    }

    @PostMapping("/reports/export")
    public SchoolPortalDtos.ReportExportResponse exportReport(
            Principal principal,
            @Valid @RequestBody DistrictDtos.ReportExportRequest request
    ) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.exportReport(context.districtId(), context.userId(), request);
    }

    @PostMapping("/announcements")
    public DistrictDtos.AnnouncementItemDto createAnnouncement(
            Principal principal,
            @Valid @RequestBody DistrictDtos.AnnouncementCreateRequest request
    ) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.createAnnouncement(context.districtId(), context.userId(), request);
    }

    @GetMapping("/interventions")
    public DistrictDtos.DistrictInterventionsResponse interventions(Principal principal) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.interventions(context.districtId());
    }

    @PostMapping("/interventions")
    public DistrictDtos.DistrictInterventionItemDto createIntervention(
            Principal principal,
            @Valid @RequestBody DistrictDtos.DistrictInterventionCreateRequest request
    ) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.createIntervention(context.districtId(), context.userId(), request);
    }

    @GetMapping("/settings")
    public DistrictDtos.DistrictSettingsResponse settings(Principal principal) {
        DistrictAccessService.AccessContext context = context(principal);
        return districtPortalService.settings(context.districtId());
    }

    private DistrictAccessService.AccessContext context(Principal principal) {
        return districtAccessService.requireDistrictContext(principal, Set.of(
                DistrictAccessService.ROLE_DISTRICT_ADMIN,
                DistrictAccessService.ROLE_DISTRICT_DIRECTOR
        ));
    }

    private DistrictAccessService.AccessContext schoolRegistrationContext(Principal principal) {
        return districtAccessService.requireDistrictContext(principal, Set.of(
                DistrictAccessService.ROLE_DISTRICT_ADMIN,
                DistrictAccessService.ROLE_DISTRICT_DIRECTOR,
                DistrictAccessService.ROLE_CIRCUIT_MANAGER,
                "ROLE_ADMIN"
        ));
    }
}
