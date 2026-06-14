package com.rail.api.dto;

import com.rail.api.entity.DailyScheduleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyScheduleDto(
    UUID pid,
    LocalDate scheduledDate,
    DailyScheduleStatus status,
    String railNotes,
    List<ScheduleEntryDto> entries
) {}
