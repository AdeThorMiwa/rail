package com.rail.api.dto;

import java.util.List;

public record HabitStatsDto(
    int currentStreak,
    int bestStreak,
    int thisWeekDone,
    int thisWeekTarget,
    int missedThisWeek,
    double thisWeekRate,
    double allTimeRate,
    List<String> weekDotStatuses,
    List<String> monthDotStatuses,
    int slipsTotal
) {}
