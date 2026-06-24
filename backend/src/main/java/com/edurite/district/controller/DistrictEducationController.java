package com.edurite.district.controller;

import com.edurite.district.dto.DistrictEducationDtos;
import com.edurite.district.service.DistrictAccessService;
import com.edurite.district.service.DistrictEducationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/district/education", "/api/district/education"})
public class DistrictEducationController {

    private final DistrictAccessService districtAccessService;
    private final DistrictEducationService districtEducationService;

    public DistrictEducationController(
            DistrictAccessService districtAccessService,
            DistrictEducationService districtEducationService
    ) {
        this.districtAccessService = districtAccessService;
        this.districtEducationService = districtEducationService;
    }

    @GetMapping("/director/dashboard")
    public DistrictEducationDtos.RoleDashboardResponse directorDashboard(Principal principal) {
        DistrictAccessService.AccessContext context = districtAccessService.requireDistrictContext(principal, Set.of(
                DistrictAccessService.ROLE_DISTRICT_ADMIN,
                DistrictAccessService.ROLE_DISTRICT_DIRECTOR
        ));
        return districtEducationService.directorDashboard(context.districtId());
    }

    @GetMapping("/circuit/dashboard")
    public DistrictEducationDtos.RoleDashboardResponse circuitDashboard(Principal principal) {
        DistrictAccessService.AccessContext context = circuitContext(principal);
        return districtEducationService.circuitDashboard(context.districtId(), context.userId());
    }

    @GetMapping("/circuit/schools")
    public DistrictEducationDtos.CircuitSchoolsResponse circuitSchools(Principal principal) {
        DistrictAccessService.AccessContext context = circuitContext(principal);
        return districtEducationService.circuitSchools(context.districtId(), context.userId());
    }

    @GetMapping("/circuit/curriculum")
    public DistrictEducationDtos.CircuitCurriculumResponse circuitCurriculum(
            Principal principal,
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Integer week
    ) {
        DistrictAccessService.AccessContext context = circuitContext(principal);
        return districtEducationService.circuitCurriculum(context.districtId(), context.userId(), school, subject, grade, term, week);
    }

    @GetMapping("/circuit/visits")
    public List<DistrictEducationDtos.SchoolVisitDto> circuitVisits(Principal principal) {
        return districtEducationService.circuitVisits(circuitContext(principal).userId());
    }

    @PostMapping("/circuit/visits")
    public DistrictEducationDtos.SchoolVisitDto createVisit(Principal principal, @Valid @RequestBody DistrictEducationDtos.SchoolVisitUpsertRequest request) {
        DistrictAccessService.AccessContext context = circuitContext(principal);
        return districtEducationService.saveVisit(context.districtId(), context.userId(), request);
    }

    @GetMapping("/circuit/support-requests")
    public List<DistrictEducationDtos.SupportRequestDto> circuitSupportRequests(Principal principal) {
        DistrictAccessService.AccessContext context = circuitContext(principal);
        return districtEducationService.circuitSupportRequests(context.districtId(), context.userId());
    }

    @PutMapping("/circuit/support-requests/{id}")
    public DistrictEducationDtos.SupportRequestDto updateCircuitSupportRequest(
            Principal principal,
            @PathVariable UUID id,
            @RequestBody DistrictEducationDtos.SupportRequestUpdateRequest request
    ) {
        DistrictAccessService.AccessContext context = circuitContext(principal);
        return districtEducationService.updateSupportRequest(context.districtId(), context.userId(), id, request);
    }

    @GetMapping("/circuit/interventions")
    public List<DistrictEducationDtos.DistrictInterventionDto> circuitInterventions(Principal principal) {
        DistrictAccessService.AccessContext context = circuitContext(principal);
        return districtEducationService.circuitInterventions(context.districtId(), context.userId());
    }

