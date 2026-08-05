package com.edurite.curriculum.service;

import com.edurite.background.DistributedJobLockService;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CurriculumNotificationScheduler {

    private final CurriculumService curriculumService;
    private final DistributedJobLockService jobLockService;

    public CurriculumNotificationScheduler(CurriculumService curriculumService, DistributedJobLockService jobLockService) {
        this.curriculumService = curriculumService;
        this.jobLockService = jobLockService;
    }

    @Scheduled(cron = "${edurite.curriculum.reminders.cron:0 0 6 * * *}")
    public void dispatchReminders() {
        jobLockService.runOnce("curriculum.scheduled-reminders", Duration.ofMinutes(30),
                curriculumService::dispatchScheduledRemindersForToday);
    }

    @Scheduled(cron = "${edurite.curriculum.risk-alerts.cron:0 0 12 * * *}")
    public void dispatchRiskAlerts() {
        jobLockService.runOnce("curriculum.risk-alerts", Duration.ofMinutes(30),
                curriculumService::evaluateCurriculumRiskAlerts);
    }
}
