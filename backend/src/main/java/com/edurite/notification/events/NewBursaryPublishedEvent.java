package com.edurite.notification.events;

import java.util.UUID;

public record NewBursaryPublishedEvent(UUID bursaryId, String bursaryTitle) {}

