package com.rail.api.dto;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserInfo user,
    boolean onboardingComplete
) {
    public record UserInfo(UUID pid, String email, String displayName) {}
}
