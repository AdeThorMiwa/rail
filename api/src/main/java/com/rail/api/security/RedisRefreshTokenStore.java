package com.rail.api.security;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String TOKEN_PREFIX = "refresh:token:";
    private static final String USER_SET_PREFIX = "refresh:user:";

    private final StringRedisTemplate redis;

    @Override
    public void save(UUID userPid, String tokenHash, Duration ttl) {
        redis
            .opsForValue()
            .set(TOKEN_PREFIX + tokenHash, userPid.toString(), ttl);
        String userSetKey = USER_SET_PREFIX + userPid;
        redis.opsForSet().add(userSetKey, tokenHash);
        redis.expire(userSetKey, ttl);
    }

    @Override
    public Optional<UUID> findUserPid(String tokenHash) {
        String value = redis.opsForValue().get(TOKEN_PREFIX + tokenHash);
        return Optional.ofNullable(value).map(UUID::fromString);
    }

    @Override
    public void delete(String tokenHash) {
        findUserPid(tokenHash).ifPresent(pid ->
            redis.opsForSet().remove(USER_SET_PREFIX + pid, tokenHash)
        );
        redis.delete(TOKEN_PREFIX + tokenHash);
    }

    @Override
    public void deleteAllForUser(UUID userPid) {
        String userSetKey = USER_SET_PREFIX + userPid;
        Set<String> tokens = redis.opsForSet().members(userSetKey);
        if (tokens != null) {
            tokens.forEach(token -> redis.delete(TOKEN_PREFIX + token));
        }
        redis.delete(userSetKey);
    }
}
