package com.rail.api.dto;

import com.rail.api.entity.GoalType;
import com.rail.api.entity.TaskFlexibility;
import com.rail.api.entity.TaskPriority;
import com.rail.api.entity.TaskStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record TaskDto(
    UUID pid,
    String title,
    String notes,
    TaskStatus status,
    TaskPriority priority,
    TaskFlexibility flexibility,
    Integer durationMinutes,
    UUID milestonePid,
    String goalTitle,
    GoalType goalType,
    boolean hasTaskTarget,
    BigDecimal estimatedValue,
    String targetUnit,
    String missReason
) {}
