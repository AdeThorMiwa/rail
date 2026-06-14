package com.rail.api.security;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {
    void save(UUID userPid, String tokenHash, Duration ttl);
    Optional<UUID> findUserPid(String tokenHash);
    void delete(String tokenHash);
    void deleteAllForUser(UUID userPid);
}
