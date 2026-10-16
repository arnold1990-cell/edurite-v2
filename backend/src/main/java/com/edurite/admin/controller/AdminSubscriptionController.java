package com.edurite.admin.controller;

import com.edurite.subscription.entity.AdminSubscriptionChange;
import com.edurite.subscription.service.AdminSubscriptionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/admin/subscriptions", "/api/v1/admin/subscriptions"})
public class AdminSubscriptionController {
    private final AdminSubscriptionService service;
    public AdminSubscriptionController(AdminSubscriptionService service) { this.service = service; }

    @GetMapping
    public Page<AdminSubscriptionService.Summary> list(Principal principal, @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return service.list(principal, search, page, size);
    }

    @GetMapping("/{userId}")
    public AdminSubscriptionService.Summary detail(Principal principal, @PathVariable UUID userId) {
        return service.detail(principal, userId);
    }

    @GetMapping("/{userId}/history")
    public Page<AdminSubscriptionChange> history(Principal principal, @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return service.history(principal, userId, page, size);
    }

    @PostMapping("/{userId}/override")
    public AdminSubscriptionService.Summary change(Principal principal, @PathVariable UUID userId,
            @Valid @RequestBody AdminSubscriptionService.ChangeRequest request) {
        return service.change(principal, userId, request);
    }
}
