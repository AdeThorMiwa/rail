package com.rail.api.scheduler;

import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalRecurrence;
import com.rail.api.entity.GoalRecurrenceDay;
import com.rail.api.entity.GoalRecurrenceFrequency;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.MilestoneStatus;
import com.rail.api.entity.Task;
import com.rail.api.repository.DailyScheduleEntryRepository;
import com.rail.api.repository.GoalRecurrenceDayRepository;
import com.rail.api.repository.GoalRecurrenceRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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
    private final DailyScheduleEntryRepository entryRepository;

    RecurrenceContext buildRecurrenceContext(List<Task> tasks, DayOfWeek dayOfWeek, LocalDate today) {
        if (tasks.isEmpty()) return new RecurrenceContext(Map.of(), Set.of(), Map.of(), Map.of());

        List<Goal> goals = tasks.stream().map(Task::getGoal).distinct().collect(Collectors.toList());
        List<GoalRecurrence> recurrences = recurrenceRepository.findByGoalIn(goals);

        Set<Long> dailyGoalIds = recurrences.stream()
            .filter(r -> r.getFrequency() == GoalRecurrenceFrequency.DAILY)
            .map(r -> r.getGoal().getId())
            .collect(Collectors.toSet());

        List<GoalRecurrence> nonDailyRecurrences = recurrences.stream()
            .filter(r -> r.getFrequency() != GoalRecurrenceFrequency.DAILY)
            .collect(Collectors.toList());

        // Map goalId → recurrence day row for today's day-of-week (for explicit-day habits)
        Map<Long, GoalRecurrenceDay> todayDayMap = new HashMap<>();
        // Map goalId → recurrence (for dynamic-day habits — no day rows)
        Map<Long, GoalRecurrence> dynamicRecurrenceMap = new HashMap<>();

        if (!nonDailyRecurrences.isEmpty()) {
            Map<Long, List<GoalRecurrenceDay>> daysByRecurrenceId = recurrenceDayRepository
                .findByGoalRecurrenceIn(nonDailyRecurrences)
                .stream()
                .collect(Collectors.groupingBy(rd -> rd.getGoalRecurrence().getId()));

            for (GoalRecurrence rec : nonDailyRecurrences) {
                List<GoalRecurrenceDay> days = daysByRecurrenceId.getOrDefault(rec.getId(), List.of());
                if (days.isEmpty()) {
                    // No preferred days → dynamic scheduling
                    dynamicRecurrenceMap.put(rec.getGoal().getId(), rec);
                } else {
                    // Has explicit days → check if today matches
                    days.stream()
                        .filter(rd -> rd.getDayOfWeek() == dayOfWeek)
                        .findFirst()
                        .ifPresent(rd -> todayDayMap.put(rec.getGoal().getId(), rd));
                }
            }
        }

        // For dynamic habits: load how many times each task already appeared this week (before today)
        Map<Long, Integer> weeklyCountByTaskId = Map.of();
        if (!dynamicRecurrenceMap.isEmpty()) {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            List<Long> dynamicTaskIds = tasks.stream()
                .filter(t -> dynamicRecurrenceMap.containsKey(t.getGoal().getId()))
                .map(Task::getId)
                .collect(Collectors.toList());

            if (!dynamicTaskIds.isEmpty()) {
                weeklyCountByTaskId = entryRepository
                    .countWeeklyAppearancesByTaskIds(dynamicTaskIds, weekStart, today)
                    .stream()
                    .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                    ));
            }
        }

        return new RecurrenceContext(todayDayMap, dailyGoalIds, dynamicRecurrenceMap, weeklyCountByTaskId);
    }

    List<Task> filterForDate(
        List<Task> allPending,
        LocalDate date,
        ZoneId userZone,
        RecurrenceContext recurrenceCtx,
        Set<Long> doneRecurringTaskIds
    ) {
        return allPending.stream()
            .filter(task -> {
                log.info("filtering task: {} {}", task.getPid(), task.getTitle());

                LocalDate today = LocalDate.now(userZone);
                LocalDate earliestDate = task.getGoal().getEarliestStartDate();
                if (earliestDate != null && today.isBefore(earliestDate)) {
                    log.info("Task is before earliest date");
                    return false;
                }

                GoalType type = task.getGoal().getType();
                boolean recurring = type == GoalType.HABIT || type == GoalType.ABSTINENCE;

                if (!recurring) {
                    if (task.getMilestone() != null &&
                        task.getMilestone().getStatus() != MilestoneStatus.IN_PROGRESS) {
                        log.info("Could not add task: milestone not in progress");
                        return false;
                    }
                    return true;
                }

                if (doneRecurringTaskIds.contains(task.getId())) {
                    log.info("Recurring task already done today");
                    return false;
                }

                // Path 1: daily habit
                if (recurrenceCtx.dailyGoalIds().contains(task.getGoal().getId())) return true;

                // Path 2: explicit preferred days — check if today matches
                if (recurrenceCtx.todayDayMap().containsKey(task.getGoal().getId())) return true;

                // Path 3: dynamic-day habit (no preferred days) — check weekly quota
                GoalRecurrence dynRec = recurrenceCtx.dynamicRecurrenceMap().get(task.getGoal().getId());
                if (dynRec != null) {
                    int quota = dynRec.getTimesPerPeriod() != null ? dynRec.getTimesPerPeriod() : 1;
                    int alreadyThisWeek = recurrenceCtx.weeklyCountByTaskId().getOrDefault(task.getId(), 0);
                    boolean include = alreadyThisWeek < quota;
                    log.info("Dynamic habit task {} — quota={} alreadyThisWeek={} include={}",
                        task.getPid(), quota, alreadyThisWeek, include);
                    return include;
                }

                return false;
            })
            .toList();
    }

    record RecurrenceContext(
        Map<Long, GoalRecurrenceDay> todayDayMap,
        Set<Long> dailyGoalIds,
        Map<Long, GoalRecurrence> dynamicRecurrenceMap,
        Map<Long, Integer> weeklyCountByTaskId
    ) {}
}
