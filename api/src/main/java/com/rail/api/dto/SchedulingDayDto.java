package com.rail.api.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record SchedulingDayDto(
    UUID pid,
    DayOfWeek dayOfWeek,
    LocalTime preferredWorkStart,
    LocalTime preferredWorkEnd
) {}
