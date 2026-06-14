package com.rail.api.dto;

import com.rail.api.entity.DailyScheduleEntryStatus;
import com.rail.api.entity.DailyScheduleEntryType;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleEntryDto(
    UUID pid,
    DailyScheduleEntryType entryType,
    DailyScheduleEntryStatus entryStatus,
    LocalTime startTime,
    LocalTime endTime,
    String notes,
    String skipReason,
    TaskDto task
) {}
