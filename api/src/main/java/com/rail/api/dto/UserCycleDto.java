package com.rail.api.dto;

import com.rail.api.entity.CycleStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record UserCycleDto(
    UUID pid,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime reviewTime,
    CycleStatus status
) {}
