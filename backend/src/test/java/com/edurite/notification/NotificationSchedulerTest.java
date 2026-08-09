package com.edurite.notification;

import com.edurite.background.DistributedJobLockService;
import com.edurite.notification.events.BursaryDeadlineReminderEvent;
import com.edurite.notification.service.NotificationScheduler;
import com.edurite.notification.service.NotificationService;
import com.edurite.school.portal.repository.LearnerEnrollmentRepository;
import com.edurite.school.portal.repository.SchoolTaskRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationSchedulerTest {

    @Test
    void deadlineReminderJobPublishesOnlyInsideDistributedLock() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        DistributedJobLockService lockService = mock(DistributedJobLockService.class);
        NotificationScheduler scheduler = new NotificationScheduler(
                publisher,
                mock(SchoolTaskRepository.class),
                mock(LearnerEnrollmentRepository.class),
                mock(SchoolUserProfileRepository.class),
                mock(NotificationService.class),
                lockService
        );

        scheduler.publishDeadlineReminderJob();

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(lockService).runOnce(eq("notifications.deadline-reminders"), eq(Duration.ofMinutes(30)), taskCaptor.capture());
        taskCaptor.getValue().run();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(BursaryDeadlineReminderEvent.class);
    }
}
