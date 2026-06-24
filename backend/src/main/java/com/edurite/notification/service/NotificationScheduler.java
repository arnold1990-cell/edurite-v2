package com.edurite.notification.service;

import com.edurite.notification.events.BursaryDeadlineReminderEvent;
import com.edurite.notification.events.CareerInsightUpdateEvent;
import com.edurite.notification.events.NewBursaryPublishedEvent;
import com.edurite.school.portal.entity.SchoolTask;
import com.edurite.school.portal.repository.LearnerEnrollmentRepository;
import com.edurite.school.portal.repository.SchoolTaskRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.service.SchoolAccessService;
import java.time.OffsetDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final SchoolTaskRepository schoolTaskRepository;
    private final LearnerEnrollmentRepository learnerEnrollmentRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final NotificationService notificationService;

    public NotificationScheduler(
            ApplicationEventPublisher applicationEventPublisher,
            SchoolTaskRepository schoolTaskRepository,
            LearnerEnrollmentRepository learnerEnrollmentRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            NotificationService notificationService
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.schoolTaskRepository = schoolTaskRepository;
        this.learnerEnrollmentRepository = learnerEnrollmentRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${edurite.notifications.deadline-reminders.cron:0 0 8 * * *}")
    public void publishDeadlineReminderJob() {
        applicationEventPublisher.publishEvent(new BursaryDeadlineReminderEvent(null, "Upcoming bursary deadlines"));
    }

    @Scheduled(cron = "${edurite.notifications.new-bursary.cron:0 0 9 * * *}")
    public void publishNewBursaryAlertsJob() {
        applicationEventPublisher.publishEvent(new NewBursaryPublishedEvent(null, "New bursary opportunities"));
    }

    @Scheduled(cron = "${edurite.notifications.career-insights.cron:0 0 10 * * MON}")
    public void publishCareerInsightJob() {
        applicationEventPublisher.publishEvent(new CareerInsightUpdateEvent("Weekly Career Insight", "New labour market and skill trend guidance is available."));
    }

    @Scheduled(cron = "${edurite.notifications.school-assignment-deadlines.cron:0 0 7 * * *}")
    public void publishSchoolAssignmentDeadlineReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime nextDay = now.plusHours(24);
        for (SchoolTask task : schoolTaskRepository.findAll()) {
            if (task.getDueAt() == null || task.getDueAt().isBefore(now) || task.getDueAt().isAfter(nextDay)) {
                continue;
            }
            String message = task.getTitle() + " is due by " + task.getDueAt().toLocalDate() + ".";
            learnerEnrollmentRepository.findBySchoolIdAndClassIdAndSubjectIdAndActiveTrue(task.getSchoolId(), task.getClassId(), task.getSubjectId())
                    .forEach(enrollment -> notificationService.createInApp(enrollment.getLearnerUserId(), "ASSIGNMENT_DEADLINE", "Assignment deadline", message));
            schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(task.getSchoolId(), SchoolAccessService.ROLE_SCHOOL_ADMIN).stream()
                    .filter(profile -> profile.isActive() && !profile.isDeleted())
                    .forEach(profile -> notificationService.createInApp(profile.getUserId(), "ASSIGNMENT_DEADLINE", "Assignment deadline", message));
            notificationService.createInApp(task.getTeacherUserId(), "ASSIGNMENT_DEADLINE", "Assignment deadline", message);
        }
    }
}

