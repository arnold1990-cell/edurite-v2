package com.edurite.curriculum.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CurriculumNotificationScheduler {

    private final CurriculumService curriculumService;

    public CurriculumNotificationScheduler(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    @Scheduled(cron = "${edurite.curriculum.reminders.cron:0 0 6 * * *}")
    public void dispatchReminders() {
        curriculumService.dispatchScheduledRemindersForToday();
    }

    @Scheduled(cron = "${edurite.curriculum.risk-alerts.cron:0 0 12 * * *}")
    public void dispatchRiskAlerts() {
        curriculumService.evaluateCurriculumRiskAlerts();
    }
}
