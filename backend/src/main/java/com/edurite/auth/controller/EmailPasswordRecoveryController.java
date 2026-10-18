package com.edurite.auth.controller;

import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.auth.service.EmailPasswordRecoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/auth/forgot-password/email", "/api/auth/forgot-password/email"})
public class EmailPasswordRecoveryController {
    private final EmailPasswordRecoveryService service;
    public EmailPasswordRecoveryController(EmailPasswordRecoveryService service) { this.service = service; }
    public record Request(@NotBlank @Email @Size(max = 255) String email) {}
    public record Verify(@NotBlank @Email @Size(max = 255) String email, @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {}
    public record Reset(@NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 43, max = 43) String resetToken,
            @NotBlank @Size(min = 8, max = 72) String newPassword,
            @NotBlank String confirmPassword) {
        @AssertTrue(message = "Passwords do not match")
        public boolean isPasswordConfirmed() { return newPassword != null && newPassword.equals(confirmPassword); }
        @AssertTrue(message = "Password must be at most 72 UTF-8 bytes")
        public boolean isPasswordWithinByteLimit() { return newPassword == null || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72; }
    }
    public record Verified(String message, String resetToken) {}
    @PostMapping("/request-otp")
    public VerificationStatusResponse request(@Valid @RequestBody Request request) {
        service.requestOtp(request.email()).exceptionally(EmailPasswordRecoveryService::deliveryFailed);
        return new VerificationStatusResponse(EmailPasswordRecoveryService.REQUEST_MESSAGE);
    }
    @PostMapping("/verify-otp")
    public Verified verify(@Valid @RequestBody Verify request) { return new Verified("Code verified.", service.verify(request.email(), request.code())); }
    @PostMapping("/reset")
    public VerificationStatusResponse reset(@Valid @RequestBody Reset request) {
        return service.reset(request.email(), request.resetToken(), request.newPassword());
    }
}
