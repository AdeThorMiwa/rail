package com.rail.api.dto;

import java.util.UUID;

public record SendMessageRequest(String rawInput, UUID replyToId) {}
