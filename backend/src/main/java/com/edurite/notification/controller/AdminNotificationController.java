package com.edurite.notification.controller;

import com.edurite.notification.dto.NotificationDtos.NotificationFilterRequest;
import com.edurite.notification.dto.NotificationDtos.NotificationSummaryDto;
import com.edurite.notification.dto.NotificationDtos.SendNotificationRequest;
import com.edurite.notification.dto.NotificationDtos.UserFilterPreviewDto;
import com.edurite.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/notifications", "/api/admin/notifications"})
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public NotificationSummaryDto send(Principal principal, @Valid @RequestBody SendNotificationRequest request) {
        return notificationService.sendByAdmin(principal, request);
    }

    @GetMapping
    public Page<NotificationSummaryDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        return notificationService.adminList(page, size, status);
    }

    @GetMapping("/users/filter")
    public UserFilterPreviewDto filterUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String subscriptionPlan,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size
    ) {
        return notificationService.filterUsers(new NotificationFilterRequest(role, status, subscriptionPlan, grade, school, className, search, activeOnly, page, size));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        notificationService.adminDelete(id);
    }
}

