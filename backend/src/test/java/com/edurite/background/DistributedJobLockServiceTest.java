package com.edurite.background;

import com.edurite.config.InstanceIdentity;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DistributedJobLockServiceTest {

    @Test
    void acquiresAndReleasesLockWithOwnerToken() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("edurite:lock:notifications.daily"), anyString(), eq(Duration.ofSeconds(30))))
                .thenReturn(true);

        DistributedJobLockService service = new DistributedJobLockService(redisTemplate, new InstanceIdentity("backend-a"));

        Optional<DistributedJobLockService.LockLease> lease = service.tryAcquire("notifications.daily", Duration.ofSeconds(30));

        assertThat(lease).isPresent();
        lease.get().close();
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("edurite:lock:notifications.daily")), anyString());
    }

    @Test
    void doesNotRunSingletonJobWhenLockIsUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("edurite:lock:notifications.daily"), anyString(), eq(Duration.ofSeconds(30))))
                .thenThrow(new RedisConnectionFailureException("down"));

        DistributedJobLockService service = new DistributedJobLockService(redisTemplate, new InstanceIdentity("backend-a"));
        boolean ran = service.runOnce("notifications.daily", Duration.ofSeconds(30), () -> {
            throw new AssertionError("job should not run without the distributed lock");
        });

        assertThat(ran).isFalse();
    }
}
