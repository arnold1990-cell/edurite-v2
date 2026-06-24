package com.edurite.auth.controller;

import com.edurite.auth.dto.DevPasswordResetRequest;
import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/auth")
public class DevAuthRepairController {

    private final AuthService authService;

    public DevAuthRepairController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/reset-password")
    public ResponseEntity<VerificationStatusResponse> resetPassword(@Valid @RequestBody DevPasswordResetRequest request) {
        return ResponseEntity.ok(authService.resetPasswordForDev(request));
    }
}

