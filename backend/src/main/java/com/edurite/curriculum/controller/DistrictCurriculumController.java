package com.edurite.curriculum.controller;

import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.service.CurriculumService;
import com.edurite.district.service.DistrictAccessService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/district/curriculum", "/api/district/curriculum"})
public class DistrictCurriculumController {

    private final DistrictAccessService districtAccessService;
    private final CurriculumService curriculumService;

    public DistrictCurriculumController(DistrictAccessService districtAccessService, CurriculumService curriculumService) {
        this.districtAccessService = districtAccessService;
        this.curriculumService = curriculumService;
    }

    @GetMapping("/assets")
    public List<CurriculumDtos.CurriculumAssetDto> assets(Principal principal, @RequestParam(required = false) String repositoryType) {
        DistrictAccessService.AccessContext context = readContext(principal);
        return curriculumService.districtAssets(context.districtId(), repositoryType);
    }

    @PostMapping("/assets")
    public CurriculumDtos.CurriculumAssetDto createAsset(Principal principal, @Valid @RequestBody CurriculumDtos.CurriculumAssetUpsertRequest request) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        return curriculumService.saveDistrictAsset(context.districtId(), context.userId(), request);
    }

    @PutMapping("/assets/{assetId}")
    public CurriculumDtos.CurriculumAssetDto updateAsset(
            Principal principal,
            @PathVariable UUID assetId,
            @Valid @RequestBody CurriculumDtos.CurriculumAssetUpsertRequest request
    ) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        return curriculumService.updateDistrictAsset(context.districtId(), context.userId(), assetId, request);
    }

    @PostMapping("/assets/{assetId}/archive")
    public CurriculumDtos.CurriculumAssetDto archiveAsset(Principal principal, @PathVariable UUID assetId) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        return curriculumService.archiveDistrictAsset(context.districtId(), assetId);
    }

    @DeleteMapping("/assets/{assetId}")
    public void deleteAsset(Principal principal, @PathVariable UUID assetId) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        curriculumService.deleteDistrictAsset(context.districtId(), assetId);
    }

    @GetMapping("/assets/{assetId}/download")
    public ResponseEntity<byte[]> downloadAsset(
            Principal principal,
            @PathVariable UUID assetId,
            @RequestParam(defaultValue = "PDF") String format
    ) {
        DistrictAccessService.AccessContext context = readContext(principal);
        CurriculumService.CurriculumFileResponse file = curriculumService.downloadDistrictAsset(context.districtId(), assetId, format);
        return fileResponse(file);
    }

    @GetMapping("/calendar")
    public CurriculumDtos.DistrictCurriculumCalendarResponse calendar(Principal principal) {
        DistrictAccessService.AccessContext context = readContext(principal);
        return curriculumService.districtCalendar(context.districtId());
    }

    @PostMapping("/assets/{assetId}/extract")
    public CurriculumDtos.CurriculumAssetDto extractAtp(Principal principal, @PathVariable UUID assetId) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        return curriculumService.extractDistrictAtp(context.districtId(), context.userId(), assetId);
    }

    @PostMapping("/calendar/items")
    public CurriculumDtos.CurriculumCalendarItemDto createCalendarItem(
            Principal principal,
            @Valid @RequestBody CurriculumDtos.CurriculumCalendarItemUpsertRequest request
    ) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        return curriculumService.saveCalendarItem(context.districtId(), context.userId(), null, request);
    }

    @PutMapping("/calendar/items/{itemId}")
    public CurriculumDtos.CurriculumCalendarItemDto updateCalendarItem(
            Principal principal,
            @PathVariable UUID itemId,
            @Valid @RequestBody CurriculumDtos.CurriculumCalendarItemUpsertRequest request
    ) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        return curriculumService.saveCalendarItem(context.districtId(), context.userId(), itemId, request);
    }

    @PostMapping("/calendar/items/{itemId}/publish")
    public CurriculumDtos.CurriculumCalendarItemDto publishCalendarItem(Principal principal, @PathVariable UUID itemId) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        return curriculumService.publishCalendarItem(context.districtId(), context.userId(), itemId);
    }

    @PostMapping("/calendar/items/{itemId}/archive")
    public CurriculumDtos.CurriculumCalendarItemDto archiveCalendarItem(Principal principal, @PathVariable UUID itemId) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        return curriculumService.archiveCalendarItem(context.districtId(), itemId);
    }

    @PostMapping("/calendar/sync")
    public CurriculumDtos.CurriculumPublishRepairResponse syncPublishedCalendarToSchools(Principal principal) {
        DistrictAccessService.AccessContext context = writeContext(principal);
        return curriculumService.syncPublishedCalendarToSchools(context.districtId());
    }

    @GetMapping("/compliance")
    public CurriculumDtos.CurriculumComplianceResponse compliance(Principal principal) {
        DistrictAccessService.AccessContext context = readContext(principal);
        return curriculumService.districtCompliance(context.districtId());
    }

    private DistrictAccessService.AccessContext readContext(Principal principal) {
        return districtAccessService.requireDistrictContext(principal, Set.of(
                DistrictAccessService.ROLE_DISTRICT_ADMIN,
                DistrictAccessService.ROLE_DISTRICT_DIRECTOR,
                DistrictAccessService.ROLE_CIRCUIT_MANAGER,
                DistrictAccessService.ROLE_SUBJECT_ADVISOR
        ));
    }

    private DistrictAccessService.AccessContext writeContext(Principal principal) {
        return districtAccessService.requireDistrictContext(principal, Set.of(
                DistrictAccessService.ROLE_DISTRICT_ADMIN,
                DistrictAccessService.ROLE_DISTRICT_DIRECTOR,
                DistrictAccessService.ROLE_SUBJECT_ADVISOR
        ));
    }

    private ResponseEntity<byte[]> fileResponse(CurriculumService.CurriculumFileResponse file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}
