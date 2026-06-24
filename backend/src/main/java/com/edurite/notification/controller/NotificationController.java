package com.edurite.notification.controller;

import com.edurite.notification.dto.NotificationDtos.NotificationAssignmentDto;
import com.edurite.notification.dto.NotificationDtos.UnreadCountDto;
import com.edurite.notification.service.NotificationRealtimeService;
import com.edurite.notification.service.NotificationService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping({"/api/v1/notifications", "/api/notifications"})
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRealtimeService notificationRealtimeService;

    public NotificationController(NotificationService notificationService, NotificationRealtimeService notificationRealtimeService) {
        this.notificationService = notificationService;
        this.notificationRealtimeService = notificationRealtimeService;
    }

    @GetMapping
    public Page<NotificationAssignmentDto> mine(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        return notificationService.mine(principal, page, size, sortBy, direction, status, type);
    }

    @GetMapping("/unread-count")
    public UnreadCountDto unreadCount(Principal principal) {
        return new UnreadCountDto(notificationService.unreadCount(principal));
    }

    @PatchMapping("/{id}/read")
    public NotificationAssignmentDto markRead(Principal principal, @PathVariable UUID id) {
        return notificationService.markRead(principal, id);
    }

    @PatchMapping("/read-all")
    public void markAllRead(Principal principal) {
        notificationService.markAllRead(principal);
    }

    @DeleteMapping("/{id}")
    public void delete(Principal principal, @PathVariable UUID id) {
        notificationService.deleteMine(principal, id);
    }

    @GetMapping("/stream")
    public SseEmitter stream(Principal principal) {
        return notificationRealtimeService.subscribe(notificationService.currentUserId(principal));
    }
}

