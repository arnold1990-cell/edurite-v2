package com.edurite.notification.dto;

import com.edurite.notification.entity.NotificationPriority;
import com.edurite.notification.entity.NotificationStatus;
import com.edurite.notification.entity.NotificationTargetAudience;
import com.edurite.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record SendNotificationRequest(
            @NotBlank(message = "title is required") String title,
            @NotBlank(message = "message is required") String message,
            NotificationType type,
            NotificationPriority priority,
            NotificationTargetAudience targetAudience,
            OffsetDateTime expiresAt,
            NotificationFilterRequest filters,
            List<UUID> selectedUserIds
    ) {
    }

    public record NotificationFilterRequest(
            String role,
            String status,
            String subscriptionPlan,
            String grade,
            String schoolId,
            String className,
            String search,
            boolean activeOnly,
            Integer page,
            Integer size
    ) {
    }

    public record NotificationAssignmentDto(
            UUID id,
            UUID notificationId,
            UUID userId,
            boolean isRead,
            OffsetDateTime readAt,
            OffsetDateTime deliveredAt,
            OffsetDateTime createdAt,
            String title,
            String message,
            NotificationType type,
            NotificationPriority priority,
            NotificationStatus status,
            OffsetDateTime notificationCreatedAt,
            OffsetDateTime expiresAt
    ) {
    }

    public record NotificationSummaryDto(
            UUID id,
            String title,
            String message,
            NotificationType type,
            NotificationPriority priority,
            NotificationTargetAudience targetAudience,
            NotificationStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt,
            long recipients
    ) {
    }

    public record UserFilterPreviewDto(long totalMatchedUsers, List<UUID> userIds) {
    }

    public record UnreadCountDto(long unreadCount) {
    }
}

