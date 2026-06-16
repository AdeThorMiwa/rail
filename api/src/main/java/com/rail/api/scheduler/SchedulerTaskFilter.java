package com.rail.api.scheduler;

import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalRecurrence;
import com.rail.api.entity.GoalRecurrenceDay;
import com.rail.api.entity.GoalRecurrenceFrequency;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.MilestoneStatus;
import com.rail.api.entity.Task;
import com.rail.api.repository.GoalRecurrenceDayRepository;
import com.rail.api.repository.GoalRecurrenceRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class SchedulerTaskFilter {

    private final GoalRecurrenceRepository recurrenceRepository;
    private final GoalRecurrenceDayRepository recurrenceDayRepository;

    RecurrenceContext buildRecurrenceContext(
        List<Task> tasks,
        DayOfWeek dayOfWeek
    ) {
        if (tasks.isEmpty()) return new RecurrenceContext(Map.of(), Set.of());

        List<Goal> goals = tasks
            .stream()
            .map(Task::getGoal)
            .distinct()
            .collect(Collectors.toList());

        List<GoalRecurrence> recurrences = recurrenceRepository.findByGoalIn(
            goals
        );

        Set<Long> dailyGoalIds = recurrences
            .stream()
            .filter(r -> r.getFrequency() == GoalRecurrenceFrequency.DAILY)
            .map(r -> r.getGoal().getId())
            .collect(Collectors.toSet());

        List<GoalRecurrence> nonDailyRecurrences = recurrences
            .stream()
            .filter(r -> r.getFrequency() != GoalRecurrenceFrequency.DAILY)
            .collect(Collectors.toList());

        Map<Long, GoalRecurrenceDay> dayMap = new HashMap<>();
        if (!nonDailyRecurrences.isEmpty()) {
            recurrenceDayRepository
                .findByGoalRecurrenceIn(nonDailyRecurrences)
                .stream()
                .filter(rd -> rd.getDayOfWeek() == dayOfWeek)
                .forEach(rd ->
                    dayMap.put(rd.getGoalRecurrence().getGoal().getId(), rd)
                );
        }

        return new RecurrenceContext(dayMap, dailyGoalIds);
    }

    List<Task> filterForDate(
        List<Task> allPending,
        LocalDate date,
        ZoneId userZone,
        RecurrenceContext recurrenceCtx,
        Set<Long> doneRecurringTaskIds
    ) {
        Map<Long, GoalRecurrenceDay> todayRecurrence = recurrenceCtx.dayMap();
        Set<Long> dailyGoalIds = recurrenceCtx.dailyGoalIds();

        return allPending
            .stream()
            .filter(task -> {
                log.info(
                    "filtering task: {} {}",
                    task.getPid(),
                    task.getTitle()
                );

                var today = LocalDate.now(userZone);
                var earliestDate = task.getGoal().getEarliestStartDate();
                var isBeforeEarliestDate =
                    earliestDate == null ? false : today.isBefore(earliestDate);

                if (isBeforeEarliestDate) {
                    log.info("Task is before earliest date");
                    return false;
                }

                GoalType type = task.getGoal().getType();
                boolean recurring =
                    type == GoalType.HABIT || type == GoalType.ABSTINENCE;
                if (!recurring) {
                    // Only schedule tasks from the current (IN_PROGRESS) milestone
                    if (
                        task.getMilestone() != null &&
                        task.getMilestone().getStatus() !=
                            MilestoneStatus.IN_PROGRESS
                    ) {
                        log.info(
                            "Could not add task: milestone not in progress"
                        );
                        return false;
                    }

                    return true;
                }

                if (doneRecurringTaskIds.contains(task.getId())) {
                    log.info("Recurring Task already done");
                    return false;
                }
                // GoalRecurrenceDay match covers WEEKLY/CUSTOM; DAILY has no day rows
                if (
                    todayRecurrence.containsKey(task.getGoal().getId())
                ) return true;
                return dailyGoalIds.contains(task.getGoal().getId());
            })
            .toList();
    }

    record RecurrenceContext(
        Map<Long, GoalRecurrenceDay> dayMap,
        Set<Long> dailyGoalIds
    ) {}
}
