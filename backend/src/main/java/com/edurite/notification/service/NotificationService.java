package com.edurite.notification.service;

import com.edurite.notification.dto.NotificationDtos.NotificationAssignmentDto;
import com.edurite.notification.dto.NotificationDtos.NotificationFilterRequest;
import com.edurite.notification.dto.NotificationDtos.NotificationSummaryDto;
import com.edurite.notification.dto.NotificationDtos.SendNotificationRequest;
import com.edurite.notification.dto.NotificationDtos.UserFilterPreviewDto;
import com.edurite.notification.entity.Notification;
import com.edurite.notification.entity.NotificationPriority;
import com.edurite.notification.entity.NotificationStatus;
import com.edurite.notification.entity.NotificationTargetAudience;
import com.edurite.notification.entity.NotificationType;
import com.edurite.notification.entity.UserNotification;
import com.edurite.notification.events.BursaryDeadlineReminderEvent;
import com.edurite.notification.events.CareerInsightUpdateEvent;
import com.edurite.notification.events.NewBursaryPublishedEvent;
import com.edurite.notification.repository.NotificationAdminRepository;
import com.edurite.notification.repository.UserNotificationRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationAdminRepository notificationAdminRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final NotificationRealtimeService notificationRealtimeService;

    public NotificationService(
            NotificationAdminRepository notificationAdminRepository,
            UserNotificationRepository userNotificationRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository,
            NotificationRealtimeService notificationRealtimeService
    ) {
        this.notificationAdminRepository = notificationAdminRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
        this.notificationRealtimeService = notificationRealtimeService;
    }

    @Transactional(readOnly = true)
    public UUID currentUserId(Principal principal) {
        return currentUserService.requireUser(principal).getId();
    }

    @Transactional(readOnly = true)
    public Page<NotificationAssignmentDto> mine(Principal principal, int page, int size, String sortBy, String direction, String status, String type) {
        User user = currentUserService.requireUser(principal);
        String sortProperty = resolveMineSortProperty(sortBy);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(sortDirection, sortProperty));

        Page<UserNotification> raw = userNotificationRepository.findByUserId(user.getId(), pageable);
        List<NotificationAssignmentDto> filtered = raw.getContent().stream()
                .map(this::toAssignmentDto)
                .filter(dto -> {
                    if (status != null && !status.isBlank()) {
                        boolean wantRead = "READ".equalsIgnoreCase(status);
                        if (dto.isRead() != wantRead) {
                            return false;
                        }
                    }
                    return type == null || type.isBlank() || dto.type().name().equalsIgnoreCase(type);
                })
                .toList();
        return new PageImpl<>(filtered, pageable, raw.getTotalElements());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return userNotificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public NotificationAssignmentDto markRead(Principal principal, UUID userNotificationId) {
        User user = currentUserService.requireUser(principal);
        UserNotification assignment = userNotificationRepository.findByIdAndUserId(userNotificationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        assignment.setRead(true);
        assignment.setReadAt(OffsetDateTime.now());
        NotificationAssignmentDto dto = toAssignmentDto(userNotificationRepository.save(assignment));
        notificationRealtimeService.sendEvent(user.getId(), "notifications-updated", "read");
        return dto;
    }

    @Transactional
    public void markAllRead(Principal principal) {
        User user = currentUserService.requireUser(principal);
        List<UserNotification> unread = userNotificationRepository.findByUserIdAndIsReadFalse(user.getId());
        OffsetDateTime now = OffsetDateTime.now();
        unread.forEach(item -> {
            item.setRead(true);
            item.setReadAt(now);
        });
        userNotificationRepository.saveAll(unread);
        notificationRealtimeService.sendEvent(user.getId(), "notifications-updated", "read-all");
    }

    @Transactional
    public void deleteMine(Principal principal, UUID userNotificationId) {
        User user = currentUserService.requireUser(principal);
        userNotificationRepository.deleteByIdAndUserId(userNotificationId, user.getId());
        notificationRealtimeService.sendEvent(user.getId(), "notifications-updated", "deleted");
    }

    @Transactional
    public NotificationAssignmentDto createInApp(UUID userId, String eventType, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setChannel("IN_APP");
        notification.setEventType(eventType == null || eventType.isBlank() ? "SYSTEM" : eventType.trim().toUpperCase());
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(resolveType(eventType));
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setTargetAudience(NotificationTargetAudience.SELECTED);
        notification.setStatus(NotificationStatus.ACTIVE);
        notification.setSentAt(OffsetDateTime.now());
        Notification saved = notificationAdminRepository.save(notification);

        UserNotification assignment = new UserNotification();
        assignment.setNotification(saved);
        assignment.setUserId(userId);
        assignment.setRead(false);
        assignment.setDeliveredAt(OffsetDateTime.now());
        NotificationAssignmentDto dto = toAssignmentDto(userNotificationRepository.save(assignment));
        notificationRealtimeService.sendEvent(userId, "notifications-updated", "created");
        return dto;
    }

    @Transactional
    public NotificationSummaryDto sendByAdmin(Principal principal, SendNotificationRequest request) {
        User admin = currentUserService.requireUser(principal);
        NotificationTargetAudience target = request.targetAudience() == null ? NotificationTargetAudience.ALL : request.targetAudience();

        Notification notification = new Notification();
        notification.setUserId(null);
        notification.setChannel("IN_APP");
        notification.setEventType((request.type() == null ? NotificationType.SYSTEM : request.type()).name());
        notification.setTitle(request.title().trim());
        notification.setMessage(request.message().trim());
        notification.setType(request.type() == null ? NotificationType.SYSTEM : request.type());
        notification.setPriority(request.priority() == null ? NotificationPriority.NORMAL : request.priority());
        notification.setTargetAudience(target);
        notification.setCreatedByAdminId(admin.getId());
        notification.setExpiresAt(request.expiresAt());
        notification.setStatus(NotificationStatus.ACTIVE);
        notification.setSentAt(OffsetDateTime.now());

        Notification saved = notificationAdminRepository.save(notification);
        List<UUID> targetUserIds = resolveTargetUsers(target, request.filters(), request.selectedUserIds());
        if (targetUserIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No users matched the selected audience.");
        }
        createAssignments(saved, targetUserIds);

        return toSummaryDto(saved, targetUserIds.size());
    }

    @Transactional(readOnly = true)
    public Page<NotificationSummaryDto> adminList(int page, int size, String status) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        if (status != null && !status.isBlank()) {
            NotificationStatus s = NotificationStatus.valueOf(status.toUpperCase());
            return notificationAdminRepository.findByStatus(s, pageable)
                    .map(item -> toSummaryDto(item, userNotificationRepository.countByNotificationId(item.getId())));
        }
        return notificationAdminRepository.findAll(pageable)
                .map(item -> toSummaryDto(item, userNotificationRepository.countByNotificationId(item.getId())));
    }

    @Transactional(readOnly = true)
    public UserFilterPreviewDto filterUsers(NotificationFilterRequest request) {
        List<UUID> ids = findUserIdsByFilter(request, request.page() == null ? 0 : request.page(), request.size() == null ? 50 : request.size());
        long count = countUsersByFilter(request);
        return new UserFilterPreviewDto(count, ids);
    }

    @Transactional
    public void adminDelete(UUID notificationId) {
        userNotificationRepository.deleteByNotificationId(notificationId);
        notificationAdminRepository.deleteById(notificationId);
    }

    private List<UUID> resolveTargetUsers(NotificationTargetAudience target, NotificationFilterRequest filters, List<UUID> selectedUserIds) {
        return switch (target) {
            case ALL -> userRepository.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE).stream()
                    .map(User::getId)
                    .toList();
            case FILTERED -> findUserIdsByFilter(filters, 0, 5000);
            case SELECTED -> selectedUserIds == null ? List.of() : selectedUserIds;
        };
    }

    private void createAssignments(Notification notification, List<UUID> userIds) {
        OffsetDateTime deliveredAt = OffsetDateTime.now();
        List<UserNotification> assignments = new ArrayList<>();
        for (UUID userId : userIds) {
            UserNotification assignment = new UserNotification();
            assignment.setNotification(notification);
            assignment.setUserId(userId);
            assignment.setRead(false);
            assignment.setDeliveredAt(deliveredAt);
            assignments.add(assignment);
        }
        userNotificationRepository.saveAll(assignments);
    }

    private List<UUID> findUserIdsByFilter(NotificationFilterRequest request, int page, int size) {
        NotificationFilterRequest criteria = request == null ? new NotificationFilterRequest(null, null, null, null, null, null, null, true, page, size) : request;
        UUID schoolId = null;
        if (criteria.schoolId() != null && !criteria.schoolId().isBlank()) {
            schoolId = UUID.fromString(criteria.schoolId());
        }
        return userRepository.findUserIdsByNotificationFilter(
                criteria.role(),
                criteria.status(),
                criteria.subscriptionPlan(),
                criteria.grade(),
                schoolId,
                criteria.search(),
                criteria.activeOnly(),
                PageRequest.of(Math.max(0, page), Math.max(1, size))
        );
    }

    private long countUsersByFilter(NotificationFilterRequest request) {
        NotificationFilterRequest criteria = request == null ? new NotificationFilterRequest(null, null, null, null, null, null, null, true, 0, 50) : request;
        UUID schoolId = null;
        if (criteria.schoolId() != null && !criteria.schoolId().isBlank()) {
            schoolId = UUID.fromString(criteria.schoolId());
        }
        return userRepository.countByNotificationFilter(criteria.role(), criteria.status(), criteria.subscriptionPlan(), criteria.grade(), schoolId, criteria.search(), criteria.activeOnly());
    }

    private NotificationType resolveType(String eventType) {
        if (eventType == null) {
            return NotificationType.SYSTEM;
        }
        if (eventType.contains("DEADLINE")) {
            return NotificationType.WARNING;
        }
        if (eventType.contains("PAY")) {
            return NotificationType.PAYMENT;
        }
        if (eventType.contains("BURSARY") || eventType.contains("CAREER")) {
            return NotificationType.ANNOUNCEMENT;
        }
        return NotificationType.SYSTEM;
    }

    private String resolveMineSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "createdat" -> "createdAt";
            case "title" -> "notification.title";
            case "priority" -> "notification.priority";
            case "type" -> "notification.type";
            case "status" -> "notification.status";
            default -> "createdAt";
        };
    }

    private NotificationAssignmentDto toAssignmentDto(UserNotification item) {
        Notification notification = item.getNotification();
        return new NotificationAssignmentDto(
                item.getId(),
                notification.getId(),
                item.getUserId(),
                item.isRead(),
                item.getReadAt(),
                item.getDeliveredAt(),
                item.getCreatedAt(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getPriority(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getExpiresAt()
        );
    }

    private NotificationSummaryDto toSummaryDto(Notification item, long recipients) {
        return new NotificationSummaryDto(
                item.getId(),
                item.getTitle(),
                item.getMessage(),
                item.getType(),
                item.getPriority(),
                item.getTargetAudience(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getExpiresAt(),
                recipients
        );
    }

    @EventListener
    public void onNewBursaryPublished(NewBursaryPublishedEvent event) {
        notifyAllStudents("BURSARY_ALERT", "New bursary alert", event.bursaryTitle() == null ? "A new bursary has been published." : event.bursaryTitle());
    }

    @EventListener
    public void onBursaryDeadlineReminder(BursaryDeadlineReminderEvent event) {
        notifyAllStudents("DEADLINE_REMINDER", "Application deadline reminder", event.bursaryTitle() == null ? "A bursary deadline is approaching." : event.bursaryTitle());
    }

    @EventListener
    public void onCareerInsightUpdate(CareerInsightUpdateEvent event) {
        notifyAllStudents("CAREER_INSIGHT", event.title(), event.summary());
    }

    private void notifyAllStudents(String eventType, String title, String message) {
        studentProfileRepository.findAll().forEach(profile -> {
            if (profile.isInAppNotificationsEnabled()) {
                createInApp(profile.getUserId(), eventType, title, message);
            }
        });
    }
}

