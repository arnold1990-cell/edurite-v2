package com.edurite.company.controller;

import com.edurite.company.dto.CompanyBursaryDto;
import com.edurite.company.dto.CompanyBursaryUpsertRequest;
import com.edurite.company.dto.CompanyDocumentDto;
import com.edurite.company.dto.CompanyProfileDto;
import com.edurite.company.dto.CompanyProfileUpdateRequest;
import com.edurite.company.dto.CompanyStudentSearchResultDto;
import com.edurite.company.service.CompanyService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping({"/api/v1/companies", "/api/companies"})
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/me")
    public CompanyProfileDto me(Principal principal) { return companyService.getMe(principal); }

    @PutMapping("/me")
    public CompanyProfileDto updateMe(Principal principal, @Valid @RequestBody CompanyProfileUpdateRequest request) { return companyService.updateMe(principal, request); }

    @PostMapping("/me/documents")
    public ResponseEntity<CompanyDocumentDto> uploadDocument(Principal principal, @RequestPart("file") MultipartFile file, @RequestParam String documentType) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.uploadDocument(principal, file, documentType));
    }

    @GetMapping("/me/documents")
    public List<CompanyDocumentDto> listDocuments(Principal principal) { return companyService.listDocuments(principal); }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Principal principal) { return companyService.dashboardSummary(principal); }

    @PostMapping("/bursaries")
    public ResponseEntity<CompanyBursaryDto> createBursary(Principal principal, @Valid @RequestBody CompanyBursaryUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createBursary(principal, request));
    }

    @PutMapping("/bursaries/{id}")
    public CompanyBursaryDto updateBursary(Principal principal, @PathVariable UUID id, @Valid @RequestBody CompanyBursaryUpsertRequest request) { return companyService.updateBursary(principal, id, request); }

    @GetMapping("/bursaries")
    public List<CompanyBursaryDto> companyBursaries(Principal principal) { return companyService.listOwnBursaries(principal); }

    @GetMapping("/bursaries/{id}")
    public CompanyBursaryDto companyBursary(Principal principal, @PathVariable UUID id) { return companyService.getOwnBursary(principal, id); }

    @PatchMapping("/bursaries/{id}/unpublish")
    public CompanyBursaryDto unpublishBursary(Principal principal, @PathVariable UUID id) { return companyService.setBursaryStatus(principal, id, "UNPUBLISHED"); }

    @PatchMapping("/bursaries/{id}/close")
    public CompanyBursaryDto closeBursary(Principal principal, @PathVariable UUID id) { return companyService.setBursaryStatus(principal, id, "CLOSED"); }

    @PatchMapping("/bursaries/{id}/reopen")
    public CompanyBursaryDto reopenBursary(Principal principal, @PathVariable UUID id) { return companyService.setBursaryStatus(principal, id, "ACTIVE"); }

    @GetMapping("/students/search")
    public List<CompanyStudentSearchResultDto> searchStudents(Principal principal,
            @RequestParam(defaultValue = "") String fieldOfInterest,
            @RequestParam(defaultValue = "") String qualificationLevel,
            @RequestParam(defaultValue = "") String skills,
            @RequestParam(defaultValue = "") String location) {
        return companyService.searchStudents(principal, fieldOfInterest, qualificationLevel, skills, location);
    }

    @PostMapping("/students/{studentId}/bookmarks")
    public Map<String, Object> bookmarkStudent(Principal principal, @PathVariable UUID studentId, @RequestBody(required = false) Map<String, String> payload) {
        return companyService.addBookmark(principal, studentId, payload == null ? Map.of() : payload);
    }

    @GetMapping("/students/bookmarks")
    public List<Map<String, Object>> bookmarks(Principal principal) { return companyService.listBookmarks(principal); }

    @PostMapping("/students/{studentId}/shortlists")
    public Map<String, Object> shortlistStudent(Principal principal, @PathVariable UUID studentId, @RequestBody(required = false) Map<String, String> payload) {
        return companyService.addShortlist(principal, studentId, payload == null ? Map.of() : payload);
    }

    @GetMapping("/students/shortlists")
    public List<Map<String, Object>> shortlists(Principal principal) { return companyService.listShortlists(principal); }

    @PostMapping("/students/{studentId}/messages")
    public Map<String, Object> messageStudent(Principal principal, @PathVariable UUID studentId, @RequestBody Map<String, String> payload) {
        return companyService.sendMessage(principal, studentId, payload);
    }

    @GetMapping("/messages")
    public List<Map<String, Object>> messages(Principal principal) { return companyService.listMessages(principal); }

    @PostMapping("/students/{studentId}/invitations")
    public Map<String, Object> inviteStudent(Principal principal, @PathVariable UUID studentId, @RequestBody Map<String, String> payload) {
        return companyService.sendInvitation(principal, studentId, payload);
    }

    @GetMapping("/invitations")
    public List<Map<String, Object>> invitations(Principal principal) { return companyService.listInvitations(principal); }
}

