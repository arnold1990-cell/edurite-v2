package com.edurite.account.controller;

// DTOs (data coming from frontend)
import com.edurite.account.dto.ChangePasswordWithOtpRequest;
import com.edurite.account.dto.DeleteAccountRequest;
import com.edurite.account.dto.ForcePasswordChangeRequest;
import com.edurite.auth.dto.AuthResponse;

// Response returned to frontend
import com.edurite.auth.dto.VerificationStatusResponse;

// Service layer (business logic)
import com.edurite.account.service.AccountService;

// Validation annotation
import jakarta.validation.Valid;

// Represents logged-in user
import java.security.Principal;

// Response type
import java.util.Map;

// Spring web annotations
import org.springframework.web.bind.annotation.*;

/**
 * This is a REST Controller.
 *
 * It handles HTTP requests from the frontend.
 *
 * It does NOT contain business logic — it delegates to AccountService.
 */
@RestController
@RequestMapping({"/api/v1/account", "/api/account"})
// Base URL for all endpoints in this controller
public class AccountController {

    private final AccountService accountService;

    /**
     * Constructor injection (Spring automatically injects AccountService)
     */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * DELETE /api/v1/account/me
     *
     * Deletes the currently logged-in user's account
     */
    @DeleteMapping("/me")
    public Map<String, String> deleteMyAccount(
            Principal principal, // contains logged-in user info
            @Valid @RequestBody DeleteAccountRequest request // request body from frontend
    ) {
        return accountService.deleteMyAccount(principal, request);
    }

    /**
     * POST /api/v1/account/password/change/request-otp
     *
     * Step 1: Request OTP for password change
     */
    @PostMapping("/password/change/request-otp")
    public VerificationStatusResponse requestPasswordChangeOtp(
            Principal principal
    ) {
        return accountService.requestPasswordChangeOtp(principal);
    }

    /**
     * POST /api/v1/account/password/change/confirm
     *
     * Step 2: Confirm password change using OTP
     */
    @PostMapping("/password/change/confirm")
    public AuthResponse changePasswordWithOtp(
            Principal principal,
            @Valid @RequestBody ChangePasswordWithOtpRequest request
    ) {
        return accountService.changePasswordWithOtp(principal, request);
    }

    @PostMapping({"/password/change/first-login", "/first-login/change-password"})
    public AuthResponse forcePasswordChange(
            Principal principal,
            @Valid @RequestBody ForcePasswordChangeRequest request
    ) {
        return accountService.forcePasswordChange(principal, request);
    }
}
