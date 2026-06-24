package com.edurite.curriculum.controller;

import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.service.CurriculumService;
import com.edurite.curriculum.service.CurriculumResourceService;
import com.edurite.school.service.SchoolAccessService;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1", "/api"})
public class CurriculumResourceController {

    private final SchoolAccessService schoolAccessService;
    private final CurriculumService curriculumService;
    private final CurriculumResourceService curriculumResourceService;

    public CurriculumResourceController(
            SchoolAccessService schoolAccessService,
            CurriculumService curriculumService,
            CurriculumResourceService curriculumResourceService
    ) {
        this.schoolAccessService = schoolAccessService;
        this.curriculumService = curriculumService;
        this.curriculumResourceService = curriculumResourceService;
    }

    @GetMapping("/school/curriculum/resources")
    public List<CurriculumDtos.CurriculumAssetDto> schoolResources(
            Principal principal,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "phase", required = false) String phase,
            @RequestParam(value = "term", required = false) String term,
            @RequestParam(value = "week", required = false) Integer week,
            @RequestParam(value = "academicYear", required = false) Integer academicYear
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return curriculumResourceService.getDistrictResourcesForSchool(
                context.schoolId(),
                new CurriculumDtos.CurriculumResourceQuery(type, subject, grade, phase, term, week, academicYear)
        );
    }

    @GetMapping("/teacher/curriculum/calendar")
    public CurriculumDtos.TeacherCurriculumWidgetResponse teacherCalendar(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return curriculumService.teacherWidgets(context.schoolId(), context.userId());
    }

    @PostMapping("/teacher/curriculum/calendar/{calendarItemId}/lesson-plan")
    public CurriculumDtos.TeacherLessonPlanResponse createTeacherLessonPlan(
            Principal principal,
            @PathVariable UUID calendarItemId,
            @RequestBody(required = false) CurriculumDtos.TeacherLessonPlanCreateRequest request
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        boolean regenerate = request != null && Boolean.TRUE.equals(request.regenerate());
        return curriculumService.createLessonPlanFromCalendarItem(context.schoolId(), context.userId(), calendarItemId, regenerate);
    }

    @GetMapping("/teacher/curriculum/resources")
    public List<CurriculumDtos.CurriculumAssetDto> teacherResources(
            Principal principal,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "phase", required = false) String phase,
            @RequestParam(value = "term", required = false) String term,
            @RequestParam(value = "week", required = false) Integer week,
            @RequestParam(value = "academicYear", required = false) Integer academicYear
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return curriculumResourceService.getDistrictResourcesForTeacher(
                context.schoolId(),
                context.userId(),
                new CurriculumDtos.CurriculumResourceQuery(type, subject, grade, phase, term, week, academicYear)
        );
    }

    @GetMapping("/curriculum/resources/{resourceId}/download")
    public ResponseEntity<byte[]> downloadResource(
            Principal principal,
            @PathVariable UUID resourceId,
            @RequestParam(value = "format", required = false) String format
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN, SchoolAccessService.ROLE_TEACHER));
        CurriculumResourceService.ResourceFileResponse response = SchoolAccessService.ROLE_TEACHER.equals(context.roleName())
                ? curriculumResourceService.downloadResourceForTeacher(context.schoolId(), context.userId(), resourceId, format)
                : curriculumResourceService.downloadResourceForSchool(context.schoolId(), resourceId, format);
        return fileResponse(response, false);
    }

    @GetMapping("/curriculum/resources/{resourceId}/view")
    public ResponseEntity<byte[]> viewResource(
            Principal principal,
            @PathVariable UUID resourceId,
            @RequestParam(value = "format", required = false) String format
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN, SchoolAccessService.ROLE_TEACHER));
        CurriculumResourceService.ResourceFileResponse response = SchoolAccessService.ROLE_TEACHER.equals(context.roleName())
                ? curriculumResourceService.viewResourceForTeacher(context.schoolId(), context.userId(), resourceId, format)
                : curriculumResourceService.viewResourceForSchool(context.schoolId(), resourceId, format);
        return fileResponse(response, true);
    }

    private ResponseEntity<byte[]> fileResponse(CurriculumResourceService.ResourceFileResponse file, boolean inlineRequested) {
        MediaType mediaType = MediaType.parseMediaType(file.contentType());
        ContentDisposition disposition = (inlineRequested && file.inline()
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(file.fileName())
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }
}
