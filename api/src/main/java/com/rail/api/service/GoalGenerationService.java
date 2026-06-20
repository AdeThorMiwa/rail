package com.rail.api.service;

import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalRecurrence;
import com.rail.api.entity.GoalRecurrenceDay;
import com.rail.api.entity.GoalStatus;
import com.rail.api.entity.GoalTarget;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.Intention;
import com.rail.api.entity.Milestone;
import com.rail.api.entity.MilestoneStatus;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskFlexibility;
import com.rail.api.entity.TaskFlexibilitySetBy;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.TaskTarget;
import com.rail.api.event.TaskCreatedEvent;
import com.rail.api.intelligence.GoalBlueprint;
import com.rail.api.intelligence.GoalBlueprint.MilestoneBlueprint;
import com.rail.api.intelligence.GoalBlueprint.RecurrenceDayBlueprint;
import com.rail.api.intelligence.GoalBlueprint.TaskBlueprint;
import com.rail.api.repository.GoalRecurrenceDayRepository;
import com.rail.api.repository.GoalRecurrenceRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.GoalTargetRepository;
import com.rail.api.repository.MilestoneRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.TaskTargetRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalGenerationService {

    private final GoalRepository goalRepository;
    private final GoalRecurrenceRepository goalRecurrenceRepository;
    private final GoalRecurrenceDayRepository goalRecurrenceDayRepository;
    private final GoalTargetRepository goalTargetRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final TaskTargetRepository taskTargetRepository;
    private final UserSchedulingProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Goal generateFromBlueprint(
        Intention intention,
        GoalBlueprint blueprint
    ) {
        LocalDate today = profileRepository
            .findByUser(intention.getOwner())
            .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
            .orElseGet(LocalDate::now);
        Goal goal = goalRepository.saveAndFlush(
            Goal.builder()
                .intention(intention)
                .title(intention.getTitle())
                .type(blueprint.goalType())
                .status(GoalStatus.ACTIVE)
                .energyLevel(blueprint.energyLevel())
                .estimatedTotalHours(
                    blueprint.estimatedHours() != null
                        ? blueprint.estimatedHours()
                        : 0L
                )
                .earliestStartDate(blueprint.earliestStartDate())
                .targetDate(
                    blueprint.daysUntilTarget() != null &&
                        blueprint.daysUntilTarget() > 0
                        ? today.plusDays(blueprint.daysUntilTarget())
                        : null
                )
                .build()
        );

        if (blueprint.target() != null) {
            goalTargetRepository.save(
                GoalTarget.builder()
                    .goal(goal)
                    .targetValue(blueprint.target().targetValue())
                    .unit(blueprint.target().unit())
                    .build()
            );
        }

        if (blueprint.recurrence() != null) {
            GoalRecurrence recurrence = goalRecurrenceRepository.saveAndFlush(
                GoalRecurrence.builder()
                    .goal(goal)
                    .frequency(blueprint.recurrence().frequency())
                    .timesPerPeriod(
                        blueprint.recurrence().timesPerPeriod() != null
                            ? blueprint.recurrence().timesPerPeriod()
                            : 1
                    )
                    .build()
            );

            List<RecurrenceDayBlueprint> days = blueprint.recurrence().days();
            if (days != null && !days.isEmpty()) {
                goalRecurrenceDayRepository.saveAll(
                    days
                        .stream()
                        .map(d ->
                            GoalRecurrenceDay.builder()
                                .goalRecurrence(recurrence)
                                .dayOfWeek(d.dayOfWeek())
                                .preferredTime(d.preferredTime())
                                .build()
                        )
                        .toList()
                );
            }
        }

        persistMilestonesAndTasks(intention.getOwner(), goal, blueprint);
        return goal;
    }

    private void persistMilestonesAndTasks(
        com.rail.api.entity.User user,
        Goal goal,
        GoalBlueprint blueprint
    ) {
        LocalDate today = profileRepository
            .findByUser(user)
            .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
            .orElseGet(LocalDate::now);
        List<MilestoneBlueprint> milestoneBlueprints =
            blueprint.milestones() != null ? blueprint.milestones() : List.of();

        List<Milestone> milestones = milestoneBlueprints.isEmpty()
            ? List.of()
            : milestoneRepository.saveAllAndFlush(
                  milestoneBlueprints
                      .stream()
                      .map(mb -> {
                          int idx = milestoneBlueprints.indexOf(mb);
                          return Milestone.builder()
                              .goal(goal)
                              .title(mb.title())
                              .position(idx + 1.0f)
                              .status(
                                  idx == 0
                                      ? MilestoneStatus.IN_PROGRESS
                                      : MilestoneStatus.PENDING
                              )
                              .targetDate(
                                  mb.daysUntilTarget() != null &&
                                      mb.daysUntilTarget() > 0
                                      ? today.plusDays(mb.daysUntilTarget())
                                      : null
                              )
                              .build();
                      })
                      .toList()
              );

        List<TaskBlueprint> rawTaskBlueprints =
            blueprint.tasks() != null ? blueprint.tasks() : List.of();

        // For recurring goals deduplicate templates by title — the LLM sometimes generates
        // one task per day even after prompt fix; keep only the first occurrence of each title.
        List<TaskBlueprint> taskBlueprints;
        if (
            goal.getType() == GoalType.HABIT ||
            goal.getType() == GoalType.ABSTINENCE
        ) {
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            taskBlueprints = rawTaskBlueprints
                .stream()
                .filter(tb ->
                    seen.add(
                        tb.title() != null
                            ? tb.title().toLowerCase().strip()
                            : ""
                    )
                )
                .toList();
        } else {
            taskBlueprints = rawTaskBlueprints;
        }

        boolean hasTarget = blueprint.target() != null;

        List<Task> tasks = new ArrayList<>();
        for (TaskBlueprint tb : taskBlueprints) {
            Milestone milestone =
                tb.milestoneIndex() != null &&
                tb.milestoneIndex() >= 0 &&
                tb.milestoneIndex() < milestones.size()
                    ? milestones.get(tb.milestoneIndex())
                    : null;
            TaskFlexibility flex = "FIXED".equalsIgnoreCase(tb.flexibility())
                ? TaskFlexibility.FIXED
                : TaskFlexibility.FLEXIBLE;

            tasks.add(
                Task.builder()
                    .goal(goal)
                    .milestone(milestone)
                    .title(tb.title())
                    .notes(tb.notes())
                    .status(TaskStatus.PENDING)
                    .flexibility(flex)
                    .flexibilitySetBy(TaskFlexibilitySetBy.RAIL)
                    .priority(tb.priority())
                    .durationMinutes(
                        tb.durationMinutes() != null ? tb.durationMinutes() : 60
                    )
                    .fixedTime(parseLocalTime(tb.fixedTime()))
                    .deadline(parseLocalDate(tb.deadline()))
                    .build()
            );
        }
        List<Task> savedTasks = taskRepository.saveAllAndFlush(tasks);
        eventPublisher.publishEvent(new TaskCreatedEvent(user, savedTasks));

        if (hasTarget) {
            List<TaskTarget> taskTargets = new ArrayList<>();
            for (int i = 0; i < savedTasks.size(); i++) {
                TaskBlueprint tb = taskBlueprints.get(i);
                taskTargets.add(
                    TaskTarget.builder()
                        .task(savedTasks.get(i))
                        .estimatedValue(
                            tb.estimatedValue() != null
                                ? tb.estimatedValue()
                                : blueprint
                                      .target()
                                      .targetValue()
                                      .divide(
                                          java.math.BigDecimal.valueOf(
                                              Math.max(savedTasks.size(), 1)
                                          ),
                                          java.math.RoundingMode.HALF_UP
                                      )
                        )
                        .build()
                );
            }
            taskTargetRepository.saveAll(taskTargets);
        }
    }

    private static LocalTime parseLocalTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalTime.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDate parseLocalDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
