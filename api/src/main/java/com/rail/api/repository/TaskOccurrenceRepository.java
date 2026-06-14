package com.rail.api.repository;

import com.rail.api.entity.Goal;
import com.rail.api.entity.OccurrenceStatus;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskOccurrence;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskOccurrenceRepository extends JpaRepository<TaskOccurrence, Long> {

    Optional<TaskOccurrence> findByTaskAndOccurrenceDate(Task task, LocalDate date);

    List<TaskOccurrence> findByTaskInAndOccurrenceDate(List<Task> tasks, LocalDate date);

    List<TaskOccurrence> findByTaskInOrderByOccurrenceDateDesc(List<Task> tasks);

    @Query("""
        SELECT o FROM TaskOccurrence o
        JOIN o.task t
        JOIN t.goal g
        WHERE g = :#{#task.goal} AND o.status = :status
        ORDER BY o.occurrenceDate DESC
    """)
    List<TaskOccurrence> findByGoalAndStatusOrderByDateDesc(Task task, OccurrenceStatus status);

    @Query("""
        SELECT o FROM TaskOccurrence o
        JOIN o.task t
        JOIN t.goal g
        WHERE g.id = :goalId
        ORDER BY o.occurrenceDate DESC
    """)
    List<TaskOccurrence> findByGoalIdOrderByDateDesc(Long goalId);

    @Query("""
        SELECT o FROM TaskOccurrence o
        JOIN o.task t
        WHERE t.goal IN :goals
        AND o.occurrenceDate BETWEEN :startDate AND :endDate
    """)
    List<TaskOccurrence> findByGoalsAndDateRange(
        @Param("goals") Collection<Goal> goals,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
