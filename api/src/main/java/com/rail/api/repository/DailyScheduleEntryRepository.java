package com.rail.api.repository;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.Goal;
import com.rail.api.entity.User;
import java.time.LocalDate;
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
          AND ds.scheduledDate < :today
          AND u = :user
        """)
    List<DailyScheduleEntry> findPendingTaskEntriesDueForAutoMiss(
        @Param("user") User user,
        @Param("today") LocalDate today
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

    @Query("""
        SELECT e FROM DailyScheduleEntry e
        JOIN FETCH e.task t
        JOIN FETCH t.goal g
        JOIN FETCH e.dailySchedule ds
        WHERE ds.user = :user
          AND ds.scheduledDate >= :since
          AND e.entryType = 'TASK'
        ORDER BY ds.scheduledDate ASC, e.startTime ASC
    """)
    List<DailyScheduleEntry> findTaskEntriesByUserSince(
        @Param("user") User user,
        @Param("since") LocalDate since
    );

    @Query("""
        SELECT e.task.id, COUNT(e)
        FROM DailyScheduleEntry e
        JOIN e.dailySchedule ds
        WHERE e.task.id IN :taskIds
          AND ds.scheduledDate >= :weekStart
          AND ds.scheduledDate < :today
        GROUP BY e.task.id
    """)
    List<Object[]> countWeeklyAppearancesByTaskIds(
        @Param("taskIds") List<Long> taskIds,
        @Param("weekStart") LocalDate weekStart,
        @Param("today") LocalDate today
    );
}
