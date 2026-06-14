package com.rail.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(
    String id,
    String conversationId,
    String senderId,
    String variant,
    JsonNode blocks,
    Instant createdAt,
    String replyToId,
    String chatEntityType,
    UUID chatEntityId
) {}
