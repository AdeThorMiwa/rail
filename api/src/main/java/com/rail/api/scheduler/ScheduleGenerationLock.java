package com.rail.api.scheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleGenerationLock {

    private static final String PREFIX = "schedule:generating:";

    @Value("${rail.scheduling.stale-generating-minutes:15}")
    private int ttlMinutes;

    private final StringRedisTemplate redis;

    /** Returns true if the lock was acquired (i.e. generation should proceed). */
    public boolean tryAcquire(UUID userPid, LocalDate date) {
        String key = key(userPid, date);
        Boolean set = redis.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(ttlMinutes));
        return Boolean.TRUE.equals(set);
    }

    public boolean isLocked(UUID userPid, LocalDate date) {
        return Boolean.TRUE.equals(redis.hasKey(key(userPid, date)));
    }

    public void release(UUID userPid, LocalDate date) {
        redis.delete(key(userPid, date));
    }

    private String key(UUID userPid, LocalDate date) {
        return PREFIX + userPid + ":" + date;
    }
}
