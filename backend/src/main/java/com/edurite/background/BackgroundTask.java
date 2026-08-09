package com.edurite.background;

import java.time.Instant;
import java.util.UUID;

public record BackgroundTask(
        String idempotencyKey,
        String type,
        Runnable work,
        Instant enqueuedAt
) {
    public static BackgroundTask of(String type, String idempotencyKey, Runnable work) {
        String safeKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString()
                : idempotencyKey.trim();
        return new BackgroundTask(safeKey, type, work, Instant.now());
    }
}
