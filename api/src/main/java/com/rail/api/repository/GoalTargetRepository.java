package com.rail.api.repository;

import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalTarget;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalTargetRepository extends JpaRepository<GoalTarget, Long> {
    Optional<GoalTarget> findByGoal(Goal goal);
}
