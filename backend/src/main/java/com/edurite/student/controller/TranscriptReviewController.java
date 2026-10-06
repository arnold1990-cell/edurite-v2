package com.edurite.student.controller;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/admin/students")
public class TranscriptReviewController {
    private final StudentService service;
    public TranscriptReviewController(StudentService service) { this.service = service; }
    public record Review(@NotBlank String documentId) {}
    @PostMapping("/{studentId}/transcript/verify")
    public void verify(Principal principal, @PathVariable UUID studentId, @Valid @RequestBody Review review) { service.verifyTranscript(principal, studentId, review.documentId()); }
}
