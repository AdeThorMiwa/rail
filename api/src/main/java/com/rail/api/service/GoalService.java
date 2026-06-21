package com.rail.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rail.api.component.DtoMapper;
import com.rail.api.dto.GoalDto;
import com.rail.api.dto.GoalTargetDto;
import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.OccurrenceStatus;
import com.rail.api.entity.ScheduleChange;
import com.rail.api.entity.ScheduleChangeType;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskOccurrence;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.User;
import com.rail.api.event.GoalCompletedEvent;
import com.rail.api.event.ScheduleChangeEvent;
import com.rail.api.repository.DailyScheduleEntryRepository;
import com.rail.api.repository.DailyScheduleRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.GoalTargetRepository;
import com.rail.api.repository.MilestoneRepository;
import com.rail.api.repository.ScheduleChangeRepository;
import com.rail.api.repository.TaskOccurrenceRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.TaskTargetRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalTargetRepository goalTargetRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final TaskTargetRepository taskTargetRepository;
    private final TaskOccurrenceRepository occurrenceRepository;
    private final DailyScheduleRepository dailyScheduleRepository;
    private final DailyScheduleEntryRepository dailyScheduleEntryRepository;
    private final ScheduleChangeRepository scheduleChangeRepository;
    private final UserSchedulingProfileRepository profileRepository;
    private final StreakService streakService;
    private final DtoMapper dtoMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<GoalDto> list(User user) {
        return goalRepository
            .findByOwnerAndStatus(user, com.rail.api.entity.GoalStatus.ACTIVE)
            .stream()
            .map(goal -> toDto(goal, user))
            .toList();
    }

    private GoalDto toDto(Goal goal, User user) {
        var milestones = milestoneRepository.findByGoalOrderByPosition(goal);
        var today = profileRepository
            .findByUser(user)
            .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
            .orElseGet(LocalDate::now);
        var todaysTasks = dailyScheduleEntryRepository
            .findTaskEntriesByUserAndDateAndGoal(user, today, goal)
            .stream()
            .map(e -> e.getTask())
            .toList();
        var target = goalTargetRepository
            .findByGoal(goal)
            .map(gt -> {
                var current =
                    taskTargetRepository.sumActualValueByGoalAndTaskStatus(
                        goal,
                        TaskStatus.DONE
                    );
                return new GoalTargetDto(
                    gt.getTargetValue(),
                    current,
                    gt.getUnit()
                );
            })
            .orElse(null);
        var habitStats = (goal.getType() == GoalType.HABIT ||
            goal.getType() == GoalType.ABSTINENCE)
            ? streakService.computeStats(goal)
            : null;
        return dtoMapper.toGoalDto(
            goal,
            goal.getIntention().getTitle(),
            target,
            habitStats,
            milestones,
            todaysTasks
        );
    }

    @Transactional(readOnly = true)
    public GoalDto get(User user, UUID goalPid) {
        var goal = goalRepository
            .findByPidAndOwner(goalPid, user)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Goal not found"
                )
            );
        return toDto(goal, user);
    }

    @Transactional
    public void complete(User user, UUID goalPid, String completionNotes) {
        Goal goal = goalRepository
            .findByPidAndOwner(goalPid, user)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Goal not found"
                )
            );
        finishGoal(goal, completionNotes);
    }

    @Transactional
    public void finishGoal(Goal goal, String completionNotes) {
        long actualMinutes = taskRepository
            .findByGoalAndStatus(goal, TaskStatus.DONE)
            .stream()
            .mapToLong(t ->
                t.getDurationMinutes() != null ? t.getDurationMinutes() : 0
            )
            .sum();
        goal.setActualTotalHours(actualMinutes / 60L);
        if (completionNotes != null && !completionNotes.isBlank()) {
            goal.setCompletionNotes(completionNotes);
        }
        goal.setStatus(com.rail.api.entity.GoalStatus.COMPLETED);
        goal.setCompletedAt(java.time.Instant.now());
        goalRepository.saveAndFlush(goal);
        eventPublisher.publishEvent(
            new GoalCompletedEvent(
                goal.getIntention().getOwner(),
                goal.getPid()
            )
        );
    }

    @Transactional
    public void slip(User user, UUID goalPid, String note) {
        Goal goal = goalRepository
            .findByPidAndOwner(goalPid, user)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Goal not found"
                )
            );

        if (goal.getType() != GoalType.ABSTINENCE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Slip is only valid for ABSTINENCE goals"
            );
        }

        List<Task> tasks = taskRepository.findByGoal(goal);
        if (tasks.isEmpty()) throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "No tasks for this goal"
        );

        LocalDate today = profileRepository
            .findByUser(user)
            .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
            .orElseGet(LocalDate::now);
        Task task = tasks.get(0);
        occurrenceRepository.save(
            TaskOccurrence.builder()
                .task(task)
                .occurrenceDate(today)
                .status(OccurrenceStatus.MISSED)
                .completionNote(note)
                .build()
        );

        dailyScheduleRepository
            .findByUserAndScheduledDate(user, today)
            .ifPresent(schedule -> {
                ObjectNode prev = objectMapper.createObjectNode();
                ObjectNode next = objectMapper
                    .createObjectNode()
                    .put("slip", "true")
                    .put("goalPid", goalPid.toString());
                ScheduleChange change = scheduleChangeRepository.save(
                    ScheduleChange.builder()
                        .dailySchedule(schedule)
                        .task(task)
                        .changeType(ScheduleChangeType.TASK_SKIPPED)
                        .previousValue(prev.toString())
                        .newValue(next.toString())
                        .reason(note)
                        .build()
                );
                eventPublisher.publishEvent(
                    new ScheduleChangeEvent(user, change)
                );
            });
    }
}
