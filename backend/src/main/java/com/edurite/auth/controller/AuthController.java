package com.edurite.auth.controller;

import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.CompanyRegisterRequest;
import com.edurite.auth.dto.ForgotPasswordOtpRequest;
import com.edurite.auth.dto.GoogleLoginRequest;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.dto.RegisterRequest;
import com.edurite.auth.dto.ResendVerificationOtpRequest;
import com.edurite.auth.dto.RegistrationResponse;
import com.edurite.auth.dto.ResetPasswordWithOtpRequest;
import com.edurite.auth.dto.SchoolRegisterRequest;
import com.edurite.auth.dto.StudentRegisterRequest;
import com.edurite.auth.dto.VerifyOtpRequest;
import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.auth.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(authService.me(principal));
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/register/student")
    public ResponseEntity<RegistrationResponse> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request));
    }

    @PostMapping("/register/company")
    public ResponseEntity<RegistrationResponse> registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCompany(request));
    }

    @PostMapping("/register/school")
    public ResponseEntity<RegistrationResponse> registerSchool(@Valid @RequestBody SchoolRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerSchool(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(authService.refresh(payload.get("refreshToken")));
    }

    @PostMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("message", "Logout successful");
    }

    @PostMapping("/keep-alive")
    public ResponseEntity<Map<String, String>> keepAlive(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "unauthorized",
                    "message", "Authentication required"
            ));
        }
        return ResponseEntity.ok(authService.keepAlive(principal));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerificationStatusResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyRegistrationOtp(request));
    }

    @PostMapping("/resend-verification-otp")
    public ResponseEntity<VerificationStatusResponse> resendVerificationOtp(
            @Valid @RequestBody ResendVerificationOtpRequest request
    ) {
        return ResponseEntity.ok(authService.resendRegistrationOtp(request));
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<VerificationStatusResponse> forgotPasswordRequestOtp(
            @Valid @RequestBody ForgotPasswordOtpRequest request
    ) {
        return ResponseEntity.ok(authService.requestPasswordResetOtp(request));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<VerificationStatusResponse> resetPasswordWithOtp(
            @Valid @RequestBody ResetPasswordWithOtpRequest request
    ) {
        return ResponseEntity.ok(authService.resetPasswordWithOtp(request));
    }
}

