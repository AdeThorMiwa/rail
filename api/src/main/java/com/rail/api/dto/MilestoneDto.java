package com.rail.api.dto;

import com.rail.api.entity.MilestoneStatus;
import java.time.LocalDate;
import java.util.UUID;

public record MilestoneDto(
    UUID pid,
    String title,
    float position,
    MilestoneStatus status,
    LocalDate targetDate
) {}
