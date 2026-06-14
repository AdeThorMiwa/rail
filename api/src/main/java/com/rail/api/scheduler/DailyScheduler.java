package com.rail.api.scheduler;

import com.rail.api.dto.DailyScheduleDto;
import com.rail.api.entity.User;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyScheduler {
    Optional<DailyScheduleDto> getToday(User user);
    DailyScheduleDto recompute(User user, LocalDate date);
}
