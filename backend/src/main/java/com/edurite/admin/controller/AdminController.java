package com.edurite.admin.controller;

import com.edurite.admin.dto.AdminAnalyticsDto;
import com.edurite.admin.dto.AdminBulkUploadResultDto;
import com.edurite.admin.dto.AdminBursaryDto;
import com.edurite.admin.dto.AdminDistrictDtos;
import com.edurite.admin.dto.AdminPlatformSettingsDto;
import com.edurite.admin.dto.AdminPlatformSettingsUpdateRequest;
import com.edurite.admin.dto.AdminUserDto;
import com.edurite.admin.service.AdminService;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/admin", "/api/admin"})
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<AdminUserDto> users(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String companyStatus,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        return adminService.users(search, status, accountType, companyStatus, includeDeleted);
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserDto userStatus(@PathVariable UUID id, @RequestBody Map<String, Boolean> payload, Principal principal) {
        return adminService.updateUserStatus(id, Boolean.TRUE.equals(payload.get("active")), principal);
    }

    @PatchMapping("/users/{id}/suspend")
    public AdminUserDto suspendUser(@PathVariable UUID id, Principal principal) {
        return adminService.updateUserStatus(id, false, principal);
    }

    @PatchMapping("/users/{id}/unsuspend")
    public AdminUserDto unsuspendUser(@PathVariable UUID id, Principal principal) {
        return adminService.updateUserStatus(id, true, principal);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, String> deleteUser(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload, Principal principal) {
        return adminService.deleteUser(id, payload == null ? null : payload.get("reason"), principal);
    }

    @DeleteMapping("/users/{id}/account")
    public Map<String, String> deleteUserAccount(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload, Principal principal) {
        return adminService.deleteUser(id, payload == null ? null : payload.get("reason"), principal);
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> roles() {
        return adminService.roles();
    }

    @PostMapping("/roles")
    public Map<String, Object> createRole(@RequestBody Map<String, Object> payload, Principal principal) {
        return adminService.createRole(payload, principal);
    }

    @PutMapping("/roles/{id}")
    public Map<String, Object> updateRole(@PathVariable UUID id, @RequestBody Map<String, Object> payload, Principal principal) {
        return adminService.updateRole(id, payload, principal);
    }

    @DeleteMapping("/roles/{id}")
    public Map<String, String> deleteRole(@PathVariable UUID id, Principal principal) {
        return adminService.deleteRole(id, principal);
    }

    @GetMapping("/bursaries")
    public List<AdminBursaryDto> bursaries(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        return adminService.bursaries(status, companyId, fromDate, toDate, includeDeleted);
    }

    @GetMapping("/bursaries/pending")
    public List<AdminBursaryDto> pendingBursaries() {
        return adminService.pendingBursaries();
    }

    @PatchMapping("/bursaries/{id}/review")
    public AdminBursaryDto reviewBursary(@PathVariable UUID id, @RequestBody Map<String, String> payload, Principal principal) {
        return adminService.reviewBursary(id, payload.get("decision"), payload.get("comment"), principal);
    }

    @PatchMapping("/bursaries/{id}/suspend")
    public AdminBursaryDto suspendBursary(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload, Principal principal) {
        return adminService.suspendBursary(id, payload == null ? null : payload.get("reason"), principal);
    }

    @PatchMapping("/bursaries/{id}/reactivate")
    public AdminBursaryDto reactivateBursary(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload, Principal principal) {
        return adminService.reactivateBursary(id, payload == null ? null : payload.get("reason"), principal);
    }

    @DeleteMapping("/bursaries/{id}")
    public AdminBursaryDto deleteBursary(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload, Principal principal) {
        return adminService.deleteBursary(id, payload == null ? null : payload.get("reason"), principal);
    }

    @GetMapping("/settings")
    public AdminPlatformSettingsDto settings() {
        return adminService.settings();
    }

    @PutMapping("/settings")
    public AdminPlatformSettingsDto updateSettings(@RequestBody AdminPlatformSettingsUpdateRequest request, Principal principal) {
        return adminService.updateSettings(request, principal);
    }

    @PostMapping("/users/bulk-upload")
    public AdminBulkUploadResultDto bulkUpload(@RequestPart("file") MultipartFile file, Principal principal) throws IOException {
        return adminService.bulkUploadUsers(file, principal);
    }

    @GetMapping(value = "/users/bulk-upload/template", produces = "text/csv")
    public ResponseEntity<String> bulkUploadTemplate() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(adminService.bulkUploadTemplate());
    }

    @GetMapping("/audit-logs")
    public List<Map<String, Object>> auditLogs() {
        return adminService.auditLogs();
    }

    @GetMapping("/analytics")
    public AdminAnalyticsDto analytics() {
        return adminService.analytics();
    }

    @GetMapping("/districts")
    public AdminDistrictDtos.AdminDistrictManagementResponse districts() {
        return adminService.districts();
    }

    @PostMapping("/districts")
    public AdminDistrictDtos.AdminDistrictItemDto createDistrict(
            @jakarta.validation.Valid @RequestBody AdminDistrictDtos.AdminCreateDistrictRequest request,
            Principal principal
    ) {
        return adminService.createDistrict(request, principal);
    }

    @PutMapping("/districts/{districtId}")
    public AdminDistrictDtos.AdminDistrictItemDto updateDistrict(
            @PathVariable UUID districtId,
            @jakarta.validation.Valid @RequestBody AdminDistrictDtos.AdminUpdateDistrictRequest request,
            Principal principal
    ) {
        return adminService.updateDistrict(districtId, request, principal);
    }

    @PostMapping("/districts/{districtId}/create-admin")
    public AdminDistrictDtos.AdminDistrictItemDto createDistrictAdmin(
            @PathVariable UUID districtId,
            Principal principal
    ) {
        return adminService.createDistrictAdmin(districtId, principal);
    }
}

