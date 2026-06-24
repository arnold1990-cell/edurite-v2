package com.edurite.company.controller;

import com.edurite.admin.dto.AdminCompanyDto;
import com.edurite.admin.service.AdminService;
import com.edurite.company.dto.AdminCompanyReviewRequest;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/companies", "/api/admin/companies"})
public class AdminCompanyReviewController {

    private final AdminService adminService;

    public AdminCompanyReviewController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<AdminCompanyDto> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        return adminService.companies(search, status, includeDeleted);
    }

    @GetMapping("/pending")
    public List<AdminCompanyDto> pending() {
        return adminService.pendingCompanies();
    }

    @GetMapping("/{id}")
    public AdminCompanyDto details(@PathVariable UUID id) {
        return adminService.companyById(id);
    }

    @PatchMapping("/{id}/approve")
    public AdminCompanyDto approve(@PathVariable UUID id, @RequestBody AdminCompanyReviewRequest request, Principal principal) {
        return adminService.approveCompany(id, request.notes(), principal);
    }

    @PatchMapping("/{id}/reject")
    public AdminCompanyDto reject(@PathVariable UUID id, @RequestBody AdminCompanyReviewRequest request, Principal principal) {
        return adminService.rejectCompany(id, request.notes(), principal);
    }

    @PatchMapping("/{id}/more-info")
    public AdminCompanyDto moreInfo(@PathVariable UUID id, @RequestBody AdminCompanyReviewRequest request, Principal principal) {
        return adminService.requestCompanyMoreInfo(id, request.notes(), principal);
    }

    @PatchMapping("/{id}/suspend")
    public AdminCompanyDto suspend(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload, Principal principal) {
        return adminService.suspendCompany(id, payload == null ? null : payload.get("notes"), principal);
    }

    @PatchMapping("/{id}/reactivate")
    public AdminCompanyDto reactivate(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload, Principal principal) {
        return adminService.reactivateCompany(id, payload == null ? null : payload.get("notes"), principal);
    }

    @DeleteMapping("/{id}")
    public AdminCompanyDto delete(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload, Principal principal) {
        return adminService.deleteCompany(id, payload == null ? null : payload.get("reason"), principal);
    }
}

