package com.edurite.background;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class LocalBackgroundTaskQueue implements BackgroundTaskQueue {

    private static final Logger log = LoggerFactory.getLogger(LocalBackgroundTaskQueue.class);

    private final ThreadPoolTaskExecutor executor;
    private final DistributedJobLockService lockService;
    private final Duration taskLockTtl;
    private final Set<String> inFlightKeys = ConcurrentHashMap.newKeySet();

    public LocalBackgroundTaskQueue(
            @Qualifier("applicationTaskExecutor") ThreadPoolTaskExecutor executor,
            DistributedJobLockService lockService,
            MeterRegistry meterRegistry,
            @Value("${edurite.background.task-lock-ttl:30m}") Duration taskLockTtl
    ) {
        this.executor = executor;
        this.lockService = lockService;
        this.taskLockTtl = taskLockTtl;
        Gauge.builder("edurite.background.inflight", inFlightKeys, Set::size)
                .description("Local in-flight background task idempotency keys")
                .register(meterRegistry);
    }

    @Override
    public boolean enqueue(BackgroundTask task) {
        if (task == null || task.work() == null) {
            return false;
        }
        Optional<DistributedJobLockService.LockLease> lease = lockService.tryAcquire("background." + task.idempotencyKey(), taskLockTtl);
        if (lease.isEmpty()) {
            log.info("Duplicate distributed background task ignored: type={}, idempotencyKey={}", task.type(), task.idempotencyKey());
            return false;
        }
        if (!inFlightKeys.add(task.idempotencyKey())) {
            lease.get().close();
            log.info("Duplicate local background task ignored: type={}, idempotencyKey={}", task.type(), task.idempotencyKey());
            return false;
        }
        try {
            executor.execute(() -> runTask(task, lease.get()));
            return true;
        } catch (TaskRejectedException ex) {
            inFlightKeys.remove(task.idempotencyKey());
            lease.get().close();
            log.warn("Local background task rejected: type={}, idempotencyKey={}", task.type(), task.idempotencyKey());
            return false;
        }
    }

    private void runTask(BackgroundTask task, DistributedJobLockService.LockLease lease) {
        try {
            task.work().run();
        } catch (RuntimeException ex) {
            log.error("Local background task failed: type={}, idempotencyKey={}, message={}",
                    task.type(), task.idempotencyKey(), ex.getMessage(), ex);
        } finally {
            inFlightKeys.remove(task.idempotencyKey());
            lease.close();
        }
    }
}
