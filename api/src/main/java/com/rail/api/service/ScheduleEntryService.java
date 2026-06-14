package com.rail.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rail.api.component.DtoMapper;
import com.rail.api.dto.ScheduleEntryDto;
import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.DailyScheduleEntryStatus;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.OccurrenceStatus;
import com.rail.api.entity.ScheduleChange;
import com.rail.api.entity.ScheduleChangeType;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskCompletionType;
import com.rail.api.entity.TaskFlexibility;
import com.rail.api.entity.TaskOccurrence;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.TaskTarget;
import com.rail.api.entity.User;
import com.rail.api.event.ScheduleChangeEvent;
import com.rail.api.event.SsePublishEvent;
import com.rail.api.event.TaskCreatedEvent;
import com.rail.api.repository.DailyScheduleEntryRepository;
import com.rail.api.repository.GoalRecurrenceRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.ScheduleChangeRepository;
import com.rail.api.repository.TaskOccurrenceRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.TaskTargetRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ScheduleEntryService {

    private final DailyScheduleEntryRepository entryRepository;
    private final TaskRepository taskRepository;
    private final TaskTargetRepository taskTargetRepository;
    private final GoalRepository goalRepository;
    private final ScheduleChangeRepository scheduleChangeRepository;
    private final TaskOccurrenceRepository occurrenceRepository;
    private final GoalRecurrenceRepository recurrenceRepository;
    private final TaskCompletionService completionService;
    private final UserSchedulingProfileRepository profileRepository;
    private final DtoMapper dtoMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ScheduleEntryDto complete(
        UUID entryPid,
        User user,
        TaskCompletionType completionType,
        String completionNote,
        BigDecimal actualValue
    ) {
        DailyScheduleEntry entry = resolveOwned(entryPid, user);
        Task task = entry.getTask();
        if (task == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry has no task");

        DailyScheduleEntryStatus prevEntryStatus = entry.getStatus();
        entry.setStatus(DailyScheduleEntryStatus.COMPLETED);
        entryRepository.save(entry);

        TaskCompletionType resolvedType = completionType != null ? completionType : TaskCompletionType.FULL;
        LocalDate occurrenceDate = entry.getDailySchedule().getScheduledDate();

        if (isRecurring(task)) {
            occurrenceRepository.save(TaskOccurrence.builder()
                .task(task)
                .occurrenceDate(occurrenceDate)
                .status(OccurrenceStatus.DONE)
                .completionType(resolvedType)
                .completionNote(completionNote)
                .completedAt(Instant.now())
                .actualValue(actualValue)
                .build());

            var goal = task.getGoal();
            long addedMinutes = task.getDurationMinutes() != null ? task.getDurationMinutes() : 0;
            goal.setActualTotalHours(goal.getActualTotalHours() + (addedMinutes / 60));
            goalRepository.save(goal);
        } else {
            TaskStatus prevTaskStatus = task.getStatus();

            task.setStatus(TaskStatus.DONE);
            task.setCompletionType(resolvedType);
            task.setCompletionNote(completionNote);
            task.setCompletedAt(Instant.now());
            task.setEndedAt(Instant.now());
            taskRepository.save(task);

            var goal = task.getGoal();
            long addedMinutes = task.getDurationMinutes() != null ? task.getDurationMinutes() : 0;
            goal.setActualTotalHours(goal.getActualTotalHours() + (addedMinutes / 60));
            goalRepository.save(goal);

            taskTargetRepository.findByTask(task).ifPresent(tt -> {
                tt.setActualValue(actualValue != null ? actualValue : tt.getEstimatedValue());
                taskTargetRepository.save(tt);
            });

            if (task.getMilestone() != null) completionService.recalcMilestone(task.getMilestone());
            completionService.recalcGoalCompletion(goal);

            if (resolvedType == TaskCompletionType.PARTIAL) {
                createFollowUpIfPartial(user, task, completionNote, actualValue);
            }

            logChange(
                user, entry, task,
                ScheduleChangeType.TASK_COMPLETED,
                buildJson("taskStatus", prevTaskStatus.name(), "entryStatus", prevEntryStatus.name()),
                buildJson("taskStatus", task.getStatus().name(), "entryStatus", entry.getStatus().name(),
                    "completionType", resolvedType.name()),
                completionNote
            );
        }

        return dtoMapper.toEntryDto(entry);
    }

    @Transactional
    public ScheduleEntryDto skip(UUID entryPid, User user, String reason) {
        DailyScheduleEntry entry = resolveOwned(entryPid, user);
        Task task = entry.getTask();
        if (task == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry has no task");

        DailyScheduleEntryStatus prevEntryStatus = entry.getStatus();
        entry.setStatus(DailyScheduleEntryStatus.SKIPPED);
        entry.setSkipReason(reason);
        entryRepository.save(entry);

        LocalDate occurrenceDate = entry.getDailySchedule().getScheduledDate();

        if (isRecurring(task)) {
            occurrenceRepository.save(TaskOccurrence.builder()
                .task(task)
                .occurrenceDate(occurrenceDate)
                .status(OccurrenceStatus.SKIPPED)
                .completionNote(reason)
                .build());

            logChange(
                user, entry, task,
                ScheduleChangeType.TASK_SKIPPED,
                buildJson("entryStatus", prevEntryStatus.name()),
                buildJson("entryStatus", entry.getStatus().name(), "recurring", "true"),
                reason
            );
        } else {
            TaskStatus prevTaskStatus = task.getStatus();

            if (task.getFlexibility() == TaskFlexibility.FIXED) {
                task.setStatus(TaskStatus.MISSED);
            }
            task.setMissReason(reason);
            taskRepository.save(task);

            logChange(
                user, entry, task,
                ScheduleChangeType.TASK_SKIPPED,
                buildJson("taskStatus", prevTaskStatus.name(), "entryStatus", prevEntryStatus.name()),
                buildJson("taskStatus", task.getStatus().name(), "entryStatus", entry.getStatus().name(),
                    "flexibility", task.getFlexibility().name()),
                reason
            );
        }

        return dtoMapper.toEntryDto(entry);
    }

    @Transactional
    public ScheduleEntryDto slip(UUID entryPid, User user, String note) {
        DailyScheduleEntry entry = resolveOwned(entryPid, user);
        Task task = entry.getTask();
        if (task == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry has no task");

        DailyScheduleEntryStatus prevEntryStatus = entry.getStatus();
        entry.setStatus(DailyScheduleEntryStatus.SKIPPED);
        entry.setSkipReason(note);
        entryRepository.save(entry);

        LocalDate occurrenceDate = entry.getDailySchedule().getScheduledDate();
        occurrenceRepository.save(TaskOccurrence.builder()
            .task(task)
            .occurrenceDate(occurrenceDate)
            .status(OccurrenceStatus.MISSED)
            .completionNote(note)
            .build());

        logChange(
            user, entry, task,
            ScheduleChangeType.TASK_SKIPPED,
            buildJson("entryStatus", prevEntryStatus.name()),
            buildJson("entryStatus", entry.getStatus().name(), "slip", "true"),
            note
        );

        return dtoMapper.toEntryDto(entry);
    }

    private boolean isRecurring(Task task) {
        GoalType type = task.getGoal().getType();
        if (type != GoalType.HABIT && type != GoalType.ABSTINENCE) return false;
        return recurrenceRepository.findByGoal(task.getGoal()).isPresent();
    }

    private void createFollowUpIfPartial(User user, Task original, String completionNote, BigDecimal actualValue) {
        String followUpNotes = completionNote != null && !completionNote.isBlank()
            ? "Continued from partial: " + completionNote
            : "Continuing from a partial session.";

        Task followUp = taskRepository.saveAndFlush(Task.builder()
            .goal(original.getGoal())
            .milestone(original.getMilestone())
            .assignedTo(original.getAssignedTo())
            .rescheduledFrom(original)
            .title(original.getTitle())
            .notes(followUpNotes)
            .status(TaskStatus.PENDING)
            .durationMinutes(original.getDurationMinutes())
            .fixedTime(original.getFixedTime())
            .flexibility(original.getFlexibility())
            .flexibilitySetBy(original.getFlexibilitySetBy())
            .priority(original.getPriority())
            .deadline(original.getDeadline())
            .build()
        );

        eventPublisher.publishEvent(new TaskCreatedEvent(user, List.of(followUp)));

        taskTargetRepository.findByTask(original).ifPresent(original_tt -> {
            BigDecimal remaining = original_tt.getEstimatedValue()
                .subtract(actualValue != null ? actualValue : original_tt.getEstimatedValue())
                .max(BigDecimal.ZERO);
            taskTargetRepository.save(TaskTarget.builder()
                .task(followUp)
                .estimatedValue(remaining.compareTo(BigDecimal.ZERO) > 0
                    ? remaining
                    : original_tt.getEstimatedValue())
                .build()
            );
        });
    }

    private void logChange(
        User user,
        DailyScheduleEntry entry,
        Task task,
        ScheduleChangeType type,
        String previousValue,
        String newValue,
        String reason
    ) {
        ScheduleChange change = scheduleChangeRepository.save(
            ScheduleChange.builder()
                .dailySchedule(entry.getDailySchedule())
                .task(task)
                .changeType(type)
                .previousValue(previousValue)
                .newValue(newValue)
                .reason(reason)
                .build()
        );
        eventPublisher.publishEvent(new ScheduleChangeEvent(user, change));
    }

    private String buildJson(String... keysAndValues) {
        ObjectNode node = objectMapper.createObjectNode();
        for (int i = 0; i < keysAndValues.length - 1; i += 2) {
            node.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return node.toString();
    }

    @Transactional
    public void autoMissOverdueEntries() {
        Set<UUID> notifiedUsers = new HashSet<>();

        for (var profile : profileRepository.findAll()) {
            ZoneId zone = ZoneId.of(profile.getTimezone());
            LocalDate today = LocalDate.now(zone);
            LocalTime now = LocalTime.now(zone);

            List<DailyScheduleEntry> overdue =
                entryRepository.findPendingTaskEntriesDueForAutoMiss(
                    profile.getUser(), today, now
                );

            for (DailyScheduleEntry entry : overdue) {
                entry.setStatus(DailyScheduleEntryStatus.MISSED);
                entryRepository.save(entry);

                Task task = entry.getTask();
                if (task != null) {
                    if (task.getFlexibility() == TaskFlexibility.FIXED) {
                        task.setStatus(TaskStatus.MISSED);
                        taskRepository.save(task);
                        completionService.recalcGoalCompletion(task.getGoal());
                    } else {
                        task.setStatus(TaskStatus.PENDING);
                        taskRepository.save(task);
                    }
                }

                notifiedUsers.add(profile.getUser().getPid());
            }
        }

        for (UUID userPid : notifiedUsers) {
            eventPublisher.publishEvent(
                new SsePublishEvent(userPid, "schedule_updated", Map.of())
            );
        }
    }

    private DailyScheduleEntry resolveOwned(UUID entryPid, User user) {
        DailyScheduleEntry entry = entryRepository.findByPid(entryPid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
        Task task = entry.getTask();
        if (task != null && !task.getGoal().getIntention().getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return entry;
    }

}
