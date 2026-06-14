package com.rail.api.repository;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.ScheduleChange;
import com.rail.api.entity.User;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduleChangeRepository extends JpaRepository<ScheduleChange, Long> {

    List<ScheduleChange> findByDailyScheduleOrderByHappenedAtDesc(DailySchedule dailySchedule);

    @Query("""
        SELECT sc FROM ScheduleChange sc
        WHERE sc.dailySchedule.user = :user AND sc.happenedAt >= :since
        ORDER BY sc.happenedAt DESC
    """)
    List<ScheduleChange> findRecentByUser(User user, Instant since);
}
