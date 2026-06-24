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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
        "/api/v1/school-admin/curriculum",
        "/api/school-admin/curriculum",
        "/api/v1/school/curriculum",
        "/api/school/curriculum"
})
public class SchoolCurriculumController {

    private final SchoolAccessService schoolAccessService;
    private final CurriculumService curriculumService;

    public SchoolCurriculumController(SchoolAccessService schoolAccessService, CurriculumService curriculumService) {
        this.schoolAccessService = schoolAccessService;
        this.curriculumService = curriculumService;
    }

    @GetMapping("/assets")
    public List<CurriculumDtos.CurriculumAssetDto> assets(Principal principal, @RequestParam(required = false) String repositoryType) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return curriculumService.schoolAssets(context.schoolId(), repositoryType);
    }

    @GetMapping("/calendar")
    public CurriculumDtos.SchoolCurriculumCalendarResponse calendar(Principal principal) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return curriculumService.schoolCalendar(context.schoolId());
    }

    @PostMapping("/assets")
    public CurriculumDtos.CurriculumAssetDto createAsset(Principal principal, @Valid @RequestBody CurriculumDtos.CurriculumAssetUpsertRequest request) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        return curriculumService.saveSchoolAsset(context.schoolId(), context.userId(), request);
    }

    @GetMapping("/assets/{assetId}/download")
    public ResponseEntity<byte[]> downloadAsset(
            Principal principal,
            @PathVariable UUID assetId,
            @RequestParam(defaultValue = "PDF") String format
    ) {
        SchoolAccessService.AccessContext context = schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
        CurriculumService.CurriculumFileResponse file = curriculumService.downloadSchoolAsset(context.schoolId(), assetId, format);
        return fileResponse(file);
    }

    private ResponseEntity<byte[]> fileResponse(CurriculumService.CurriculumFileResponse file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}
