package com.edurite.notification;

import com.edurite.notification.dto.NotificationDtos.NotificationFilterRequest;
import com.edurite.notification.dto.NotificationDtos.SendNotificationRequest;
import com.edurite.notification.entity.NotificationPriority;
import com.edurite.notification.entity.NotificationTargetAudience;
import com.edurite.notification.entity.NotificationType;
import com.edurite.notification.entity.UserNotification;
import com.edurite.notification.repository.NotificationAdminRepository;
import com.edurite.notification.repository.UserNotificationRepository;
import com.edurite.notification.service.NotificationRealtimeService;
import com.edurite.notification.service.NotificationService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationServiceTest {

    @Test
    void sendNotificationToFilteredUsersCreatesAssignments() {
        NotificationAdminRepository notificationAdminRepository = mock(NotificationAdminRepository.class);
        UserNotificationRepository userNotificationRepository = mock(UserNotificationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        NotificationRealtimeService notificationRealtimeService = mock(NotificationRealtimeService.class);

        NotificationService service = new NotificationService(notificationAdminRepository, userNotificationRepository, userRepository, currentUserService, studentProfileRepository, notificationRealtimeService);

        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setStatus(UserStatus.ACTIVE);

        when(currentUserService.requireUser(any(Principal.class))).thenReturn(admin);
        when(notificationAdminRepository.save(any())).thenAnswer(invocation -> {
            var n = invocation.getArgument(0, com.edurite.notification.entity.Notification.class);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(userRepository.findUserIdsByNotificationFilter(any(), any(), any(), any(), any(), any(), anyBoolean(), any(PageRequest.class)))
                .thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));

        SendNotificationRequest request = new SendNotificationRequest(
                "Maintenance notice",
                "System updates",
                NotificationType.SYSTEM,
                NotificationPriority.HIGH,
                NotificationTargetAudience.FILTERED,
                null,
                new NotificationFilterRequest("ROLE_STUDENT", "ACTIVE", "PREMIUM", null, null, null, null, true, 0, 50),
                null
        );

        service.sendByAdmin(() -> "admin@edurite.com", request);

        verify(userNotificationRepository).saveAll(argThat(list -> {
            int count = 0;
            for (Object ignored : list) {
                count++;
            }
            return count == 2;
        }));
    }

    @Test
    void markReadUpdatesUserNotification() {
        NotificationAdminRepository notificationAdminRepository = mock(NotificationAdminRepository.class);
        UserNotificationRepository userNotificationRepository = mock(UserNotificationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        NotificationRealtimeService notificationRealtimeService = mock(NotificationRealtimeService.class);

        NotificationService service = new NotificationService(notificationAdminRepository, userNotificationRepository, userRepository, currentUserService, studentProfileRepository, notificationRealtimeService);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);

        var notification = new com.edurite.notification.entity.Notification();
        notification.setId(UUID.randomUUID());
        notification.setTitle("Title");
        notification.setMessage("Message");

        UserNotification assignment = new UserNotification();
        assignment.setId(UUID.randomUUID());
        assignment.setUserId(user.getId());
        assignment.setNotification(notification);
        assignment.setRead(false);

        when(currentUserService.requireUser(any(Principal.class))).thenReturn(user);
        when(userNotificationRepository.findByIdAndUserId(assignment.getId(), user.getId())).thenReturn(java.util.Optional.of(assignment));
        when(userNotificationRepository.save(any(UserNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markRead(() -> "student@edurite.com", assignment.getId());

        verify(userNotificationRepository).save(argThat(saved -> saved.isRead() && saved.getReadAt() != null));
    }

    @Test
    void sendNotificationAllUsersUsesActiveUsersOnly() {
        NotificationAdminRepository notificationAdminRepository = mock(NotificationAdminRepository.class);
        UserNotificationRepository userNotificationRepository = mock(UserNotificationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        NotificationRealtimeService notificationRealtimeService = mock(NotificationRealtimeService.class);

        NotificationService service = new NotificationService(notificationAdminRepository, userNotificationRepository, userRepository, currentUserService, studentProfileRepository, notificationRealtimeService);

        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setStatus(UserStatus.ACTIVE);
        User activeUser = new User();
        activeUser.setId(UUID.randomUUID());
        activeUser.setStatus(UserStatus.ACTIVE);

        when(currentUserService.requireUser(any(Principal.class))).thenReturn(admin);
        when(notificationAdminRepository.save(any())).thenAnswer(invocation -> {
            var n = invocation.getArgument(0, com.edurite.notification.entity.Notification.class);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(userRepository.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE)).thenReturn(List.of(activeUser));

        SendNotificationRequest request = new SendNotificationRequest(
                "System notice",
                "Message",
                NotificationType.SYSTEM,
                NotificationPriority.NORMAL,
                NotificationTargetAudience.ALL,
                null,
                null,
                null
        );

        service.sendByAdmin(() -> "admin@edurite.com", request);

        verify(userNotificationRepository).saveAll(argThat(list -> {
            int count = 0;
            for (Object ignored : list) {
                count++;
            }
            return count == 1;
        }));
    }

    @Test
    void sendNotificationThrowsWhenNoRecipientsMatched() {
        NotificationAdminRepository notificationAdminRepository = mock(NotificationAdminRepository.class);
        UserNotificationRepository userNotificationRepository = mock(UserNotificationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        NotificationRealtimeService notificationRealtimeService = mock(NotificationRealtimeService.class);

        NotificationService service = new NotificationService(notificationAdminRepository, userNotificationRepository, userRepository, currentUserService, studentProfileRepository, notificationRealtimeService);

        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setStatus(UserStatus.ACTIVE);

        when(currentUserService.requireUser(any(Principal.class))).thenReturn(admin);
        when(notificationAdminRepository.save(any())).thenAnswer(invocation -> {
            var n = invocation.getArgument(0, com.edurite.notification.entity.Notification.class);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(userRepository.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE)).thenReturn(List.of());

        SendNotificationRequest request = new SendNotificationRequest(
                "System notice",
                "Message",
                NotificationType.SYSTEM,
                NotificationPriority.NORMAL,
                NotificationTargetAudience.ALL,
                null,
                null,
                null
        );

        assertThrows(ResponseStatusException.class, () -> service.sendByAdmin(() -> "admin@edurite.com", request));
    }
}

