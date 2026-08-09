package com.edurite.background;

import com.edurite.config.InstanceIdentity;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class DistributedJobLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedJobLockService.class);
    private static final String KEY_PREFIX = "edurite:lock:";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final InstanceIdentity instanceIdentity;

    public DistributedJobLockService(StringRedisTemplate redisTemplate, InstanceIdentity instanceIdentity) {
        this.redisTemplate = redisTemplate;
        this.instanceIdentity = instanceIdentity;
    }

    public Optional<LockLease> tryAcquire(String lockName, Duration ttl) {
        String key = key(lockName);
        String token = instanceIdentity.id() + ":" + UUID.randomUUID();
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, safeTtl(ttl));
            if (Boolean.TRUE.equals(acquired)) {
                return Optional.of(new LockLease(key, token));
            }
            log.info("Distributed job lock already held: lockName={}, instance={}", lockName, instanceIdentity.id());
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Distributed job lock unavailable; skipping singleton job: lockName={}, instance={}, message={}",
                    lockName, instanceIdentity.id(), ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean runOnce(String lockName, Duration ttl, Runnable task) {
        Optional<LockLease> lease = tryAcquire(lockName, ttl);
        if (lease.isEmpty()) {
            return false;
        }
        try (LockLease ignored = lease.get()) {
            task.run();
            return true;
        }
    }

    private Duration safeTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return Duration.ofMinutes(10);
        }
        return ttl;
    }

    private String key(String lockName) {
        if (lockName == null || lockName.isBlank()) {
            throw new IllegalArgumentException("lockName must not be blank");
        }
        return KEY_PREFIX + lockName.trim().replaceAll("[^A-Za-z0-9_.:-]", "-");
    }

    public final class LockLease implements AutoCloseable {
        private final String key;
        private final String token;
        private boolean released;

        private LockLease(String key, String token) {
            this.key = key;
            this.token = token;
        }

        @Override
        public void close() {
            if (released) {
                return;
            }
            released = true;
            try {
                redisTemplate.execute(RELEASE_SCRIPT, List.of(key), token);
            } catch (RuntimeException ex) {
                log.warn("Distributed job lock release failed: key={}, instance={}, message={}",
                        key, instanceIdentity.id(), ex.getMessage());
            }
        }
    }
}
