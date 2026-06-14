package com.rail.api.event;

import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.User;
import java.util.UUID;

public record IntentionConfirmedEvent(
    User user,
    String intentionTitle,
    UUID intentionPid,
    UUID goalPid,
    ChatEntityType sourceChatEntityType,
    UUID sourceChatEntityId
) {}
