package com.rail.api.repository;

import com.rail.api.entity.GoalRecurrence;
import com.rail.api.entity.GoalRecurrenceDay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRecurrenceDayRepository
    extends JpaRepository<GoalRecurrenceDay, Long>
{
    List<GoalRecurrenceDay> findByGoalRecurrence(GoalRecurrence goalRecurrence);
    List<GoalRecurrenceDay> findByGoalRecurrenceIn(List<GoalRecurrence> recurrences);
}
