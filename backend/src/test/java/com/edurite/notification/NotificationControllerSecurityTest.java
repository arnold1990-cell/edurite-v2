package com.edurite.notification;

import com.edurite.notification.controller.AdminNotificationController;
import com.edurite.notification.controller.NotificationController;
import com.edurite.notification.dto.NotificationDtos.NotificationAssignmentDto;
import com.edurite.notification.entity.NotificationPriority;
import com.edurite.notification.entity.NotificationStatus;
import com.edurite.notification.entity.NotificationType;
import com.edurite.notification.service.NotificationRealtimeService;
import com.edurite.notification.service.NotificationService;
import com.edurite.security.config.RestAccessDeniedHandler;
import com.edurite.security.config.RestAuthenticationEntryPoint;
import com.edurite.security.config.SecurityConfig;
import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.security.service.CustomUserDetailsService;
import com.edurite.security.service.JwtService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminNotificationController.class, NotificationController.class}, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private NotificationRealtimeService notificationRealtimeService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "student@edurite.com", authorities = "ROLE_STUDENT")
    void normalUserCannotSendAdminNotifications() throws Exception {
        mockMvc.perform(post("/api/admin/notifications/send")
                        .contentType("application/json")
                        .content("{\"title\":\"x\",\"message\":\"y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@edurite.com", authorities = "ROLE_ADMIN")
    void adminCannotSendEmptyNotification() throws Exception {
        mockMvc.perform(post("/api/admin/notifications/send")
                        .contentType("application/json")
                        .content("{\"title\":\"\",\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "student@edurite.com", authorities = "ROLE_STUDENT")
    void studentCanReadAndMarkNotifications() throws Exception {
        NotificationAssignmentDto assignment = new NotificationAssignmentDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                false,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "Title",
                "Message",
                NotificationType.INFO,
                NotificationPriority.NORMAL,
                NotificationStatus.ACTIVE,
                OffsetDateTime.now(),
                null
        );
        when(notificationService.mine(any(), eq(0), eq(20), eq("createdAt"), eq("desc"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(assignment)));
        when(notificationService.markRead(any(), any())).thenReturn(assignment);

        mockMvc.perform(get("/api/notifications")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/notifications/" + assignment.id() + "/read")).andExpect(status().isOk());
    }
}

