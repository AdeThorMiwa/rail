package com.rail.api.component;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redis;

    @Value("${rail.rate-limit.chat-per-minute}")
    private int chatPerMinute;

    @Value("${rail.rate-limit.recomputes-per-day}")
    private int recomputesPerDay;

    public void checkChat(UUID userPid) {
        String key = "rl:chat:" + userPid;
        long count = increment(key, Duration.ofMinutes(1));
        if (count > chatPerMinute) {
            log.warn("[rate-limit] chat throttled user={} count={}", userPid, count);
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many messages — slow down a little"
            );
        }
    }

    public boolean tryRecompute(UUID userPid) {
        String key = "rl:recompute:" + userPid;
        long count = increment(key, Duration.ofDays(1));
        if (count > recomputesPerDay) {
            log.warn("[rate-limit] recompute skipped user={} count={}", userPid, count);
            return false;
        }
        return true;
    }

    private long increment(String key, Duration ttl) {
        Long count = redis.opsForValue().increment(key);
        if (count == null) count = 1L;
        if (count == 1) {
            redis.expire(key, ttl);
        }
        return count;
    }
}
