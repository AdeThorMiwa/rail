package com.rail.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record SchedulingDayRequest(
    @NotNull DayOfWeek dayOfWeek,
    LocalTime preferredWorkStart,
    LocalTime preferredWorkEnd
) {}
