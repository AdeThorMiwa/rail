package com.rail.api.event;

import com.rail.api.entity.ChatEntityType;
import java.util.UUID;

public record NotificationEvent(
    UUID userPid,
    ChatEntityType entityType,
    UUID entityId,
    String rawText
) {}
