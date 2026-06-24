package com.edurite.notification.events;

import java.util.UUID;

public record BursaryDeadlineReminderEvent(UUID bursaryId, String bursaryTitle) {}

