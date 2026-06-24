package com.edurite.student.controller;

import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.dto.StudentProfileUpsertRequest;
import com.edurite.student.dto.StudentSavedProfileDto;
import com.edurite.student.dto.StudentSavedProfileSaveRequest;
import com.edurite.student.dto.StudentSavedProfileSummaryDto;
import com.edurite.student.dto.StudentPreferencesDto;
import com.edurite.student.dto.StudentSettingsDto;
import com.edurite.student.service.StudentPreferenceService;
import com.edurite.student.service.StudentService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/student", "/api/student"})
public class StudentController {

    private final StudentService studentService;
    private final StudentPreferenceService studentPreferenceService;

    public StudentController(StudentService studentService, StudentPreferenceService studentPreferenceService) {
        this.studentService = studentService;
        this.studentPreferenceService = studentPreferenceService;
    }

    @GetMapping("/profile")
    public StudentProfileDto profile(Principal principal) {
        return studentService.getProfile(principal);
    }

    @PutMapping("/profile")
    public StudentProfileDto upsert(Principal principal, @Valid @org.springframework.web.bind.annotation.RequestBody StudentProfileUpsertRequest request) {
        return studentService.upsertProfile(principal, request);
    }

    @GetMapping("/profile/saved")
    public List<StudentSavedProfileSummaryDto> savedProfiles(Principal principal) {
        return studentService.listSavedProfiles(principal);
    }

    @PostMapping("/profile/saved")
    public StudentSavedProfileDto saveProfileVersion(
            Principal principal,
            @Valid @org.springframework.web.bind.annotation.RequestBody StudentSavedProfileSaveRequest request
    ) {
        return studentService.saveProfileVersion(principal, request);
    }

    @GetMapping("/profile/saved/{savedProfileId}")
    public StudentSavedProfileDto savedProfileDetails(Principal principal, @PathVariable UUID savedProfileId) {
        return studentService.savedProfileDetails(principal, savedProfileId);
    }

    @PostMapping("/profile/saved/{savedProfileId}/apply")
    public StudentProfileDto applySavedProfile(Principal principal, @PathVariable UUID savedProfileId) {
        return studentService.applySavedProfile(principal, savedProfileId);
    }

    @DeleteMapping("/profile/saved/{savedProfileId}")
    public ResponseEntity<Map<String, String>> deleteSavedProfile(Principal principal, @PathVariable UUID savedProfileId) {
        studentService.deleteSavedProfile(principal, savedProfileId);
        return ResponseEntity.ok(Map.of("message", "Saved profile deleted"));
    }

    @PostMapping("/profile/cv")
    public StudentProfileDto uploadCv(Principal principal, @RequestParam("file") MultipartFile file) throws IOException {
        return studentService.uploadDocument(principal, file, "cv");
    }

    @PostMapping("/profile/transcript")
    public StudentProfileDto uploadTranscript(Principal principal, @RequestParam("file") MultipartFile file) throws IOException {
        return studentService.uploadDocument(principal, file, "transcript");
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Principal principal) {
        return studentService.dashboard(principal);
    }

    @GetMapping("/settings")
    public StudentSettingsDto settings(Principal principal) {
        return studentService.getSettings(principal);
    }

    @PutMapping("/settings")
    public StudentSettingsDto updateSettings(Principal principal, @org.springframework.web.bind.annotation.RequestBody StudentSettingsDto request) {
        return studentService.updateSettings(principal, request);
    }

    @PostMapping("/careers/{careerId}/save")
    public ResponseEntity<Map<String, String>> saveCareer(Principal principal, @PathVariable UUID careerId) {
        studentService.saveCareer(principal, careerId);
        return ResponseEntity.ok(Map.of("message", "Career saved"));
    }

    @PostMapping("/bursaries/{bursaryId}/save")
    public ResponseEntity<Map<String, String>> saveBursary(Principal principal, @PathVariable UUID bursaryId) {
        studentService.saveBursary(principal, bursaryId);
        return ResponseEntity.ok(Map.of("message", "Bursary saved"));
    }

    @DeleteMapping("/careers/{careerId}/save")
    public ResponseEntity<Map<String, String>> unsaveCareer(Principal principal, @PathVariable UUID careerId) {
        studentService.unsaveCareer(principal, careerId);
        return ResponseEntity.ok(Map.of("message", "Career removed"));
    }

    @DeleteMapping("/bursaries/{bursaryId}/save")
    public ResponseEntity<Map<String, String>> unsaveBursary(Principal principal, @PathVariable UUID bursaryId) {
        studentService.unsaveBursary(principal, bursaryId);
        return ResponseEntity.ok(Map.of("message", "Bursary removed"));
    }

    @GetMapping("/careers/saved")
    public Map<String, Object> savedCareers(Principal principal) {
        return Map.of("items", studentService.savedCareerIds(principal));
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/bursaries/bookmarks")
    public Map<String, Object> bookmarkedBursaries(Principal principal) {
        return Map.of("items", studentService.savedBursaries(principal));
    }

    @GetMapping("/bursaries/saved")
    public Map<String, Object> savedBursaries(Principal principal) {
        return Map.of("items", studentService.savedBursaryIds(principal));
    }

    @GetMapping("/preferences")
    public StudentPreferencesDto preferences(Principal principal) {
        return studentPreferenceService.get(principal);
    }

    @PutMapping("/preferences")
    public StudentPreferencesDto updatePreferences(Principal principal, @org.springframework.web.bind.annotation.RequestBody StudentPreferencesDto request) {
        return studentPreferenceService.update(principal, request);
    }
}


