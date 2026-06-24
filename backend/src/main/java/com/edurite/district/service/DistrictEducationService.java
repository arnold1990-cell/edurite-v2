package com.edurite.district.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.service.CurriculumResourceService;
import com.edurite.curriculum.service.CurriculumService;
import com.edurite.district.dto.DistrictDtos;
import com.edurite.district.dto.DistrictEducationDtos;
import com.edurite.district.entity.Circuit;
import com.edurite.district.entity.DistrictIntervention;
import com.edurite.district.entity.SchoolCircuitAssignment;
import com.edurite.district.entity.SchoolVisitSchedule;
import com.edurite.district.entity.SubjectAdvisorAssignment;
import com.edurite.district.entity.SupportRequest;
import com.edurite.district.repository.CircuitRepository;
import com.edurite.district.repository.DistrictInterventionRepository;
import com.edurite.district.repository.SchoolCircuitAssignmentRepository;
import com.edurite.district.repository.SchoolVisitScheduleRepository;
import com.edurite.district.repository.SubjectAdvisorAssignmentRepository;
import com.edurite.district.repository.SupportRequestRepository;
import com.edurite.school.admin.entity.SchoolSupportRequest;
import com.edurite.school.admin.repository.SchoolSupportRequestRepository;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolSubject;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.entity.TeacherAssignment;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistrictEducationService {

    private final CircuitRepository circuitRepository;
    private final SchoolCircuitAssignmentRepository schoolCircuitAssignmentRepository;
    private final SubjectAdvisorAssignmentRepository subjectAdvisorAssignmentRepository;
    private final SchoolVisitScheduleRepository schoolVisitScheduleRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final SchoolSupportRequestRepository schoolSupportRequestRepository;
    private final DistrictInterventionRepository districtInterventionRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final SchoolSubjectRepository schoolSubjectRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final UserRepository userRepository;
    private final CurriculumService curriculumService;
    private final CurriculumResourceService curriculumResourceService;
    private final AiProviderOrchestratorService aiProviderOrchestratorService;

    public DistrictEducationService(
            CircuitRepository circuitRepository,
            SchoolCircuitAssignmentRepository schoolCircuitAssignmentRepository,
            SubjectAdvisorAssignmentRepository subjectAdvisorAssignmentRepository,
            SchoolVisitScheduleRepository schoolVisitScheduleRepository,
            SupportRequestRepository supportRequestRepository,
            SchoolSupportRequestRepository schoolSupportRequestRepository,
            DistrictInterventionRepository districtInterventionRepository,
            SchoolRepository schoolRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            SchoolSubjectRepository schoolSubjectRepository,
            TeacherAssignmentRepository teacherAssignmentRepository,
            UserRepository userRepository,
            CurriculumService curriculumService,
            CurriculumResourceService curriculumResourceService,
            AiProviderOrchestratorService aiProviderOrchestratorService
    ) {
        this.circuitRepository = circuitRepository;
        this.schoolCircuitAssignmentRepository = schoolCircuitAssignmentRepository;
        this.subjectAdvisorAssignmentRepository = subjectAdvisorAssignmentRepository;
        this.schoolVisitScheduleRepository = schoolVisitScheduleRepository;
        this.supportRequestRepository = supportRequestRepository;
        this.schoolSupportRequestRepository = schoolSupportRequestRepository;
        this.districtInterventionRepository = districtInterventionRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.schoolSubjectRepository = schoolSubjectRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.userRepository = userRepository;
        this.curriculumService = curriculumService;
        this.curriculumResourceService = curriculumResourceService;
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
    }

    @Transactional(readOnly = true)
    public DistrictEducationDtos.RoleDashboardResponse directorDashboard(UUID districtId) {
        List<School> schools = schoolRepository.findByDistrictIdOrderBySchoolNameAsc(districtId);
        List<Circuit> circuits = circuitRepository.findByDistrictIdAndActiveTrueOrderByNameAsc(districtId);
        long teachers = countRole(schools, "ROLE_TEACHER");
        long learners = countRole(schools, "ROLE_SCHOOL_STUDENT");
        long openInterventions = districtInterventionRepository.findByDistrictIdAndActiveTrueOrderByCreatedAtDesc(districtId).stream()
                .filter(item -> !"RESOLVED".equalsIgnoreCase(item.getStatus()))
                .count();
        long schoolsBehind = computeComplianceRows(schools).stream().filter(item -> item.atpCompliance() < 85).count();
        return new DistrictEducationDtos.RoleDashboardResponse(
                "District Director Dashboard",
                "District-wide school oversight, curriculum delivery, interventions, and support.",
                List.of(
                        metric("Total Schools", schools.size(), "District schools", "neutral"),
                        metric("Total Circuits", circuits.size(), "Active circuits", "neutral"),
                        metric("Total Teachers", teachers, "Teachers across district schools", "neutral"),
                        metric("Total Learners", learners, "Learners across district schools", "neutral"),
                        metric("District Pass Rate", averagePassRate(schools) + "%", "District-wide proxy pass rate", averagePassRate(schools) >= 70 ? "positive" : "warning"),
                        metric("ATP Compliance %", averageCompliance(schools) + "%", "District ATP pacing", averageCompliance(schools) >= 85 ? "positive" : "warning"),
                        metric("Schools Behind ATP", schoolsBehind, "Schools needing curriculum recovery", schoolsBehind > 0 ? "warning" : "positive"),
                        metric("Open Interventions", openInterventions, "District intervention workload", openInterventions > 0 ? "warning" : "positive"),
                        metric("Teacher Attendance %", teacherAttendance(schools) + "%", "Activity-based attendance proxy", teacherAttendance(schools) >= 85 ? "positive" : "warning"),
                        metric("Curriculum Risk Alerts", schoolsBehind, "Schools with ATP risk signals", schoolsBehind > 0 ? "warning" : "positive")
                )
        );
    }

    @Transactional(readOnly = true)
    public DistrictEducationDtos.RoleDashboardResponse circuitDashboard(UUID districtId, UUID managerUserId) {
        List<School> schools = assignedCircuitSchools(districtId, managerUserId);
        List<SupportRequest> requests = supportRequestRepository.findBySchoolIdInOrderByCreatedAtDesc(schoolIds(schools));
        return new DistrictEducationDtos.RoleDashboardResponse(
                "Circuit Manager Dashboard",
                "Circuit-scoped school monitoring, support requests, visits, and interventions.",
                List.of(
                        metric("Schools in My Circuit", schools.size(), "Assigned schools", "neutral"),
                        metric("Learners in My Circuit", countRole(schools, "ROLE_SCHOOL_STUDENT"), "Assigned learners", "neutral"),
                        metric("Teachers in My Circuit", countRole(schools, "ROLE_TEACHER"), "Assigned teachers", "neutral"),
                        metric("Average Pass Rate", averagePassRate(schools) + "%", "Circuit pass-rate proxy", averagePassRate(schools) >= 70 ? "positive" : "warning"),
                        metric("ATP Compliance %", averageCompliance(schools) + "%", "Circuit ATP pacing", averageCompliance(schools) >= 85 ? "positive" : "warning"),
                        metric("Teacher Attendance %", teacherAttendance(schools) + "%", "Activity-based attendance proxy", teacherAttendance(schools) >= 85 ? "positive" : "warning"),
                        metric("Schools At Risk", computeComplianceRows(schools).stream().filter(item -> "Red".equalsIgnoreCase(item.riskStatus())).count(), "Red-risk schools", "warning"),
                        metric("Open Support Requests", requests.stream().filter(item -> !"CLOSED".equalsIgnoreCase(item.getStatus())).count(), "Requests in circuit queue", requests.isEmpty() ? "positive" : "warning")
                )
        );
    }

    @Transactional(readOnly = true)
    public DistrictEducationDtos.CircuitSchoolsResponse circuitSchools(UUID districtId, UUID managerUserId) {
        List<School> schools = assignedCircuitSchools(districtId, managerUserId);
        return new DistrictEducationDtos.CircuitSchoolsResponse(
                List.of(
                        metric("Schools", schools.size(), "Assigned schools", "neutral"),
                        metric("Teachers", countRole(schools, "ROLE_TEACHER"), "Visible teachers", "neutral"),
                        metric("Learners", countRole(schools, "ROLE_SCHOOL_STUDENT"), "Visible learners", "neutral"),
                        metric("At Risk", computeComplianceRows(schools).stream().filter(item -> "Red".equalsIgnoreCase(item.riskStatus())).count(), "Schools at risk", "warning")
                ),
                computeComplianceRows(schools)
        );
    }

    @Transactional(readOnly = true)
    public DistrictEducationDtos.CircuitCurriculumResponse circuitCurriculum(
            UUID districtId,
            UUID managerUserId,
            String schoolId,
            String subject,
            String grade,
            String term,
            Integer week
    ) {
        Map<UUID, School> visibleSchools = assignedCircuitSchools(districtId, managerUserId).stream()
                .collect(Collectors.toMap(School::getId, Function.identity()));
        List<DistrictEducationDtos.CircuitCurriculumRowDto> items = new ArrayList<>();
        for (School school : visibleSchools.values()) {
            for (TeacherAssignment assignment : teacherAssignmentRepository.findBySchoolIdAndActiveTrue(school.getId())) {
                CurriculumDtos.TeacherCurriculumWidgetResponse widget = curriculumService.teacherWidgets(school.getId(), assignment.getTeacherUserId());
                CurriculumDtos.TeacherCoverageItemDto current = widget.thisWeeksCoverage();
                CurriculumDtos.TeacherCoverageItemDto expected = current == null ? widget.currentTopic() : current;
                if (expected == null) {
                    continue;
                }
                int actualWeek = expected.status() != null && "COMPLETED".equalsIgnoreCase(expected.status()) ? expected.weekNumber() : Math.max(0, expected.weekNumber() - Math.max(1, widget.topicsBehindSchedule().size()));
                int weeksBehind = Math.max(0, expected.weekNumber() - actualWeek);
                String riskTone = weeksBehind >= 3 ? "Red" : weeksBehind >= 1 ? "Amber" : "Green";
                String statusValue = weeksBehind >= 3 ? "Curriculum Risk" : weeksBehind >= 1 ? "Behind" : "On Track";
                SchoolSubject schoolSubject = schoolSubjectRepository.findById(assignment.getSubjectId()).orElse(null);
                DistrictEducationDtos.CircuitCurriculumRowDto row = new DistrictEducationDtos.CircuitCurriculumRowDto(
                        school.getId(),
                        school.getSchoolName(),
                        schoolSubject == null ? expected.subject() : schoolSubject.getSubjectName(),
                        firstNonBlank(assignment.getGrade(), expected.grade()),
                        expected.term(),
                        expected.weekNumber(),
                        actualWeek,
                        expected.topic(),
                        current == null ? null : current.topic(),
                        weeksBehind,
                        statusValue,
                        riskTone
                );
                if (matchesCircuitFilters(row, schoolId, subject, grade, term, week)) {
                    items.add(row);
                }
            }
        }
        items.sort(Comparator.comparing(DistrictEducationDtos.CircuitCurriculumRowDto::schoolName)
                .thenComparing(DistrictEducationDtos.CircuitCurriculumRowDto::subject));
        return new DistrictEducationDtos.CircuitCurriculumResponse(items);
    }

    @Transactional(readOnly = true)
    public List<DistrictEducationDtos.SchoolVisitDto> circuitVisits(UUID managerUserId) {
        Map<UUID, School> schoolMap = schoolRepository.findAll().stream().collect(Collectors.toMap(School::getId, Function.identity()));
        return schoolVisitScheduleRepository.findByCircuitManagerIdOrderByVisitDateDesc(managerUserId).stream()
                .map(item -> toVisitDto(item, schoolMap))
                .toList();
    }

    @Transactional
    public DistrictEducationDtos.SchoolVisitDto saveVisit(UUID districtId, UUID managerUserId, DistrictEducationDtos.SchoolVisitUpsertRequest request) {
        ensureSchoolInManagerScope(districtId, managerUserId, request.schoolId());
        SchoolVisitSchedule visit = new SchoolVisitSchedule();
        visit.setCircuitManagerId(managerUserId);
        visit.setSchoolId(request.schoolId());
        visit.setVisitDate(request.visitDate());
        visit.setPurpose(request.purpose().trim());
        visit.setNotes(trim(request.notes()));
        visit.setStatus(firstNonBlank(request.status(), "SCHEDULED"));
        visit.setOutcome(trim(request.outcome()));
        SchoolVisitSchedule saved = schoolVisitScheduleRepository.save(visit);
        return toVisitDto(saved, schoolRepository.findAll().stream().collect(Collectors.toMap(School::getId, Function.identity())));
    }

    @Transactional(readOnly = true)
    public List<DistrictEducationDtos.SupportRequestDto> circuitSupportRequests(UUID districtId, UUID managerUserId) {
        List<School> schools = assignedCircuitSchools(districtId, managerUserId);
        Map<UUID, School> schoolMap = schools.stream().collect(Collectors.toMap(School::getId, Function.identity()));
        List<UUID> schoolIds = schoolIds(schools);
        List<DistrictEducationDtos.SupportRequestDto> items = new ArrayList<>();
        supportRequestRepository.findBySchoolIdInOrderByCreatedAtDesc(schoolIds).forEach(item -> items.add(toSupportDto(item, schoolMap)));
        schoolSupportRequestRepository.findBySchoolIdInAndActiveTrueOrderByCreatedAtDesc(schoolIds).forEach(item -> items.add(toLegacySupportDto(item, schoolMap)));
        items.sort(Comparator.comparing(DistrictEducationDtos.SupportRequestDto::createdAt).reversed());
        return items;
    }

    @Transactional
    public DistrictEducationDtos.SupportRequestDto updateSupportRequest(UUID districtId, UUID managerUserId, UUID requestId, DistrictEducationDtos.SupportRequestUpdateRequest request) {
        SupportRequest supportRequest = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceConflictException("Support request not found."));
        ensureSchoolInManagerScope(districtId, managerUserId, supportRequest.getSchoolId());
        if (request.status() != null && !request.status().isBlank()) {
            supportRequest.setStatus(request.status().trim().toUpperCase(Locale.ROOT));
        }
        if (request.assignedTo() != null) {
            supportRequest.setAssignedTo(request.assignedTo());
        }
        SupportRequest saved = supportRequestRepository.save(supportRequest);
        Map<UUID, School> schoolMap = schoolRepository.findAll().stream().collect(Collectors.toMap(School::getId, Function.identity()));
        return toSupportDto(saved, schoolMap);
    }

    @Transactional(readOnly = true)
    public List<DistrictEducationDtos.DistrictInterventionDto> circuitInterventions(UUID districtId, UUID managerUserId) {
        Set<UUID> visibleSchoolIds = assignedCircuitSchools(districtId, managerUserId).stream().map(School::getId).collect(Collectors.toSet());
        Map<UUID, School> schoolMap = schoolRepository.findAll().stream().collect(Collectors.toMap(School::getId, Function.identity()));
        Map<UUID, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return districtInterventionRepository.findByDistrictIdAndActiveTrueOrderByCreatedAtDesc(districtId).stream()
                .filter(item -> item.getSchoolId() == null || visibleSchoolIds.contains(item.getSchoolId()))
                .map(item -> toInterventionDto(item, schoolMap, userMap))
                .toList();
    }

    @Transactional
    public DistrictEducationDtos.DistrictInterventionDto createIntervention(UUID districtId, UUID actorUserId, DistrictEducationDtos.DistrictInterventionUpsertRequest request) {
        if (request.schoolId() != null) {
            schoolRepository.findById(request.schoolId()).orElseThrow(() -> new ResourceConflictException("School not found."));
        }
        DistrictIntervention intervention = new DistrictIntervention();
        intervention.setDistrictId(districtId);
        intervention.setCreatedByUserId(actorUserId);
        intervention.setTitle(request.title().trim());
        intervention.setDescription(trim(request.description()));
        intervention.setInterventionType(firstNonBlank(request.interventionType(), "GENERAL_SUPPORT"));
        intervention.setCategory(firstNonBlank(request.interventionType(), "GENERAL_SUPPORT"));
        intervention.setPriority(firstNonBlank(request.priority(), "MEDIUM"));
        intervention.setStatus(firstNonBlank(request.status(), "OPEN"));
        intervention.setSchoolId(request.schoolId());
        intervention.setTeacherId(request.teacherId());
        intervention.setSubject(trim(request.subject()));
        intervention.setGrade(trim(request.grade()));
        intervention.setAssignedTo(request.assignedTo());
        intervention.setDueDate(request.dueDate());
        intervention.setFollowUpDate(request.dueDate());
        intervention.setNotes(trim(request.notes()));
        intervention.setTargetScope(request.schoolId() == null ? "DISTRICT" : "SCHOOL");
        intervention.setActive(true);
        DistrictIntervention saved = districtInterventionRepository.save(intervention);
        Map<UUID, School> schoolMap = schoolRepository.findAll().stream().collect(Collectors.toMap(School::getId, Function.identity()));
        Map<UUID, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return toInterventionDto(saved, schoolMap, userMap);
    }

    @Transactional
    public DistrictEducationDtos.AiSupportPlanResponse generateAiSupportPlan(UUID districtId, UUID interventionId) {
        DistrictIntervention intervention = districtInterventionRepository.findById(interventionId)
                .filter(item -> Objects.equals(item.getDistrictId(), districtId))
                .orElseThrow(() -> new ResourceConflictException("Intervention not found."));
        String prompt = """
                Generate a concise district education support plan.
                Problem summary: %s
                Description: %s
                Subject: %s
                Grade: %s
                Intervention type: %s
                Current status: %s
                Return plain text with these sections:
                Problem summary
                Topics missed
                Suggested recovery plan
                Weekly catch-up plan
                Suggested lesson resources
                Suggested assessment activity
                Follow-up date
                """.formatted(
                intervention.getTitle(),
                firstNonBlank(intervention.getDescription(), intervention.getNotes()),
                firstNonBlank(intervention.getSubject(), "Not specified"),
                firstNonBlank(intervention.getGrade(), "Not specified"),
                firstNonBlank(intervention.getInterventionType(), intervention.getCategory()),
                intervention.getStatus()
        );
        String generated = aiProviderOrchestratorService.generateContent(prompt);
        intervention.setSupportPlan(generated);
        districtInterventionRepository.save(intervention);
        return new DistrictEducationDtos.AiSupportPlanResponse(intervention.getId(), generated);
    }

    @Transactional(readOnly = true)
    public DistrictEducationDtos.RoleDashboardResponse advisorDashboard(UUID districtId, UUID advisorUserId) {
        List<SubjectAdvisorAssignment> assignments = subjectAdvisorAssignmentRepository.findByAdvisorUserIdAndActiveTrue(advisorUserId);
        List<DistrictEducationDtos.AdvisorTeacherRowDto> teachers = advisorTeachers(districtId, advisorUserId).items();
        long openInterventions = districtInterventionRepository.findByDistrictIdAndActiveTrueOrderByCreatedAtDesc(districtId).stream()
                .filter(item -> Objects.equals(item.getAssignedTo(), advisorUserId))
                .filter(item -> !"RESOLVED".equalsIgnoreCase(item.getStatus()))
                .count();
        long resources = curriculumResourceService.getActiveDistrictResources(districtId, new CurriculumDtos.CurriculumResourceQuery(null, null, null, null, null, null, null)).stream()
                .filter(item -> "SUBJECT_ADVISOR".equalsIgnoreCase(item.source()))
                .count();
        return new DistrictEducationDtos.RoleDashboardResponse(
                "Subject Advisor Dashboard",
                "Subject-scoped teacher support, ATP monitoring, resources, and interventions.",
                List.of(
                        metric("My Subject/s", assignments.stream().map(SubjectAdvisorAssignment::getSubject).distinct().count(), "Assigned subjects", "neutral"),
                        metric("Teachers Supported", teachers.size(), "Teachers in advisor scope", "neutral"),
                        metric("Learners Reached", teachers.stream().map(DistrictEducationDtos.AdvisorTeacherRowDto::schoolId).distinct().count() * 100L, "Estimated learner reach", "neutral"),
                        metric("Average Mark", averageTeacherMark(teachers) + "%", "Teacher subject average proxy", averageTeacherMark(teachers) >= 60 ? "positive" : "warning"),
                        metric("ATP Compliance %", averageTeacherCompliance(teachers) + "%", "Subject delivery pacing", averageTeacherCompliance(teachers) >= 85 ? "positive" : "warning"),
                        metric("Schools Behind ATP", teachers.stream().filter(item -> "Behind".equalsIgnoreCase(item.status())).map(DistrictEducationDtos.AdvisorTeacherRowDto::schoolId).distinct().count(), "Subject-scoped school risk", "warning"),
                        metric("Uploaded Resources", resources, "Advisor-uploaded district resources", resources > 0 ? "positive" : "neutral"),
                        metric("Open Interventions", openInterventions, "Advisor-owned interventions", openInterventions > 0 ? "warning" : "positive")
                )
        );
    }

    @Transactional(readOnly = true)
    public DistrictEducationDtos.AdvisorTeachersResponse advisorTeachers(UUID districtId, UUID advisorUserId) {
        List<SubjectAdvisorAssignment> assignments = subjectAdvisorAssignmentRepository.findByAdvisorUserIdAndActiveTrue(advisorUserId);
        if (assignments.isEmpty()) {
            return new DistrictEducationDtos.AdvisorTeachersResponse(List.of(), List.of());
        }
        Map<UUID, School> schoolMap = schoolRepository.findByDistrictIdOrderBySchoolNameAsc(districtId).stream()
                .collect(Collectors.toMap(School::getId, Function.identity()));
        Map<UUID, SchoolSubject> subjectMap = schoolSubjectRepository.findAll().stream()
                .collect(Collectors.toMap(SchoolSubject::getId, Function.identity()));
        Map<UUID, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<DistrictEducationDtos.AdvisorTeacherRowDto> items = new ArrayList<>();
        for (TeacherAssignment assignment : teacherAssignmentRepository.findAll()) {
            School school = schoolMap.get(assignment.getSchoolId());
            if (school == null) {
                continue;
            }
            SchoolSubject subject = subjectMap.get(assignment.getSubjectId());
            if (subject == null || !matchesAdvisorAssignments(assignments, subject, assignment)) {
                continue;
            }
            CurriculumDtos.TeacherCurriculumWidgetResponse widget = curriculumService.teacherWidgets(school.getId(), assignment.getTeacherUserId());
            CurriculumDtos.TeacherCoverageItemDto coverage = widget.thisWeeksCoverage() == null ? widget.currentTopic() : widget.thisWeeksCoverage();
            int expectedWeek = coverage == null || coverage.weekNumber() == null ? 0 : coverage.weekNumber();
            int actualWeek = Math.max(0, expectedWeek - widget.topicsBehindSchedule().size());
            User teacher = userMap.get(assignment.getTeacherUserId());
            items.add(new DistrictEducationDtos.AdvisorTeacherRowDto(
                    assignment.getTeacherUserId(),
                    displayName(teacher),
                    school.getId(),
                    school.getSchoolName(),
                    subject.getSubjectName(),
                    firstNonBlank(assignment.getGrade(), subject.getGrade()),
                    teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(school.getId(), assignment.getTeacherUserId()).size(),
                    actualWeek,
                    expectedWeek,
                    widget.topicsBehindSchedule().isEmpty() ? "On Track" : "Behind",
                    Math.max(45, 75 - (widget.topicsBehindSchedule().size() * 5))
            ));
        }
        items.sort(Comparator.comparing(DistrictEducationDtos.AdvisorTeacherRowDto::teacherName));
        return new DistrictEducationDtos.AdvisorTeachersResponse(
                List.of(
                        metric("Teachers", items.size(), "Advisor scope", "neutral"),
                        metric("Schools", items.stream().map(DistrictEducationDtos.AdvisorTeacherRowDto::schoolId).distinct().count(), "Schools in scope", "neutral"),
                        metric("Behind ATP", items.stream().filter(item -> "Behind".equalsIgnoreCase(item.status())).count(), "Teachers needing support", "warning"),
                        metric("Average Mark", averageTeacherMark(items) + "%", "Subject average proxy", averageTeacherMark(items) >= 60 ? "positive" : "warning")
                ),
                items
        );
    }

    @Transactional(readOnly = true)
    public DistrictEducationDtos.TeacherProfileResponse advisorTeacherProfile(UUID districtId, UUID advisorUserId, UUID teacherUserId) {
        DistrictEducationDtos.AdvisorTeachersResponse teachers = advisorTeachers(districtId, advisorUserId);
        DistrictEducationDtos.AdvisorTeacherRowDto row = teachers.items().stream()
                .filter(item -> item.teacherUserId().equals(teacherUserId))
                .findFirst()
                .orElseThrow(() -> new ResourceConflictException("Teacher is outside advisor scope."));
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(row.schoolId(), teacherUserId);
        List<String> subjects = assignments.stream()
                .map(item -> schoolSubjectRepository.findById(item.getSubjectId()).map(SchoolSubject::getSubjectName).orElse(item.getGrade()))
                .distinct()
                .toList();
        List<String> classes = assignments.stream().map(item -> firstNonBlank(item.getGrade(), "Class")).distinct().toList();
        CurriculumDtos.TeacherCurriculumWidgetResponse widget = curriculumService.teacherWidgets(row.schoolId(), teacherUserId);
        Map<UUID, School> schoolMap = schoolRepository.findAll().stream().collect(Collectors.toMap(School::getId, Function.identity()));
        Map<UUID, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<DistrictEducationDtos.DistrictInterventionDto> interventions = districtInterventionRepository.findByDistrictIdAndActiveTrueOrderByCreatedAtDesc(districtId).stream()
                .filter(item -> teacherUserId.equals(item.getTeacherId()) || teacherUserId.equals(item.getAssignedTo()))
                .map(item -> toInterventionDto(item, schoolMap, userMap))
                .toList();
        return new DistrictEducationDtos.TeacherProfileResponse(
                teacherUserId,
                row.teacherName(),
                row.schoolName(),
                subjects,
                classes,
                widget.currentTopic(),
                widget.topicsBehindSchedule(),
                Math.max(60, 95 - (widget.topicsBehindSchedule().size() * 10)),
                row.averageMark(),
                interventions
        );
    }

    @Transactional(readOnly = true)
    public List<DistrictEducationDtos.CommonAssessmentDto> advisorAssessments(UUID districtId, UUID advisorUserId) {
        Set<String> subjects = subjectAdvisorAssignmentRepository.findByAdvisorUserIdAndActiveTrue(advisorUserId).stream()
                .map(item -> normalize(item.getSubject()))
                .collect(Collectors.toSet());
        return curriculumResourceService.getActiveDistrictResources(districtId, new CurriculumDtos.CurriculumResourceQuery("COMMON_ASSESSMENT", null, null, null, null, null, null)).stream()
                .filter(item -> subjects.isEmpty() || subjects.contains(normalize(item.subject())))
                .map(item -> new DistrictEducationDtos.CommonAssessmentDto(
                        item.id(),
                        item.title(),
                        item.subject(),
                        item.grade(),
                        item.term(),
                        null,
                        null,
                        null,
                        item.badge(),
                        item
                ))
                .toList();
    }

    @Transactional
    public DistrictEducationDtos.CommonAssessmentDto createAssessment(UUID districtId, UUID advisorUserId, DistrictEducationDtos.CommonAssessmentCreateRequest request) {
        CurriculumDtos.CurriculumAssetUpsertRequest assetRequest = new CurriculumDtos.CurriculumAssetUpsertRequest(
                "COMMON_ASSESSMENT",
                request.title(),
                request.subject(),
                request.grade(),
                request.asset().curriculumPhase(),
                request.asset().academicYear(),
                request.asset().province(),
                request.asset().versionNumber(),
                request.asset().description(),
                request.term(),
                request.asset().weekNumber(),
                request.asset().pdf(),
                request.asset().docx(),
                request.asset().excel()
        );
        CurriculumDtos.CurriculumAssetDto asset = curriculumService.saveDistrictAsset(districtId, advisorUserId, assetRequest);
        return new DistrictEducationDtos.CommonAssessmentDto(
                asset.id(),
                request.title(),
                request.subject(),
                request.grade(),
                request.term(),
                request.date(),
                request.totalMarks(),
                request.dueDate(),
                asset.badge(),
                asset
        );
    }

    private boolean matchesCircuitFilters(
            DistrictEducationDtos.CircuitCurriculumRowDto row,
            String schoolId,
            String subject,
            String grade,
            String term,
            Integer week
    ) {
        return (schoolId == null || schoolId.isBlank() || row.schoolId().toString().equalsIgnoreCase(schoolId))
                && (subject == null || subject.isBlank() || normalize(row.subject()).contains(normalize(subject)))
                && (grade == null || grade.isBlank() || normalize(row.grade()).contains(normalize(grade)))
                && (term == null || term.isBlank() || normalize(row.term()).equals(normalize(term)))
                && (week == null || Objects.equals(row.expectedWeek(), week));
    }

    private List<School> assignedCircuitSchools(UUID districtId, UUID managerUserId) {
        Circuit circuit = circuitRepository.findByDistrictIdAndManagerUserIdAndActiveTrue(districtId, managerUserId)
                .orElseThrow(() -> new ResourceConflictException("No schools are assigned to this circuit yet."));
        List<UUID> schoolIds = schoolCircuitAssignmentRepository.findByCircuitId(circuit.getId()).stream()
                .map(SchoolCircuitAssignment::getSchoolId)
                .toList();
        if (schoolIds.isEmpty()) {
            return List.of();
        }
        return schoolRepository.findAllById(schoolIds).stream()
                .sorted(Comparator.comparing(School::getSchoolName))
                .toList();
    }

    private void ensureSchoolInManagerScope(UUID districtId, UUID managerUserId, UUID schoolId) {
        boolean visible = assignedCircuitSchools(districtId, managerUserId).stream().anyMatch(item -> item.getId().equals(schoolId));
        if (!visible) {
            throw new ResourceConflictException("School is outside the circuit manager scope.");
        }
    }

    private List<DistrictEducationDtos.CircuitSchoolRowDto> computeComplianceRows(List<School> schools) {
        Map<UUID, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return schools.stream().map(school -> {
            List<SchoolUserProfile> teachers = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(school.getId(), "ROLE_TEACHER");
            List<SchoolUserProfile> learners = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(school.getId(), "ROLE_SCHOOL_STUDENT");
            List<SchoolUserProfile> principals = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(school.getId(), "ROLE_SCHOOL_ADMIN");
            int atpCompliance = schoolCompliance(school.getId());
            String riskStatus = atpCompliance >= 85 ? "Green" : atpCompliance >= 70 ? "Amber" : "Red";
            return new DistrictEducationDtos.CircuitSchoolRowDto(
                    school.getId(),
                    school.getSchoolName(),
                    principals.isEmpty() ? "Principal not assigned" : displayName(userMap.get(principals.getFirst().getUserId())),
                    learners.size(),
                    teachers.size(),
                    Math.max(45, atpCompliance - 5),
                    atpCompliance,
                    Math.max(60, atpCompliance - 3),
                    riskStatus
            );
        }).toList();
    }

    private int schoolCompliance(UUID schoolId) {
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndActiveTrue(schoolId);
        if (assignments.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (TeacherAssignment assignment : assignments) {
            CurriculumDtos.TeacherCurriculumWidgetResponse widget = curriculumService.teacherWidgets(schoolId, assignment.getTeacherUserId());
            total += Math.max(40, 100 - (widget.topicsBehindSchedule().size() * 15));
        }
        return Math.max(0, Math.min(100, Math.round(total / (float) assignments.size())));
    }

    private long countRole(List<School> schools, String roleName) {
        return schools.stream()
                .map(School::getId)
                .mapToLong(schoolId -> schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, roleName).size())
                .sum();
    }

    private int averagePassRate(List<School> schools) {
        if (schools.isEmpty()) {
            return 0;
        }
        return Math.round((float) schools.stream().mapToInt(item -> Math.max(45, schoolCompliance(item.getId()) - 5)).average().orElse(0));
    }

    private int averageCompliance(List<School> schools) {
        if (schools.isEmpty()) {
            return 0;
        }
        return Math.round((float) schools.stream().mapToInt(item -> schoolCompliance(item.getId())).average().orElse(0));
    }

    private int teacherAttendance(List<School> schools) {
        long totalTeachers = countRole(schools, "ROLE_TEACHER");
        if (totalTeachers == 0) {
            return 0;
        }
        long activeTeachers = schools.stream()
                .flatMap(item -> schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(item.getId(), "ROLE_TEACHER").stream())
                .filter(profile -> profile.getUpdatedAt() != null && profile.getUpdatedAt().isAfter(OffsetDateTime.now().minusDays(30)))
                .count();
        return Math.round((activeTeachers * 100f) / totalTeachers);
    }

    private List<UUID> schoolIds(List<School> schools) {
        return schools.stream().map(School::getId).toList();
    }

    private boolean matchesAdvisorAssignments(List<SubjectAdvisorAssignment> assignments, SchoolSubject subject, TeacherAssignment assignment) {
        return assignments.stream().anyMatch(item ->
                normalize(item.getSubject()).equals(normalize(subject.getSubjectName()))
                        && (normalize(item.getGrade()).isBlank() || normalize(item.getGrade()).equals(normalize(firstNonBlank(assignment.getGrade(), subject.getGrade()))))
                        && (normalize(item.getPhase()).isBlank() || normalize(item.getPhase()).equals(normalize(subject.getPhase())))
        );
    }

    private int averageTeacherMark(List<DistrictEducationDtos.AdvisorTeacherRowDto> items) {
        if (items.isEmpty()) {
            return 0;
        }
        return Math.round((float) items.stream().mapToInt(DistrictEducationDtos.AdvisorTeacherRowDto::averageMark).average().orElse(0));
    }

    private int averageTeacherCompliance(List<DistrictEducationDtos.AdvisorTeacherRowDto> items) {
        if (items.isEmpty()) {
            return 0;
        }
        return Math.round((float) items.stream()
                .mapToInt(item -> item.expectedWeek() == null || item.expectedWeek() == 0 ? 100 : Math.max(0, 100 - Math.max(0, item.expectedWeek() - item.atpWeek()) * 15))
                .average()
                .orElse(0));
    }

    private DistrictEducationDtos.SchoolVisitDto toVisitDto(SchoolVisitSchedule item, Map<UUID, School> schoolMap) {
        School school = schoolMap.get(item.getSchoolId());
        return new DistrictEducationDtos.SchoolVisitDto(
                item.getId(),
                item.getSchoolId(),
                school == null ? "School" : school.getSchoolName(),
                item.getVisitDate(),
                item.getPurpose(),
                item.getStatus(),
                item.getNotes(),
                item.getOutcome(),
                item.getCreatedAt()
        );
    }

    private DistrictEducationDtos.SupportRequestDto toSupportDto(SupportRequest item, Map<UUID, School> schoolMap) {
        School school = schoolMap.get(item.getSchoolId());
        User assigned = item.getAssignedTo() == null ? null : userRepository.findById(item.getAssignedTo()).orElse(null);
        return new DistrictEducationDtos.SupportRequestDto(
                item.getId(),
                item.getSchoolId(),
                school == null ? "School" : school.getSchoolName(),
                item.getRequestType(),
                item.getSubject(),
                item.getGrade(),
                item.getDescription(),
                item.getStatus(),
                item.getAssignedTo(),
                displayName(assigned),
                item.getCreatedAt()
        );
    }

    private DistrictEducationDtos.SupportRequestDto toLegacySupportDto(SchoolSupportRequest item, Map<UUID, School> schoolMap) {
        School school = schoolMap.get(item.getSchoolId());
        return new DistrictEducationDtos.SupportRequestDto(
                item.getId(),
                item.getSchoolId(),
                school == null ? "School" : school.getSchoolName(),
                item.getCategory(),
                null,
                null,
                item.getMessage(),
                item.getStatus(),
                null,
                null,
                item.getCreatedAt()
        );
    }

    private DistrictEducationDtos.DistrictInterventionDto toInterventionDto(DistrictIntervention item, Map<UUID, School> schoolMap, Map<UUID, User> userMap) {
        School school = item.getSchoolId() == null ? null : schoolMap.get(item.getSchoolId());
        User teacher = item.getTeacherId() == null ? null : userMap.get(item.getTeacherId());
        User assignee = item.getAssignedTo() == null ? null : userMap.get(item.getAssignedTo());
        return new DistrictEducationDtos.DistrictInterventionDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                firstNonBlank(item.getInterventionType(), item.getCategory()),
                item.getPriority(),
                item.getStatus(),
                item.getSchoolId(),
                school == null ? null : school.getSchoolName(),
                item.getTeacherId(),
                displayName(teacher),
                item.getSubject(),
                item.getGrade(),
                item.getAssignedTo(),
                displayName(assignee),
                item.getDueDate(),
                item.getSupportPlan(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private DistrictDtos.MetricCardDto metric(String label, long value, String helperText, String tone) {
        return new DistrictDtos.MetricCardDto(label, String.valueOf(value), helperText, tone);
    }

    private DistrictDtos.MetricCardDto metric(String label, String value, String helperText, String tone) {
        return new DistrictDtos.MetricCardDto(label, value, helperText, tone);
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        String value = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return value.isBlank() ? user.getEmail() : value;
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
