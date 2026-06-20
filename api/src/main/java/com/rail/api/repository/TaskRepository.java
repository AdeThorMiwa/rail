package com.rail.api.repository;

import com.rail.api.entity.Goal;
import com.rail.api.entity.Milestone;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskFlexibility;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.User;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findByPid(UUID pid);
    List<Task> findByMilestone(Milestone milestone);
    List<Task> findByGoal(Goal goal);
    List<Task> findByGoalAndStatus(Goal goal, TaskStatus status);

    @Query(
        """
            SELECT t FROM Task t
            JOIN t.goal g
            JOIN g.intention i
            WHERE i.owner = :owner AND t.status = 'PENDING' AND g.status = 'ACTIVE'
            ORDER BY t.priority ASC, g.targetDate ASC NULLS LAST
        """
    )
    List<Task> findPendingTasksForUser(User owner);

    @Query(
        """
            SELECT t FROM Task t
            JOIN t.goal g
            JOIN g.intention i
            WHERE i.owner = :owner
              AND (:goalPid IS NULL OR g.pid = :goalPid)
              AND (:milestonePid IS NULL OR t.milestone.pid = :milestonePid)
            ORDER BY t.priority ASC, t.createdAt ASC
        """
    )
    List<Task> findByUserFiltered(
        @Param("owner") User owner,
        @Param("goalPid") UUID goalPid,
        @Param("milestonePid") UUID milestonePid
    );

    @Query(
        """
            SELECT t FROM Task t
            JOIN t.goal g
            JOIN g.intention i
            WHERE i.owner = :owner AND t.status = 'DONE' AND t.completedAt >= :since
            ORDER BY t.completedAt DESC
        """
    )
    List<Task> findRecentCompletions(User owner, Instant since);

    @Query(
        """
            SELECT t FROM Task t
            JOIN t.goal g
            JOIN g.intention i
            WHERE i.owner = :owner
              AND t.status IN ('SKIPPED', 'MISSED')
              AND t.updatedAt >= :since
            ORDER BY t.updatedAt DESC
        """
    )
    List<Task> findRecentSkipsAndMisses(User owner, Instant since);

    @Query(
        """
            SELECT t FROM Task t
            WHERE t.goal IN :goals
            AND t.status = 'PENDING'
            AND t.flexibility = :flexibility
            AND t.goal.type NOT IN ('HABIT', 'ABSTINENCE')
        """
    )
    List<Task> findCarryOverCandidates(
        @org.springframework.data.repository.query.Param("goals") Collection<Goal> goals,
        @org.springframework.data.repository.query.Param("flexibility") TaskFlexibility flexibility
    );
}
