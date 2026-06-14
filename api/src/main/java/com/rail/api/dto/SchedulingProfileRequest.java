package com.rail.api.dto;

import com.rail.api.entity.EnergyPattern;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record SchedulingProfileRequest(
    @NotNull LocalTime deepWorkStart,
    @NotNull LocalTime deepWorkEnd,
    @NotNull EnergyPattern energyPattern,
    @NotNull LocalTime wakeTime,
    @NotNull LocalTime sleepTime,
    @NotBlank String timezone,
    @NotEmpty @Valid List<SchedulingDayRequest> days
) {}
