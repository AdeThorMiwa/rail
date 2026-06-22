package com.rail.api.scheduler;

import com.rail.api.component.DtoMapper;
import com.rail.api.dto.DailyScheduleDto;
import com.rail.api.entity.CycleStatus;
import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.DailyScheduleEntryStatus;
import com.rail.api.entity.DailyScheduleStatus;
import com.rail.api.entity.OccurrenceStatus;
import com.rail.api.entity.ScheduleChange;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskFlexibility;
import com.rail.api.entity.User;
import com.rail.api.entity.UserSchedulingDay;
import com.rail.api.entity.UserSchedulingProfile;
import com.rail.api.event.SsePublishEvent;
import com.rail.api.repository.CycleFocusRepository;
import com.rail.api.repository.DailyScheduleEntryRepository;
import com.rail.api.repository.DailyScheduleRepository;
import com.rail.api.repository.ScheduleChangeRepository;
import com.rail.api.repository.TaskOccurrenceRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.UserConnieLogRepository;
import com.rail.api.repository.UserCycleRepository;
import com.rail.api.repository.UserSchedulingDayRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@Slf4j
public class LlmDailyScheduler implements DailyScheduler {

    @Value("${rail.scheduling.buffer-minutes:10}")
    private int bufferMinutes;

    @Value("${rail.scheduling.day-start:07:00}")
    private LocalTime schedulingDayStart;

    @Value("${rail.scheduling.day-end:22:00}")
    private LocalTime schedulingDayEnd;

    private final DailyScheduleRepository scheduleRepository;
    private final DailyScheduleEntryRepository entryRepository;
    private final ScheduleChangeRepository changeRepository;
    private final TaskRepository taskRepository;
    private final TaskOccurrenceRepository occurrenceRepository;
    private final UserSchedulingProfileRepository profileRepository;
    private final UserSchedulingDayRepository schedulingDayRepository;
    private final UserCycleRepository cycleRepository;
    private final CycleFocusRepository cycleFocusRepository;
    private final DtoMapper dtoMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SchedulerTaskFilter taskFilter;
    private final SchedulingPromptBuilder promptBuilder;
    private final SchedulingLlmClient llmClient;
    private final TaskTimePlacer timePlacer;
    private final ScheduleChangeLogger changeLogger;
    private final UserConnieLogRepository connieLogRepository;

    @Override
    public Optional<DailyScheduleDto> getToday(User user) {
        LocalDate today = profileRepository
            .findByUser(user)
            .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
            .orElseGet(LocalDate::now);
        return scheduleRepository
            .findByUserAndScheduledDate(user, today)
            .map(schedule -> {
                List<DailyScheduleEntry> entries =
                    entryRepository.findByDailyScheduleOrderByStartTime(
                        schedule
                    );
                return dtoMapper.toDailyScheduleDto(schedule, entries);
            });
    }

    @Override
    @Transactional
    public DailyScheduleDto recompute(User user, LocalDate date) {
        log.info(
            "[scheduler] recompute started — user={} date={}",
            user.getPid(),
            date
        );

        DailySchedule schedule = scheduleRepository
            .findByUserAndScheduledDate(user, date)
            .orElseGet(() ->
                scheduleRepository.saveAndFlush(
                    DailySchedule.builder()
                        .user(user)
                        .scheduledDate(date)
                        .generatedAt(Instant.now())
                        .status(DailyScheduleStatus.GENERATING)
                        .railNotes("Preparing your schedule…")
                        .build()
                )
            );

        List<DailyScheduleEntry> allEntries =
            entryRepository.findByDailyScheduleOrderByStartTime(schedule);

        List<DailyScheduleEntry> frozenEntries = allEntries
            .stream()
            .filter(
                e ->
                    e.getStatus() == DailyScheduleEntryStatus.COMPLETED ||
                    e.getStatus() == DailyScheduleEntryStatus.MISSED
            )
            .toList();

        List<DailyScheduleEntry> mutableEntries = allEntries
            .stream()
            .filter(e -> !frozenEntries.contains(e))
            .toList();

        if (!mutableEntries.isEmpty()) {
            entryRepository.deleteAll(mutableEntries);
            entryRepository.flush();
        }

        DailyScheduleDto result = buildSchedule(
            user,
            schedule,
            date,
            frozenEntries
        );

        if (!mutableEntries.isEmpty()) {
            List<DailyScheduleEntry> newEntries =
                entryRepository.findByDailyScheduleOrderByStartTime(schedule);
            changeLogger.log(schedule, mutableEntries, newEntries);
        }

        log.info(
            "[scheduler] recompute complete — user={} date={} entries={}",
            user.getPid(),
            date,
            result.entries().size()
        );
        return result;
    }

