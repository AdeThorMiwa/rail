package com.rail.api.dto;

import com.rail.api.entity.EnergyLevel;
import com.rail.api.entity.GoalStatus;
import com.rail.api.entity.GoalType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoalDto(
    UUID pid,
    String title,
    GoalType type,
    GoalStatus status,
    EnergyLevel energyLevel,
    long estimatedTotalHours,
    long actualTotalHours,
    LocalDate targetDate,
    String intentionTitle,
    GoalTargetDto target,
    HabitStatsDto habitStats,
    List<MilestoneDto> milestones,
    List<TaskDto> tasks
) {}
