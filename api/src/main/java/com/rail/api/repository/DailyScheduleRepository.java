package com.rail.api.repository;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.User;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyScheduleRepository
    extends JpaRepository<DailySchedule, Long>
{
    Optional<DailySchedule> findByUserAndScheduledDate(
        User user,
        LocalDate date
    );
    Optional<DailySchedule> findByPidAndUser(UUID pid, User user);
}
