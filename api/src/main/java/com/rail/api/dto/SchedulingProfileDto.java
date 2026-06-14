package com.rail.api.dto;

import com.rail.api.entity.EnergyPattern;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SchedulingProfileDto(
    UUID pid,
    LocalTime deepWorkStart,
    LocalTime deepWorkEnd,
    LocalTime wakeTime,
    LocalTime sleepTime,
    EnergyPattern energyPattern,
    String timezone,
    List<SchedulingDayDto> days
) {}
