package com.rail.api.repository;

import com.rail.api.entity.Goal;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.TaskTarget;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskTargetRepository extends JpaRepository<TaskTarget, Long> {
    Optional<TaskTarget> findByTask(Task task);

    @Query(
        """
        SELECT COALESCE(SUM(tt.actualValue), 0)
        FROM TaskTarget tt
        WHERE tt.task.goal = :goal
          AND tt.task.status = :status
          AND tt.actualValue IS NOT NULL
        """
    )
    BigDecimal sumActualValueByGoalAndTaskStatus(
        @Param("goal") Goal goal,
        @Param("status") TaskStatus status
    );
}
