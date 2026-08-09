package com.edurite.background;

public interface BackgroundTaskQueue {
    boolean enqueue(BackgroundTask task);
}