    @PostMapping("/interventions")
    public DistrictEducationDtos.DistrictInterventionDto createIntervention(
            Principal principal,
            @Valid @RequestBody DistrictEducationDtos.DistrictInterventionUpsertRequest request
    ) {
        DistrictAccessService.AccessContext context = districtAccessService.requireDistrictContext(principal, Set.of(
                DistrictAccessService.ROLE_DISTRICT_ADMIN,
                DistrictAccessService.ROLE_DISTRICT_DIRECTOR,
                DistrictAccessService.ROLE_CIRCUIT_MANAGER,
                DistrictAccessService.ROLE_SUBJECT_ADVISOR
        ));
        return districtEducationService.createIntervention(context.districtId(), context.userId(), request);
    }

    @PostMapping("/interventions/{id}/ai-support-plan")
    public DistrictEducationDtos.AiSupportPlanResponse generateAiSupportPlan(Principal principal, @PathVariable UUID id) {
        DistrictAccessService.AccessContext context = districtAccessService.requireDistrictContext(principal, Set.of(
                DistrictAccessService.ROLE_DISTRICT_ADMIN,
                DistrictAccessService.ROLE_DISTRICT_DIRECTOR,
                DistrictAccessService.ROLE_CIRCUIT_MANAGER,
                DistrictAccessService.ROLE_SUBJECT_ADVISOR
        ));
        return districtEducationService.generateAiSupportPlan(context.districtId(), id);
    }

    @GetMapping("/advisor/dashboard")
    public DistrictEducationDtos.RoleDashboardResponse advisorDashboard(Principal principal) {
        DistrictAccessService.AccessContext context = advisorContext(principal);
        return districtEducationService.advisorDashboard(context.districtId(), context.userId());
    }

    @GetMapping("/advisor/teachers")
    public DistrictEducationDtos.AdvisorTeachersResponse advisorTeachers(Principal principal) {
        DistrictAccessService.AccessContext context = advisorContext(principal);
        return districtEducationService.advisorTeachers(context.districtId(), context.userId());
    }

    @GetMapping("/advisor/teachers/{teacherId}")
    public DistrictEducationDtos.TeacherProfileResponse advisorTeacherProfile(Principal principal, @PathVariable UUID teacherId) {
        DistrictAccessService.AccessContext context = advisorContext(principal);
        return districtEducationService.advisorTeacherProfile(context.districtId(), context.userId(), teacherId);
    }

    @GetMapping("/advisor/atp-monitoring")
    public DistrictEducationDtos.AdvisorTeachersResponse advisorAtpMonitoring(Principal principal) {
        DistrictAccessService.AccessContext context = advisorContext(principal);
        return districtEducationService.advisorTeachers(context.districtId(), context.userId());
    }

    @GetMapping("/advisor/assessments")
    public List<DistrictEducationDtos.CommonAssessmentDto> advisorAssessments(Principal principal) {
        DistrictAccessService.AccessContext context = advisorContext(principal);
        return districtEducationService.advisorAssessments(context.districtId(), context.userId());
    }

    @PostMapping("/advisor/assessments")
    public DistrictEducationDtos.CommonAssessmentDto createAssessment(
            Principal principal,
            @Valid @RequestBody DistrictEducationDtos.CommonAssessmentCreateRequest request
    ) {
        DistrictAccessService.AccessContext context = advisorContext(principal);
        return districtEducationService.createAssessment(context.districtId(), context.userId(), request);
    }

    private DistrictAccessService.AccessContext circuitContext(Principal principal) {
        return districtAccessService.requireDistrictContext(principal, Set.of(DistrictAccessService.ROLE_CIRCUIT_MANAGER));
    }

    private DistrictAccessService.AccessContext advisorContext(Principal principal) {
        return districtAccessService.requireDistrictContext(principal, Set.of(
                DistrictAccessService.ROLE_SUBJECT_ADVISOR,
                DistrictAccessService.ROLE_DISTRICT_ADMIN,
                DistrictAccessService.ROLE_DISTRICT_DIRECTOR
        ));
    }
}
