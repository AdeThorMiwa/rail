package com.rail.api.dto;

import com.rail.api.entity.IntentionStatus;
import com.rail.api.entity.IntentionType;
import java.time.Instant;
import java.util.UUID;

public record IntentionDto(
    UUID pid,
    String rawInput,
    String title,
    String completionCriteria,
    IntentionType type,
    IntentionStatus status,
    Instant createdAt,
    GoalDto activeGoal
) {}
