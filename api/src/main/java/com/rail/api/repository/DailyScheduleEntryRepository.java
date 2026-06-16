package com.rail.api.repository;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.Goal;
import com.rail.api.entity.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyScheduleEntryRepository
    extends JpaRepository<DailyScheduleEntry, Long>
{
    Optional<DailyScheduleEntry> findByPid(UUID pid);

    List<DailyScheduleEntry> findByDailyScheduleOrderByStartTime(
        DailySchedule schedule
    );

    @Query("""
        SELECT e FROM DailyScheduleEntry e
        JOIN FETCH e.task t
        JOIN FETCH e.dailySchedule ds
        JOIN FETCH ds.user u
        WHERE e.status = 'PENDING'
          AND e.entryType = 'TASK'
          AND e.endTime <= :now
          AND ds.scheduledDate = :today
          AND u = :user
        """)
    List<DailyScheduleEntry> findPendingTaskEntriesDueForAutoMiss(
        @Param("user") User user,
        @Param("today") LocalDate today,
        @Param("now") LocalTime now
    );

    @Query("""
        SELECT e FROM DailyScheduleEntry e
        JOIN e.task t
        JOIN e.dailySchedule ds
        WHERE ds.user = :user
          AND ds.scheduledDate = :date
          AND t.goal = :goal
          AND e.entryType = 'TASK'
        ORDER BY e.startTime
        """)
    List<DailyScheduleEntry> findTaskEntriesByUserAndDateAndGoal(
        @Param("user") User user,
        @Param("date") LocalDate date,
        @Param("goal") Goal goal
    );
}
