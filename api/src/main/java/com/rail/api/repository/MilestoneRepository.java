package com.rail.api.repository;

import com.rail.api.entity.Goal;
import com.rail.api.entity.Milestone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByGoalOrderByPosition(Goal goal);
    Optional<Milestone> findByPid(UUID pid);
}
