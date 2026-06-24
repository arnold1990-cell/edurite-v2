package com.edurite.curriculum.controller;

import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.service.CurriculumService;
import com.edurite.school.service.SchoolAccessService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/teacher/portal/curriculum", "/api/teacher/portal/curriculum"})
public class TeacherCurriculumController {

    private final SchoolAccessService schoolAccessService;
    private final CurriculumService curriculumService;

    public TeacherCurriculumController(SchoolAccessService schoolAccessService, CurriculumService curriculumService) {
        this.schoolAccessService = schoolAccessService;
        this.curriculumService = curriculumService;
    }

    @GetMapping("/widgets")
    public CurriculumDtos.TeacherCurriculumWidgetResponse widgets(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return curriculumService.teacherWidgets(context.schoolId(), context.userId());
    }

    @GetMapping("/reminders")
    public List<CurriculumDtos.TeacherReminderDto> reminders(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return curriculumService.teacherReminders(context.schoolId(), context.userId());
    }

    @GetMapping("/assets/{assetId}/download")
    public ResponseEntity<byte[]> downloadAsset(
            Principal principal,
            @PathVariable UUID assetId,
            @RequestParam(defaultValue = "PDF") String format
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        CurriculumService.CurriculumFileResponse file = curriculumService.downloadSchoolAsset(context.schoolId(), assetId, format);
        return fileResponse(file);
    }

    @PatchMapping("/weeks/{weekPlanId}")
    public CurriculumDtos.TeacherCoverageItemDto updateProgress(
            Principal principal,
            @PathVariable UUID weekPlanId,
            @Valid @RequestBody CurriculumDtos.TeacherProgressUpdateRequest request
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return curriculumService.updateTeacherProgress(context.schoolId(), context.userId(), weekPlanId, request);
    }

    @PostMapping("/lesson-plan/generate")
    public CurriculumDtos.TeacherLessonPlanResponse generateLessonPlan(
            Principal principal,
            @Valid @RequestBody CurriculumDtos.TeacherLessonPlanGenerateRequest request
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_TEACHER));
        return curriculumService.generateLessonPlan(context.schoolId(), context.userId(), request.weekPlanId());
    }

    private ResponseEntity<byte[]> fileResponse(CurriculumService.CurriculumFileResponse file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}
