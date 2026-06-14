package com.rail.api.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rail.api.component.DtoMapper;
import com.rail.api.dto.DailyScheduleDto;
import com.rail.api.entity.CycleStatus;
import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.DailyScheduleEntryStatus;
import com.rail.api.entity.DailyScheduleEntryType;
import com.rail.api.entity.DailyScheduleStatus;
import com.rail.api.entity.EnergyLevel;
import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalRecurrence;
import com.rail.api.entity.GoalRecurrenceDay;
import com.rail.api.entity.GoalRecurrenceFrequency;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.MilestoneStatus;
import com.rail.api.entity.OccurrenceStatus;
import com.rail.api.entity.ScheduleChange;
import com.rail.api.entity.ScheduleChangeType;
import com.rail.api.entity.Task;
import com.rail.api.entity.User;
import com.rail.api.entity.UserSchedulingDay;
import com.rail.api.entity.UserSchedulingProfile;
import com.rail.api.event.SsePublishEvent;
import com.rail.api.repository.CycleFocusRepository;
import com.rail.api.repository.DailyScheduleEntryRepository;
import com.rail.api.repository.DailyScheduleRepository;
import com.rail.api.repository.GoalRecurrenceDayRepository;
import com.rail.api.repository.GoalRecurrenceRepository;
import com.rail.api.repository.ScheduleChangeRepository;
import com.rail.api.repository.TaskOccurrenceRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.UserCycleRepository;
import com.rail.api.repository.UserSchedulingDayRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
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

    private record TimeSlot(LocalTime start, LocalTime end) {}

    private record Placement(LocalTime start, LocalTime end, Task task) {}

    private record LlmResult(SchedulingPlan plan, boolean usedFallback) {}

    private record RecurrenceContext(
        Map<Long, GoalRecurrenceDay> dayMap,
        Set<Long> dailyGoalIds
    ) {}

    private static final DateTimeFormatter FILE_FMT =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Value("${rail.scheduling.buffer-minutes:10}")
    private int bufferMinutes;

    @Value("${rail.scheduling.day-start:07:00}")
    private LocalTime schedulingDayStart;

    @Value("${rail.scheduling.day-end:22:00}")
    private LocalTime schedulingDayEnd;

    @Value("${rail.scheduling.model:deepseek-v4-pro}")
    private String schedulingModel;

    @Value("${rail.llm.max-json-retries:2}")
    private int maxJsonRetries;

    @Value("${rail.llm.log-dir}")
    private String logDir;

    @Value("${rail.llm.log-enabled}")
    private boolean llmLogEnabled;

    private final DailyScheduleRepository scheduleRepository;
    private final DailyScheduleEntryRepository entryRepository;
    private final ScheduleChangeRepository changeRepository;
    private final TaskRepository taskRepository;
    private final TaskOccurrenceRepository occurrenceRepository;
    private final UserSchedulingProfileRepository profileRepository;
    private final UserSchedulingDayRepository schedulingDayRepository;
    private final GoalRecurrenceRepository recurrenceRepository;
    private final GoalRecurrenceDayRepository recurrenceDayRepository;
    private final UserCycleRepository cycleRepository;
    private final CycleFocusRepository cycleFocusRepository;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final DtoMapper dtoMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Public API ────────────────────────────────────────────────────────────

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
                return toDto(schedule, entries);
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
            logScheduleChanges(schedule, mutableEntries, newEntries);
        }

        log.info(
            "[scheduler] recompute complete — user={} date={} entries={}",
            user.getPid(),
            date,
            result.entries().size()
        );
        return result;
    }

    // ─── Core schedule builder ──────────────────────────────────────────────────

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
        // For a first-time schedule generated mid-day, start from now so past time slots aren't planned
        LocalTime effectiveStart = nowInZone.isAfter(schedulingDayStart)
            ? nowInZone
            : schedulingDayStart;

        log.info("Scheduling from: {}", effectiveStart.toString());

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
        RecurrenceContext recurrenceCtx = buildRecurrenceMap(
            allPendingTasks,
            dayOfWeek
        );
        Map<Long, GoalRecurrenceDay> todayRecurrence = recurrenceCtx.dayMap();
        Set<Long> dailyGoalIds = recurrenceCtx.dailyGoalIds();

        log.info("Daily goals id: {}", dailyGoalIds);

        // Filter recurring tasks: only include if scheduled today and not already done today
        Set<Long> doneRecurringTaskIds = occurrenceRepository
            .findByTaskInAndOccurrenceDate(allPendingTasks, date)
            .stream()
            .filter(o -> o.getStatus() == OccurrenceStatus.DONE)
            .map(o -> o.getTask().getId())
            .collect(Collectors.toSet());

        log.info("Filtering all pending tasks");

        List<Task> pendingTasks = allPendingTasks
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
        List<Task> recentHistory = taskRepository.findRecentCompletions(
            user,
            Instant.now().minus(14, ChronoUnit.DAYS)
        );
        List<ScheduleChange> recentChanges = changeRepository.findRecentByUser(
            user,
            Instant.now().minus(7, ChronoUnit.DAYS)
        );
        boolean isRecompute = !frozenEntries.isEmpty();

        List<String> cycleFocusGoalTitles = loadCycleFocusTitles(user);

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
            llmResult = callLlmWithRetry(
                pendingTasks,
                profile,
                deepStart,
                deepEnd,
                effectiveStart,
                date,
                dayOfWeek,
                frozenEntries,
                todayRecurrence,
                dailyGoalIds,
                recentHistory,
                recentChanges,
                isRecompute,
                cycleFocusGoalTitles
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

        Map<String, Task> taskByPid = pendingTasks
            .stream()
            .collect(Collectors.toMap(t -> t.getPid().toString(), t -> t));

        log.info("Tasks to place: {}", taskByPid);

        List<DailyScheduleEntry> newEntries = placeEntries(
            schedule,
            plan,
            taskByPid,
            deepStart,
            deepEnd,
            effectiveStart,
            frozenEntries
        );
        entryRepository.saveAllAndFlush(newEntries);

        List<DailyScheduleEntry> allEntries =
            entryRepository.findByDailyScheduleOrderByStartTime(schedule);

        if (llmResult.usedFallback()) {
            eventPublisher.publishEvent(
                new SsePublishEvent(
                    user.getPid(),
                    "schedule_updated",
                    toDto(schedule, allEntries)
                )
            );
        }

        return toDto(schedule, allEntries);
    }

    private LlmResult callLlmWithRetry(
        List<Task> pendingTasks,
        UserSchedulingProfile profile,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart,
        LocalDate date,
        DayOfWeek dayOfWeek,
        List<DailyScheduleEntry> frozenEntries,
        Map<Long, GoalRecurrenceDay> todayRecurrence,
        Set<Long> dailyGoalIds,
        List<Task> recentHistory,
        List<ScheduleChange> recentChanges,
        boolean isRecompute,
        List<String> cycleFocusGoalTitles
    ) {
        String systemPrompt = buildSystemPrompt(
            pendingTasks,
            profile,
            deepStart,
            deepEnd,
            effectiveStart,
            date,
            dayOfWeek,
            frozenEntries,
            todayRecurrence,
            dailyGoalIds,
            recentHistory,
            recentChanges,
            isRecompute,
            cycleFocusGoalTitles
        );

        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage("Build today's schedule.")
        );

        log.info(
            "[scheduler] calling LLM — model={} tasks={}",
            schedulingModel,
            pendingTasks.size()
        );
        String raw = invokeModel(messages, 0);
        log.info(
            "[scheduler] LLM responded (attempt 0) — response length={}",
            raw == null ? 0 : raw.length()
        );
        writeLlmLog(messages, raw, 0);
        SchedulingPlan plan = parsePlan(raw);

        for (
            int attempt = 1;
            attempt <= maxJsonRetries && plan == null;
            attempt++
        ) {
            List<Message> retryMessages = new ArrayList<>(messages);
            retryMessages.add(new AssistantMessage(raw));
            retryMessages.add(
                new UserMessage(
                    "Your response was not valid JSON matching the required schema. " +
                        "Return ONLY the JSON object. No explanation, no markdown fences."
                )
            );
            log.warn(
                "[scheduler] LLM parse failed — retrying (attempt {})",
                attempt
            );
            raw = invokeModel(retryMessages, attempt);
            log.info(
                "[scheduler] LLM responded (attempt {}) — response length={}",
                attempt,
                raw == null ? 0 : raw.length()
            );
            writeLlmLog(retryMessages, raw, attempt);
            plan = parsePlan(raw);
        }

        if (plan == null) {
            log.error(
                "Scheduling LLM failed after {} retries — using linear fallback",
                maxJsonRetries
            );
            return new LlmResult(
                linearFallback(
                    pendingTasks,
                    deepStart,
                    deepEnd,
                    effectiveStart,
                    date
                ),
                true
            );
        }

        return new LlmResult(plan, false);
    }

    private String buildSystemPrompt(
        List<Task> pendingTasks,
        UserSchedulingProfile profile,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart,
        LocalDate date,
        DayOfWeek dayOfWeek,
        List<DailyScheduleEntry> frozenEntries,
        Map<Long, GoalRecurrenceDay> todayRecurrence,
        Set<Long> dailyGoalIds,
        List<Task> recentHistory,
        List<ScheduleChange> recentChanges,
        boolean isRecompute,
        List<String> cycleFocusGoalTitles
    ) {
        int deepMinutes = (int) Duration.between(
            deepStart,
            deepEnd
        ).toMinutes();
        int preDeepMinutes = (int) Duration.between(
            effectiveStart,
            deepStart
        ).toMinutes();
        int postDeepMinutes = (int) Duration.between(
            deepEnd,
            schedulingDayEnd
        ).toMinutes();
        int outerMinutes = preDeepMinutes + postDeepMinutes;

        StringBuilder sb = new StringBuilder();
        sb.append(
            "You are Rail's intelligent daily scheduler. Select and order tasks to build the best possible day.\n\n"
        );

        // Windows
        sb.append("═══ SCHEDULING WINDOWS ═══\n");
        sb.append("Date: ")
            .append(date)
            .append(", ")
            .append(dayOfWeek)
            .append("\n");
        sb.append("Energy pattern: ")
            .append(profile.getEnergyPattern())
            .append("\n");
        sb.append("Effective start time: ")
            .append(effectiveStart)
            .append(" → ")
            .append(schedulingDayEnd)
            .append("\n")
            .append(
                "Times before the effective start time either means it has already been scheduled/frozen for other tasks, or this is when the user woke up today. So DO NOT schedule anything before this time!"
            )
            .append("\n\n");

        sb.append("DEEP WORK WINDOW  ")
            .append(deepStart)
            .append(" → ")
            .append(deepEnd)
            .append("  (")
            .append(deepMinutes)
            .append(" min)\n");
        sb.append("  Purpose: focused, cognitively demanding work.\n");
        sb.append(
            "  Allowed: DEEP tasks (first priority), LIGHT tasks (only if room remains after all DEEP placed).\n"
        );
        sb.append("  ADMIN tasks are NEVER placed here.\n\n");

        sb.append(
            "OUTER WINDOW  (everything outside the deep work window within the waking day)\n"
        );
        if (preDeepMinutes > 0) {
            sb.append("  Pre-deep:  ")
                .append(effectiveStart)
                .append(" → ")
                .append(deepStart)
                .append("  (")
                .append(preDeepMinutes)
                .append(" min)\n");
        }
        sb.append("  Post-deep: ")
            .append(deepEnd)
            .append(" → ")
            .append(schedulingDayEnd)
            .append("  (")
            .append(postDeepMinutes)
            .append(" min)\n");
        sb.append("  Total outer capacity: ")
            .append(outerMinutes)
            .append(" min\n");
        sb.append(
            "  Purpose: lighter execution work outside the focus window.\n"
        );
        sb.append(
            "  Allowed: LIGHT tasks (first priority), ADMIN tasks (fills remaining slots).\n\n"
        );

        String deepOrderNote = switch (profile.getEnergyPattern()) {
            case MORNING_PEAK -> "MORNING_PEAK: place most demanding DEEP tasks early in the deep window (lower order numbers).";
            case EVENING_PEAK -> "EVENING_PEAK: place most demanding DEEP tasks later in the deep window (higher order numbers).";
            default -> "CONSISTENT: order DEEP tasks by priority and deadline.";
        };
        sb.append("Ordering note — ").append(deepOrderNote).append("\n\n");

        // Task pool
        sb.append("═══ TASK POOL (")
            .append(pendingTasks.size())
            .append(" available) ═══\n");
        for (Task task : pendingTasks) {
            sb.append("\n[")
                .append(task.getPid())
                .append("] ")
                .append(task.getTitle())
                .append("\n");
            sb.append("  Goal: \"")
                .append(task.getGoal().getTitle())
                .append("\"")
                .append(" | Type: ")
                .append(task.getGoal().getType())
                .append(" | Energy: ")
                .append(task.getGoal().getEnergyLevel())
                .append("\n");
            sb.append("  Priority: ").append(task.getPriority());
            sb.append(" | Duration: ")
                .append(
                    task.getDurationMinutes() != null
                        ? task.getDurationMinutes()
                        : 60
                )
                .append("min");
            sb.append(" | Flexibility: ")
                .append(task.getFlexibility())
                .append("\n");

            if (task.getDeadline() != null) {
                sb.append("  Deadline: ").append(task.getDeadline());
                if (task.getDeadline().equals(date)) sb.append(" ⚠️ TODAY");
                sb.append("\n");
            }
            if (task.getFixedTime() != null) {
                sb.append("  Fixed time: ")
                    .append(task.getFixedTime())
                    .append("\n");
            }
            if (task.getGoal().getTargetDate() != null) {
                sb.append("  Goal target date: ")
                    .append(task.getGoal().getTargetDate())
                    .append("\n");
            }
            if (task.getNotes() != null && !task.getNotes().isBlank()) {
                sb.append("  Notes: ").append(task.getNotes()).append("\n");
            }
            GoalRecurrenceDay recDay = todayRecurrence.get(
                task.getGoal().getId()
            );
            if (recDay != null) {
                sb.append("  Recurring today");
                if (recDay.getPreferredTime() != null) sb.append(" at ").append(
                    recDay.getPreferredTime()
                );
                sb.append("\n");
            } else {
                if (dailyGoalIds.contains(task.getGoal().getId())) sb.append(
                    "  Recurring today (daily habit)\n"
                );
            }
            if (task.getMilestone() != null) {
                sb.append("  Milestone: \"")
                    .append(task.getMilestone().getTitle())
                    .append("\" (position ")
                    .append(task.getMilestone().getPosition())
                    .append(
                        task.getMilestone().getStatus() ==
                            MilestoneStatus.IN_PROGRESS
                            ? ") ← current milestone"
                            : ")"
                    )
                    .append("\n");
            }
        }

        // Frozen entries
        sb.append("\n═══ ALREADY DONE TODAY ═══\n");
        if (frozenEntries.isEmpty()) {
            sb.append("None — fresh schedule.\n");
        } else {
            sb.append("Frozen — do not reassign their slots:\n");
            for (DailyScheduleEntry e : frozenEntries) {
                String title =
                    e.getTask() != null
                        ? "\"" + e.getTask().getTitle() + "\""
                        : "(unknown)";
                sb.append("  ")
                    .append(e.getStartTime())
                    .append("–")
                    .append(e.getEndTime())
                    .append(": ")
                    .append(title)
                    .append(" [DONE]\n");
            }
        }

        // History
        sb.append("\n═══ COMPLETION HISTORY (last 14 days) ═══\n");
        if (recentHistory.isEmpty()) {
            sb.append(
                "No history yet — new user. Prefer shorter tasks and conservative estimates.\n"
            );
        } else {
            for (Task t : recentHistory.stream().limit(20).toList()) {
                String actual = (t.getStartedAt() != null &&
                    t.getEndedAt() != null)
                    ? Duration.between(
                          t.getStartedAt(),
                          t.getEndedAt()
                      ).toMinutes() + "min actual"
                    : "duration unrecorded";
                sb.append("  - \"")
                    .append(t.getTitle())
                    .append("\" (")
                    .append(t.getGoal().getTitle())
                    .append(") — ")
                    .append(
                        t.getCompletionType() != null
                            ? t.getCompletionType()
                            : "FULL"
                    )
                    .append(", est ")
                    .append(
                        t.getDurationMinutes() != null
                            ? t.getDurationMinutes()
                            : "?"
                    )
                    .append("min, ")
                    .append(actual)
                    .append("\n");
            }
        }

        // Recent changes
        sb.append("\n═══ RECENT PLAN CHANGES (last 7 days) ═══\n");
        if (recentChanges.isEmpty()) {
            sb.append("No recent changes — plan is stable.\n");
        } else {
            for (ScheduleChange sc : recentChanges
                .stream()
                .limit(10)
                .toList()) {
                sb.append("  - ")
                    .append(sc.getChangeType())
                    .append(sc.getReason() != null ? ": " + sc.getReason() : "")
                    .append("\n");
            }
        }

        // Active cycle focus
        if (!cycleFocusGoalTitles.isEmpty()) {
            sb.append("\n═══ ACTIVE CYCLE FOCUS ═══\n");
            sb.append(
                "The user is in an active cycle. Their focus goals are:\n"
            );
            cycleFocusGoalTitles.forEach(title ->
                sb.append("  - \"").append(title).append("\"\n")
            );
            sb.append(
                "When capacity is limited and tasks compete for the same window, " +
                    "prefer tasks belonging to these focus goals. " +
                    "Non-focus goal tasks are still scheduled but yield to focus tasks when slots are tight.\n"
            );
        }

        // Rules
        sb.append("\n═══ RULES ═══\n");
        sb.append(
            "1. ADMIN tasks → energyZone = \"OUTER\" always. Never in DEEP_WINDOW.\n"
        );
        sb.append("2. DEEP tasks → energyZone = \"DEEP_WINDOW\" always.\n");
        sb.append(
            "3. LIGHT tasks → \"DEEP_WINDOW\" if room remains after all DEEP tasks are placed; otherwise \"OUTER\".\n"
        );
        sb.append(
            "4. FIXED tasks → energyZone = \"FIXED\" always. FIXED tasks bypass ALL window rules entirely.\n" +
                "   A FIXED task is placed at its fixedTime regardless of which window that time falls in.\n" +
                "   The ADMIN-never-in-DEEP_WINDOW rule does NOT apply to FIXED tasks.\n" +
                "   FIXED tasks are NEVER added to conflicts[] for window placement reasons — never.\n" +
                "   The only valid conflict for a FIXED task is a direct time overlap with a frozen (already completed) entry.\n"
        );
        sb.append(
            "5. Tasks with deadline = today → mustScheduleToday = true.\n"
        );
        sb.append(
            "6. Deep window capacity: total DEEP_WINDOW task duration + ("
        )
            .append(bufferMinutes)
            .append("min × count) must not exceed ")
            .append(deepMinutes)
            .append("min.\n");
        sb.append("7. Outer window capacity: total OUTER task duration + (")
            .append(bufferMinutes)
            .append("min × count) must not exceed ")
            .append(outerMinutes)
            .append("min.\n");
        sb.append(
            "8. Priority order when capacity is limited: CRITICAL > HIGH > MEDIUM > LOW, then closer deadlines first.\n"
        );
        sb.append(
            "9. Deadline = today NON-FIXED tasks that cannot fit in either window → add to conflicts[].\n"
        );
        sb.append(
            "10. dayNotes: exactly 1 warm sentence — no more. Reference specific task or goal names. Sound like a thoughtful friend setting the tone for the day.\n"
        );
        if (isRecompute && !frozenEntries.isEmpty()) {
            sb.append(
                "    dayNotes must acknowledge work already done today and welcome the new tasks — still exactly 1 sentence.\n"
            );
        }

        sb.append(
            """

            ═══ OUTPUT — STRICT JSON ═══
            {
              "selected": [
                {"taskPid": "<exact pid from pool>", "order": 1, "energyZone": "DEEP_WINDOW|OUTER|FIXED", "mustScheduleToday": true|false, "schedulingNote": "one line why"}
              ],
              "conflicts": [
                {"taskPid": "...", "reason": "Deadline today but both windows are full"}
              ],
              "dayNotes": "..."
            }

            Only use taskPid values that appear in the task pool above. No fabrication.
            """
        );

        return sb.toString();
    }

    private String invokeModel(List<Message> messages, int attempt) {
        var options = OpenAiChatOptions.builder()
            .model(schedulingModel)
            .responseFormat(
                new ResponseFormat(
                    ResponseFormat.Type.JSON_OBJECT,
                    (String) null
                )
            )
            .reasoningEffort("high")
            .build();

        var accumulated = new StringBuilder();
        var charCount = new AtomicInteger(0);
        Path logPath = resolveStreamLogPath(attempt);

        try (
            BufferedWriter writer = tryOpenStreamLog(logPath, messages, attempt)
        ) {
            ChatClient.create(chatModel)
                .prompt()
                .messages(messages)
                .options(options)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    accumulated.append(chunk);
                    int total = charCount.addAndGet(chunk.length());
                    appendToStreamLog(writer, chunk);
                    if (total % 500 < chunk.length()) {
                        log.info(
                            "[scheduler] streaming... {} chars received",
                            total
                        );
                    }
                })
                .doOnError(e -> {
                    log.error(
                        "[scheduler] stream error after {} chars: {}",
                        charCount.get(),
                        e.getMessage(),
                        e
                    );
                    appendToStreamLog(
                        writer,
                        "\n\n=== STREAM ERROR: " + e.getMessage() + " ===\n"
                    );
                })
                .doOnComplete(() -> {
                    log.info(
                        "[scheduler] stream complete — {} chars total",
                        charCount.get()
                    );
                    appendToStreamLog(
                        writer,
                        "\n\n=== STREAM COMPLETE: " +
                            charCount.get() +
                            " chars ===\n"
                    );
                })
                .blockLast();
        } catch (Exception e) {
            log.error(
                "[scheduler] invokeModel failed after {} chars: {}",
                charCount.get(),
                e.getMessage(),
                e
            );
            return null;
        }

        return accumulated.isEmpty() ? null : accumulated.toString();
    }

    private Path resolveStreamLogPath(int attempt) {
        String suffix = attempt > 0 ? "_retry" + attempt : "";
        String filename =
            LocalDateTime.now().format(FILE_FMT) +
            "_LlmDailyScheduler_stream" +
            suffix +
            ".log";
        return Paths.get(logDir).resolve(filename);
    }

    private BufferedWriter tryOpenStreamLog(
        Path path,
        List<Message> messages,
        int attempt
    ) {
        if (!llmLogEnabled) return null;
        try {
            Files.createDirectories(path.getParent());
            var writer = Files.newBufferedWriter(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            writer.write("scheduler: LlmDailyScheduler\n");
            writer.write("model    : " + schedulingModel + "\n");
            writer.write("attempt  : " + attempt + "\n");
            writer.write("time     : " + LocalDateTime.now() + "\n\n");
            for (Message msg : messages) {
                String role = switch (msg) {
                    case SystemMessage m -> "system";
                    case AssistantMessage m -> "assistant";
                    default -> "user";
                };
                writer.write("=== " + role.toUpperCase() + " ===\n");
                writer.write(msg.getText() + "\n\n");
            }
            writer.write("=== STREAMING RESPONSE ===\n");
            writer.flush();
            return writer;
        } catch (IOException e) {
            log.warn(
                "[scheduler] could not open stream log at {}: {}",
                path,
                e.getMessage()
            );
            return null;
        }
    }

    private void appendToStreamLog(BufferedWriter writer, String text) {
        if (writer == null) return;
        try {
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            log.warn(
                "[scheduler] failed to write stream log chunk: {}",
                e.getMessage()
            );
        }
    }

    private SchedulingPlan parsePlan(String raw) {
        if (raw == null) return null;
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n?", "");
            trimmed = trimmed.replaceAll("\\n?```$", "").strip();
        }
        try {
            return objectMapper.readValue(trimmed, SchedulingPlan.class);
        } catch (Exception e) {
            log.warn("Failed to parse scheduling plan: {}", e.getMessage());
            return null;
        }
    }

    private SchedulingPlan linearFallback(
        List<Task> tasks,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart,
        LocalDate date
    ) {
        int deepMinutes = (int) Duration.between(
            deepStart,
            deepEnd
        ).toMinutes();
        int outerMinutes =
            (int) Duration.between(effectiveStart, deepStart).toMinutes() +
            (int) Duration.between(deepEnd, schedulingDayEnd).toMinutes();
        int deepUsed = 0,
            outerUsed = 0;
        List<SchedulingPlan.SelectedTask> selected = new ArrayList<>();
        int order = 1;

        for (Task task : tasks) {
            int dur =
                task.getDurationMinutes() != null
                    ? task.getDurationMinutes()
                    : 60;
            EnergyLevel energy = task.getGoal().getEnergyLevel();
            boolean deadline =
                task.getDeadline() != null && task.getDeadline().equals(date);

            if (energy == EnergyLevel.ADMIN) {
                if (outerUsed + dur + bufferMinutes <= outerMinutes) {
                    selected.add(
                        new SchedulingPlan.SelectedTask(
                            task.getPid().toString(),
                            order++,
                            "OUTER",
                            deadline,
                            "Linear fallback"
                        )
                    );
                    outerUsed += dur + bufferMinutes;
                }
            } else if (energy == EnergyLevel.DEEP) {
                if (deepUsed + dur + bufferMinutes <= deepMinutes) {
                    selected.add(
                        new SchedulingPlan.SelectedTask(
                            task.getPid().toString(),
                            order++,
                            "DEEP_WINDOW",
                            deadline,
                            "Linear fallback"
                        )
                    );
                    deepUsed += dur + bufferMinutes;
                }
            } else {
                // LIGHT
                if (deepUsed + dur + bufferMinutes <= deepMinutes) {
                    selected.add(
                        new SchedulingPlan.SelectedTask(
                            task.getPid().toString(),
                            order++,
                            "DEEP_WINDOW",
                            deadline,
                            "Linear fallback"
                        )
                    );
                    deepUsed += dur + bufferMinutes;
                } else if (outerUsed + dur + bufferMinutes <= outerMinutes) {
                    selected.add(
                        new SchedulingPlan.SelectedTask(
                            task.getPid().toString(),
                            order++,
                            "OUTER",
                            deadline,
                            "Linear fallback"
                        )
                    );
                    outerUsed += dur + bufferMinutes;
                }
            }
        }

        return new SchedulingPlan(
            selected,
            List.of(),
            "Here's your plan for today."
        );
    }

    // ─── Deterministic time placement ─────────────────────────────────────────

    private List<DailyScheduleEntry> placeEntries(
        DailySchedule schedule,
        SchedulingPlan plan,
        Map<String, Task> taskByPid,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart,
        List<DailyScheduleEntry> frozenEntries
    ) {
        List<SchedulingPlan.SelectedTask> fixedSelected = plan
            .selected()
            .stream()
            .filter(s -> "FIXED".equalsIgnoreCase(s.energyZone()))
            .toList();
        List<SchedulingPlan.SelectedTask> deepSelected = plan
            .selected()
            .stream()
            .filter(s -> "DEEP_WINDOW".equalsIgnoreCase(s.energyZone()))
            .sorted(Comparator.comparingInt(SchedulingPlan.SelectedTask::order))
            .toList();
        List<SchedulingPlan.SelectedTask> outerSelected = plan
            .selected()
            .stream()
            .filter(s -> "OUTER".equalsIgnoreCase(s.energyZone()))
            .sorted(Comparator.comparingInt(SchedulingPlan.SelectedTask::order))
            .toList();

        List<TimeSlot> occupied = new ArrayList<>(
            frozenEntries
                .stream()
                .map(e -> new TimeSlot(e.getStartTime(), e.getEndTime()))
                .toList()
        );

        // Anchor FIXED tasks at their fixedTime (valid anywhere in the day)
        List<Placement> fixedPlacements = new ArrayList<>();
        for (SchedulingPlan.SelectedTask sel : fixedSelected) {
            Task t = taskByPid.get(sel.taskPid());
            if (t == null || t.getFixedTime() == null) continue;
            int dur =
                t.getDurationMinutes() != null ? t.getDurationMinutes() : 60;
            LocalTime start = t.getFixedTime();
            if (start.isBefore(effectiveStart)) continue;
            LocalTime end = start.plusMinutes(dur);
            fixedPlacements.add(new Placement(start, end, t));
            occupied.add(new TimeSlot(start, end));
        }

        // Place DEEP_WINDOW tasks within [deepStart, deepEnd]
        List<Placement> deepPlacements = placeTasksInWindow(
            deepSelected,
            taskByPid,
            deepStart.isAfter(effectiveStart) ? deepStart : effectiveStart,
            deepEnd,
            occupied
        );
        for (Placement p : deepPlacements)
            occupied.add(new TimeSlot(p.start(), p.end()));

        // Place OUTER tasks: fill post-deep first, then overflow to pre-deep
        List<Placement> outerPlacements = new ArrayList<>();
        List<SchedulingPlan.SelectedTask> remainingOuter = new ArrayList<>(
            outerSelected
        );

        List<Placement> postDeep = placeTasksInWindow(
            remainingOuter,
            taskByPid,
            deepEnd.isAfter(effectiveStart) ? deepEnd : effectiveStart,
            schedulingDayEnd,
            occupied
        );
        outerPlacements.addAll(postDeep);
        for (Placement p : postDeep)
            occupied.add(new TimeSlot(p.start(), p.end()));

        Set<String> placedOuter = postDeep
            .stream()
            .map(p -> p.task().getPid().toString())
            .collect(Collectors.toSet());
        List<SchedulingPlan.SelectedTask> spillover = remainingOuter
            .stream()
            .filter(s -> !placedOuter.contains(s.taskPid()))
            .toList();

        if (!spillover.isEmpty() && deepStart.isAfter(effectiveStart)) {
            List<Placement> preDeep = placeTasksInWindow(
                spillover,
                taskByPid,
                effectiveStart,
                deepStart,
                occupied
            );
            outerPlacements.addAll(preDeep);
        }

        List<Placement> allPlacements = new ArrayList<>();
        allPlacements.addAll(fixedPlacements);
        allPlacements.addAll(deepPlacements);
        allPlacements.addAll(outerPlacements);
        allPlacements.sort(Comparator.comparing(Placement::start));

        return buildEntries(
            schedule,
            allPlacements,
            frozenEntries,
            deepStart,
            deepEnd,
            effectiveStart
        );
    }

    private List<Placement> placeTasksInWindow(
        List<SchedulingPlan.SelectedTask> selected,
        Map<String, Task> taskByPid,
        LocalTime windowStart,
        LocalTime windowEnd,
        List<TimeSlot> occupied
    ) {
        List<Placement> placements = new ArrayList<>();
        LocalTime cursor = windowStart;

        for (SchedulingPlan.SelectedTask sel : selected) {
            Task t = taskByPid.get(sel.taskPid());
            if (t == null) continue;
            int dur =
                t.getDurationMinutes() != null ? t.getDurationMinutes() : 60;

            cursor = maxTime(cursor, windowStart);
            cursor = advancePastOccupied(cursor, occupied);

            LocalTime end = cursor.plusMinutes(dur);
            if (end.isAfter(windowEnd)) break;

            placements.add(new Placement(cursor, end, t));
            occupied.add(new TimeSlot(cursor, end));

            cursor = end.plusMinutes(bufferMinutes);
            cursor = advancePastOccupied(cursor, occupied);
        }

        return placements;
    }

    private List<DailyScheduleEntry> buildEntries(
        DailySchedule schedule,
        List<Placement> allPlacements,
        List<DailyScheduleEntry> frozenEntries,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart
    ) {
        List<TimeSlot> allOccupied = new ArrayList<>();
        for (DailyScheduleEntry fe : frozenEntries) {
            allOccupied.add(new TimeSlot(fe.getStartTime(), fe.getEndTime()));
        }
        for (Placement p : allPlacements) {
            allOccupied.add(new TimeSlot(p.start(), p.end()));
        }
        allOccupied.sort(Comparator.comparing(TimeSlot::start));

        Set<LocalTime> frozenStarts = frozenEntries
            .stream()
            .map(DailyScheduleEntry::getStartTime)
            .collect(Collectors.toSet());
        Map<LocalTime, Placement> placementByStart = new HashMap<>();
        for (Placement p : allPlacements)
            placementByStart.putIfAbsent(p.start(), p);

        List<DailyScheduleEntry> entries = new ArrayList<>();

        // Pre-deep outer window [effectiveStart, deepStart]
        if (effectiveStart.isBefore(deepStart)) {
            entries.addAll(
                buildWindowEntries(
                    schedule,
                    allOccupied,
                    placementByStart,
                    frozenStarts,
                    effectiveStart,
                    deepStart
                )
            );
        }

        // Deep work window [deepStart, deepEnd]
        entries.addAll(
            buildWindowEntries(
                schedule,
                allOccupied,
                placementByStart,
                frozenStarts,
                deepStart,
                deepEnd
            )
        );

        // Post-deep outer window [deepEnd, dayEnd]
        if (deepEnd.isBefore(schedulingDayEnd)) {
            entries.addAll(
                buildWindowEntries(
                    schedule,
                    allOccupied,
                    placementByStart,
                    frozenStarts,
                    deepEnd,
                    schedulingDayEnd
                )
            );
        }

        // FIXED tasks outside all managed windows (edge case)
        for (Placement p : allPlacements) {
            boolean inDay =
                !p.start().isBefore(effectiveStart) &&
                !p.end().isAfter(schedulingDayEnd);
            if (!inDay && !frozenStarts.contains(p.start())) {
                entries.add(
                    buildTaskEntry(schedule, p.task(), p.start(), p.end())
                );
            }
        }

        entries.sort(Comparator.comparing(DailyScheduleEntry::getStartTime));
        return entries;
    }

    private List<DailyScheduleEntry> buildWindowEntries(
        DailySchedule schedule,
        List<TimeSlot> allOccupied,
        Map<LocalTime, Placement> placementByStart,
        Set<LocalTime> frozenStarts,
        LocalTime windowStart,
        LocalTime windowEnd
    ) {
        List<DailyScheduleEntry> entries = new ArrayList<>();

        // Filter occupied slots that overlap with this window
        List<TimeSlot> windowSlots = allOccupied
            .stream()
            .filter(
                s ->
                    !s.end().isAfter(windowEnd) &&
                    !s.start().isBefore(windowStart)
            )
            .toList();

        LocalTime fill = windowStart;

        for (int i = 0; i < windowSlots.size(); i++) {
            TimeSlot slot = windowSlots.get(i);

            if (slot.start().isAfter(fill)) {
                entries.add(buildFreeEntry(schedule, fill, slot.start()));
            }

            if (frozenStarts.contains(slot.start())) {
                fill = slot.end();
                continue;
            }

            Placement p = placementByStart.get(slot.start());
            if (p == null) {
                fill = slot.end();
                continue;
            }

            entries.add(buildTaskEntry(schedule, p.task(), p.start(), p.end()));

            LocalTime nextStart = (i + 1 < windowSlots.size())
                ? windowSlots.get(i + 1).start()
                : windowEnd;
            LocalTime bufferEnd = minTime(
                p.end().plusMinutes(bufferMinutes),
                nextStart
            );
            if (bufferEnd.isAfter(p.end())) {
                entries.add(buildBufferEntry(schedule, p.end(), bufferEnd));
                fill = bufferEnd;
            } else {
                fill = p.end();
            }
        }

        if (fill.isBefore(windowEnd)) {
            entries.add(buildFreeEntry(schedule, fill, windowEnd));
        }

        return entries;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private LocalTime advancePastOccupied(
        LocalTime cursor,
        List<TimeSlot> occupied
    ) {
        LocalTime result = cursor;
        boolean advanced = true;
        while (advanced) {
            advanced = false;
            for (TimeSlot slot : occupied) {
                if (
                    !slot.start().isAfter(result) && slot.end().isAfter(result)
                ) {
                    result = slot.end();
                    advanced = true;
                    break;
                }
            }
        }
        return result;
    }

    private RecurrenceContext buildRecurrenceMap(
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
        StringBuilder sb = new StringBuilder(
            plan.dayNotes() != null ? plan.dayNotes() : ""
        );
        if (!plan.conflicts().isEmpty()) {
            sb.append("\n\n⚠️ ")
                .append(plan.conflicts().size())
                .append(
                    " task(s) with today's deadline couldn't fit in your schedule."
                );
        }
        return sb.toString();
    }

    private void logScheduleChanges(
        DailySchedule schedule,
        List<DailyScheduleEntry> oldEntries,
        List<DailyScheduleEntry> newEntries
    ) {
        Map<String, DailyScheduleEntry> oldByPid = oldEntries
            .stream()
            .filter(e -> e.getTask() != null)
            .collect(
                Collectors.toMap(
                    e -> e.getTask().getPid().toString(),
                    e -> e,
                    (a, b) -> a
                )
            );
        Map<String, DailyScheduleEntry> newByPid = newEntries
            .stream()
            .filter(e -> e.getTask() != null)
            .collect(
                Collectors.toMap(
                    e -> e.getTask().getPid().toString(),
                    e -> e,
                    (a, b) -> a
                )
            );

        List<ScheduleChange> changes = new ArrayList<>();

        for (Map.Entry<
            String,
            DailyScheduleEntry
        > entry : newByPid.entrySet()) {
            if (!oldByPid.containsKey(entry.getKey())) {
                changes.add(
                    ScheduleChange.builder()
                        .dailySchedule(schedule)
                        .task(entry.getValue().getTask())
                        .changeType(ScheduleChangeType.TASK_ADDED)
                        .reason("Added via new intention")
                        .build()
                );
            }
        }

        for (Map.Entry<
            String,
            DailyScheduleEntry
        > entry : newByPid.entrySet()) {
            DailyScheduleEntry old = oldByPid.get(entry.getKey());
            if (old != null) {
                boolean same =
                    old
                        .getStartTime()
                        .equals(entry.getValue().getStartTime()) &&
                    old.getEndTime().equals(entry.getValue().getEndTime());
                if (!same) {
                    changes.add(
                        ScheduleChange.builder()
                            .dailySchedule(schedule)
                            .task(entry.getValue().getTask())
                            .changeType(ScheduleChangeType.TIME_SHIFTED)
                            .previousValue(
                                "{\"startTime\":\"" +
                                    old.getStartTime() +
                                    "\",\"endTime\":\"" +
                                    old.getEndTime() +
                                    "\"}"
                            )
                            .newValue(
                                "{\"startTime\":\"" +
                                    entry.getValue().getStartTime() +
                                    "\",\"endTime\":\"" +
                                    entry.getValue().getEndTime() +
                                    "\"}"
                            )
                            .build()
                    );
                }
            }
        }

        if (!changes.isEmpty()) changeRepository.saveAll(changes);
    }

    // ─── Entry builders ────────────────────────────────────────────────────────

    private DailyScheduleEntry buildTaskEntry(
        DailySchedule schedule,
        Task task,
        LocalTime start,
        LocalTime end
    ) {
        return DailyScheduleEntry.builder()
            .dailySchedule(schedule)
            .task(task)
            .entryType(DailyScheduleEntryType.TASK)
            .startTime(start)
            .endTime(end)
            .build();
    }

    private DailyScheduleEntry buildBufferEntry(
        DailySchedule schedule,
        LocalTime start,
        LocalTime end
    ) {
        return DailyScheduleEntry.builder()
            .dailySchedule(schedule)
            .entryType(DailyScheduleEntryType.BUFFER)
            .startTime(start)
            .endTime(end)
            .build();
    }

    private DailyScheduleEntry buildFreeEntry(
        DailySchedule schedule,
        LocalTime start,
        LocalTime end
    ) {
        return DailyScheduleEntry.builder()
            .dailySchedule(schedule)
            .entryType(DailyScheduleEntryType.FREE)
            .startTime(start)
            .endTime(end)
            .notes("Free time — yours to use.")
            .build();
    }

    // ─── Logging ───────────────────────────────────────────────────────────────

    private void writeLlmLog(
        List<Message> messages,
        String rawOutput,
        int attempt
    ) {
        if (!llmLogEnabled) return;
        try {
            Path dir = Paths.get(logDir);
            Files.createDirectories(dir);
            String suffix = attempt > 0 ? "_retry" + attempt : "";
            String filename =
                LocalDateTime.now().format(FILE_FMT) +
                "_LlmDailyScheduler" +
                suffix +
                ".log";
            StringBuilder sb = new StringBuilder();
            sb.append("scheduler: LlmDailyScheduler\n");
            sb.append("model    : ").append(schedulingModel).append("\n");
            sb.append("attempt  : ").append(attempt).append("\n");
            sb.append("time     : ").append(LocalDateTime.now()).append("\n\n");
            for (Message msg : messages) {
                String role = switch (msg) {
                    case SystemMessage m -> "system";
                    case AssistantMessage m -> "assistant";
                    default -> "user";
                };
                sb.append("=== ").append(role.toUpperCase()).append(" ===\n");
                sb.append(msg.getText()).append("\n\n");
            }
            sb.append("=== RESPONSE ===\n").append(rawOutput).append("\n");
            Files.writeString(dir.resolve(filename), sb.toString());
        } catch (IOException e) {
            log.warn("Failed to write scheduler LLM log: {}", e.getMessage());
        }
    }

    // ─── DTO + utilities ───────────────────────────────────────────────────────

    private DailyScheduleDto toDto(
        DailySchedule schedule,
        List<DailyScheduleEntry> entries
    ) {
        return dtoMapper.toDailyScheduleDto(schedule, entries);
    }

    private static LocalTime maxTime(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime minTime(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}
