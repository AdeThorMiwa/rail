package com.rail.api.repository;

import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalRecurrence;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRecurrenceRepository
    extends JpaRepository<GoalRecurrence, Long>
{
    Optional<GoalRecurrence> findByGoal(Goal goal);
    List<GoalRecurrence> findByGoalIn(List<Goal> goals);
}
