package com.edurite.auth.controller;

import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.auth.service.EmailVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/auth/email-verification", "/api/auth/email-verification"})
public class EmailVerificationController {
    private final EmailVerificationService service;
    public EmailVerificationController(EmailVerificationService service) { this.service = service; }
    public record Resend(@NotBlank @Email String email) {}
    public record Verify(@NotBlank @Email String email, @NotBlank @Size(max = 128) String token) {}
    @PostMapping("/resend")
    public VerificationStatusResponse resend(@Valid @RequestBody Resend request) { return service.resend(request.email()); }
    @PostMapping("/verify")
    public VerificationStatusResponse verify(@Valid @RequestBody Verify request) { return service.verify(request.email(), request.token()); }
}
