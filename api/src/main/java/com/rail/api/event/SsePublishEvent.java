package com.rail.api.event;

import java.util.UUID;

public record SsePublishEvent(UUID userPid, String eventType, Object payload) {}
