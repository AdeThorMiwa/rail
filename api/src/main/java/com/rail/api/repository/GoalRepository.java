package com.rail.api.repository;

import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalStatus;
import com.rail.api.entity.Intention;
import com.rail.api.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    Optional<Goal> findByIntentionAndStatus(
        Intention intention,
        GoalStatus status
    );

    @Query(
        """
            SELECT g FROM Goal g
            JOIN g.intention i
            WHERE i.owner = :owner AND g.status = :status
            ORDER BY g.createdAt DESC
        """
    )
    List<Goal> findByOwnerAndStatus(User owner, GoalStatus status);

    @Query(
        "SELECT g FROM Goal g WHERE g.pid = :pid AND g.intention.owner = :owner"
    )
    Optional<Goal> findByPidAndOwner(
        @Param("pid") UUID pid,
        @Param("owner") User owner
    );

    Optional<Goal> findByPid(UUID pid);

    List<Goal> findByIntentionAndStatusOrderByCreatedAtAsc(Intention intention, GoalStatus status);
}