    @Transactional
    private DailyScheduleDto buildSchedule(
        User user,
        DailySchedule existing,
        LocalDate date,
        List<DailyScheduleEntry> frozenEntries
    ) {
        UserSchedulingProfile profile = profileRepository
            .findByUser(user)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "Scheduling profile not set up"
                )
            );

        ZoneId userZone = ZoneId.of(profile.getTimezone());
        LocalTime nowInZone = LocalTime.now(userZone);
        LocalTime effectiveStart = nowInZone.isAfter(schedulingDayStart)
            ? nowInZone
            : schedulingDayStart;

        log.info("Scheduling from: {}", effectiveStart);

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        LocalTime deepStart = profile.getDeepWorkStart();
        LocalTime deepEnd = profile.getDeepWorkEnd();

        for (UserSchedulingDay day : schedulingDayRepository.findByUserSchedulingProfile(
            profile
        )) {
            if (day.getDayOfWeek() == dayOfWeek) {
                if (day.getPreferredWorkStart() != null) deepStart =
                    day.getPreferredWorkStart();
                if (day.getPreferredWorkEnd() != null) deepEnd =
                    day.getPreferredWorkEnd();
                break;
            }
        }

        List<Task> allPendingTasks = taskRepository.findPendingTasksForUser(
            user
        );
        SchedulerTaskFilter.RecurrenceContext recurrenceCtx =
            taskFilter.buildRecurrenceContext(allPendingTasks, dayOfWeek, date);

        log.info(
            "All pending task: {}",
            allPendingTasks
                .stream()
                .map(t -> t.getPid())
                .toList()
        );
        log.info("Daily goal ids: {}", recurrenceCtx.dailyGoalIds());

        Set<Long> doneRecurringTaskIds = occurrenceRepository
            .findByTaskInAndOccurrenceDate(allPendingTasks, date)
            .stream()
            .filter(o -> o.getStatus() == OccurrenceStatus.DONE)
            .map(o -> o.getTask().getId())
            .collect(Collectors.toSet());

        log.info("Filtering all pending tasks");
        List<Task> pendingTasks = taskFilter.filterForDate(
            allPendingTasks,
            date,
            userZone,
            recurrenceCtx,
            doneRecurringTaskIds
        );

        List<Task> fixedTasks = pendingTasks
            .stream()
            .filter(
                t ->
                    t.getFlexibility() == TaskFlexibility.FIXED &&
                    t.getFixedTime() != null
            )
            .toList();
        List<Task> flexibleTasks = pendingTasks
            .stream()
            .filter(
                t ->
                    t.getFlexibility() != TaskFlexibility.FIXED ||
                    t.getFixedTime() == null
            )
            .toList();

        List<Task> recentHistory = taskRepository.findRecentCompletions(
            user,
            Instant.now().minus(14, ChronoUnit.DAYS)
        );
        List<Task> recentSkips = taskRepository.findRecentSkipsAndMisses(
            user,
            Instant.now().minus(14, ChronoUnit.DAYS)
        );
        List<ScheduleChange> recentChanges = changeRepository.findRecentByUser(
            user,
            Instant.now().minus(7, ChronoUnit.DAYS)
        );
        boolean isRecompute = !frozenEntries.isEmpty();
        List<String> cycleFocusGoalTitles = loadCycleFocusTitles(user);
        var connieLogs = connieLogRepository.findByUserOrderByCreatedAtAsc(user);

        LlmResult llmResult;
        if (pendingTasks.isEmpty()) {
            llmResult = new LlmResult(
                new SchedulingPlan(
                    List.of(),
                    List.of(),
                    "No tasks scheduled yet. Add some intentions to get started!"
                ),
                false
            );
        } else {
            String systemPrompt = promptBuilder.build(
                flexibleTasks,
                fixedTasks,
                profile,
                deepStart,
                deepEnd,
                effectiveStart,
                schedulingDayEnd,
                date,
                dayOfWeek,
                frozenEntries,
                recurrenceCtx.todayDayMap(),
                recurrenceCtx.dailyGoalIds(),
                recentHistory,
                recentSkips,
                recentChanges,
                isRecompute,
                cycleFocusGoalTitles,
                bufferMinutes,
                connieLogs,
                recurrenceCtx.dynamicRecurrenceMap(),
                recurrenceCtx.weeklyCountByTaskId()
            );

            llmResult = llmClient.callWithRetry(
                systemPrompt,
                flexibleTasks,
                deepStart,
                deepEnd,
                effectiveStart,
                schedulingDayEnd,
                date,
                bufferMinutes
            );
        }

        SchedulingPlan plan = llmResult.plan();
        String railNotes = llmResult.usedFallback()
            ? "Your schedule was auto-arranged — Rail's AI was briefly unavailable. Chat with Connie to adjust anything."
            : buildRailNotes(plan);

        existing.setRailNotes(railNotes);
        existing.setGeneratedAt(Instant.now());
        existing.setStatus(
            pendingTasks.isEmpty()
                ? DailyScheduleStatus.PLANNED
                : DailyScheduleStatus.IN_PROGRESS
        );
        DailySchedule schedule = scheduleRepository.saveAndFlush(existing);

        Map<String, Task> flexibleTaskByPid = flexibleTasks
            .stream()
            .collect(Collectors.toMap(t -> t.getPid().toString(), t -> t));

        log.info(
            "Tasks to place — fixed={} flexible={}",
            fixedTasks.size(),
            flexibleTasks.size()
        );

        List<DailyScheduleEntry> newEntries = timePlacer.place(
            schedule,
            fixedTasks,
            plan,
            flexibleTaskByPid,
            deepStart,
            deepEnd,
            effectiveStart,
            schedulingDayEnd,
            bufferMinutes,
            frozenEntries
        );
        entryRepository.saveAllAndFlush(newEntries);

        List<DailyScheduleEntry> allEntries =
            entryRepository.findByDailyScheduleOrderByStartTime(schedule);

        DailyScheduleDto result = dtoMapper.toDailyScheduleDto(schedule, allEntries);
        eventPublisher.publishEvent(new SsePublishEvent(user.getPid(), "schedule_updated", result));

        return result;
    }

    private List<String> loadCycleFocusTitles(User user) {
        return cycleRepository
            .findTopByOwnerAndStatusInOrderByEndDateDesc(
                user,
                List.of(CycleStatus.ACTIVE)
            )
            .map(cycle ->
                cycleFocusRepository
                    .findByCycleOrderByPositionAsc(cycle)
                    .stream()
                    .map(f -> f.getGoal().getTitle())
                    .collect(Collectors.toList())
            )
            .orElse(List.of());
    }

    private String buildRailNotes(SchedulingPlan plan) {
        String notes = plan.dayNotes() != null ? plan.dayNotes() : "";
        if (!plan.conflicts().isEmpty()) {
            notes +=
                "\n\n⚠️ %d task(s) with today's deadline couldn't fit in your schedule.".formatted(
                    plan.conflicts().size()
                );
        }
        return notes;
    }
}
