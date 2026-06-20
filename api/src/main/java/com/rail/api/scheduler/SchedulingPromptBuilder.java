package com.rail.api.scheduler;

import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.GoalRecurrence;
import com.rail.api.entity.GoalRecurrenceDay;
import com.rail.api.entity.MilestoneStatus;
import com.rail.api.entity.ScheduleChange;
import com.rail.api.entity.Task;
import com.rail.api.entity.UserConnieLog;
import com.rail.api.entity.UserConnieLogType;
import com.rail.api.entity.UserSchedulingProfile;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class SchedulingPromptBuilder {

    @Value("${rail.scheduling.history-limit:20}")
    private int historyLimit;

    @Value("${rail.scheduling.changes-limit:10}")
    private int changesLimit;

    String build(
        List<Task> flexibleTasks,
        List<Task> fixedTasks,
        UserSchedulingProfile profile,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart,
        LocalTime dayEnd,
        LocalDate date,
        DayOfWeek dayOfWeek,
        List<DailyScheduleEntry> frozenEntries,
        Map<Long, GoalRecurrenceDay> todayRecurrence,
        Set<Long> dailyGoalIds,
        List<Task> recentHistory,
        List<Task> recentSkips,
        List<ScheduleChange> recentChanges,
        boolean isRecompute,
        List<String> cycleFocusGoalTitles,
        int bufferMinutes,
        List<UserConnieLog> connieLogs,
        Map<Long, GoalRecurrence> dynamicRecurrenceMap,
        Map<Long, Integer> weeklyCountByTaskId
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
            dayEnd
        ).toMinutes();
        int outerMinutes = preDeepMinutes + postDeepMinutes;

        String deepOrderNote = switch (profile.getEnergyPattern()) {
            case MORNING_PEAK -> "MORNING_PEAK: place most demanding DEEP tasks early in the deep window (lower order numbers).";
            case EVENING_PEAK -> "EVENING_PEAK: place most demanding DEEP tasks later in the deep window (higher order numbers).";
            default -> "CONSISTENT: order DEEP tasks by priority and deadline.";
        };

        return """
        You are Connie, Rail's intelligence layer. In this session you are the Daily Scheduler — your role is to select and order tasks to build the best possible day for the user.

        ═══ SCHEDULING WINDOWS ═══
        Date: %s, %s
        Energy pattern: %s
        Effective start time: %s → %s
        Times before the effective start time either means it has already been scheduled/frozen for other tasks, or this is when the user woke up today. So DO NOT schedule anything before this time!

        DEEP WORK WINDOW  %s → %s  (%d min)
          Purpose: focused, cognitively demanding work.
          Allowed: DEEP tasks (first priority), LIGHT tasks (only if room remains after all DEEP placed).
          ADMIN tasks are NEVER placed here.

        OUTER WINDOW  (everything outside the deep work window within the waking day)
        %s  Post-deep: %s → %s  (%d min)
          Total outer capacity: %d min
          Purpose: lighter execution work outside the focus window.
          Allowed: LIGHT tasks (first priority), ADMIN tasks (fills remaining slots).

        Ordering note — %s

        ═══ FIXED TASKS (pre-placed — do NOT include in selected[]) ═══
        %s
        ═══ FLEXIBLE TASK POOL (%d available) ═══
        %s
        ═══ WEEKLY RECURRENCE STATUS ═══
        %s
        ═══ ALREADY DONE TODAY ═══
        %s
        ═══ COMPLETION HISTORY (last 14 days) ═══
        %s
        ═══ SKIPPED / MISSED HISTORY (last 14 days) ═══
        %s
        ═══ RECENT PLAN CHANGES (last 7 days) ═══
        %s%s
        ═══ CONNIE'S KNOWLEDGE OF THIS USER ═══
        %s
        ═══ RULES ═══
        1. ADMIN tasks → energyZone = "OUTER" always. Never in DEEP_WINDOW.
        2. DEEP tasks → energyZone = "DEEP_WINDOW" always.
        3. LIGHT tasks → "DEEP_WINDOW" if room remains after all DEEP tasks are placed; otherwise "OUTER".
        4. FIXED tasks are already placed outside this plan — do NOT include them in selected[].
        5. Tasks with deadline = today → mustScheduleToday = true.
        6. Deep window capacity: total DEEP_WINDOW task duration + (%dmin × count) must not exceed %dmin.
        7. Outer window capacity: total OUTER task duration + (%dmin × count) must not exceed %dmin.
        8. Priority order when capacity is limited: CRITICAL > HIGH > MEDIUM > LOW, then closer deadlines first.
        9. Deadline = today tasks that cannot fit in either window → add to conflicts[].
        10. dayNotes: exactly 1 warm sentence — no more. Reference specific task or goal names. Sound like a thoughtful friend setting the tone for the day.
        %s
        ═══ OUTPUT — STRICT JSON ═══
        {
          "selected": [
            {"taskPid": "<exact pid from flexible pool>", "order": 1, "energyZone": "DEEP_WINDOW|OUTER", "mustScheduleToday": true|false, "schedulingNote": "one line why"}
          ],
          "conflicts": [
            {"taskPid": "...", "reason": "Deadline today but both windows are full"}
          ],
          "dayNotes": "..."
        }

        Only use taskPid values that appear in the flexible task pool above. No fabrication.
        """.formatted(
            date,
            dayOfWeek,
            profile.getEnergyPattern(),
            effectiveStart,
            dayEnd,
            deepStart,
            deepEnd,
            deepMinutes,
            preDeepMinutes > 0
                ? "  Pre-deep:  %s → %s  (%d min)\n".formatted(
                      effectiveStart,
                      deepStart,
                      preDeepMinutes
                  )
                : "",
            deepEnd,
            dayEnd,
            postDeepMinutes,
            outerMinutes,
            deepOrderNote,
            buildFixedTasksSection(fixedTasks),
            flexibleTasks.size(),
            buildFlexibleTasksSection(
                flexibleTasks,
                date,
                todayRecurrence,
                dailyGoalIds
            ),
            buildWeeklyRecurrenceSection(flexibleTasks, date, dynamicRecurrenceMap, weeklyCountByTaskId),
            buildFrozenSection(frozenEntries),
            buildHistorySection(recentHistory),
            buildSkipsSection(recentSkips),
            buildChangesSection(recentChanges),
            buildCycleFocusSection(cycleFocusGoalTitles),
            buildConnieLogsSection(connieLogs),
            bufferMinutes,
            deepMinutes,
            bufferMinutes,
            outerMinutes,
            isRecompute && !frozenEntries.isEmpty()
                ? "    dayNotes must acknowledge work already done today and welcome the new tasks — still exactly 1 sentence.\n"
                : ""
        );
    }

    private String buildFixedTasksSection(List<Task> fixedTasks) {
        if (fixedTasks.isEmpty()) return "None.\n";
        return fixedTasks
            .stream()
            .map(t ->
                "[%s] %s — fixed at %s (%dmin)\n".formatted(
                    t.getPid(),
                    t.getTitle(),
                    t.getFixedTime(),
                    t.getDurationMinutes() != null ? t.getDurationMinutes() : 60
                )
            )
            .collect(Collectors.joining());
    }

    private String buildFlexibleTasksSection(
        List<Task> tasks,
        LocalDate date,
        Map<Long, GoalRecurrenceDay> todayRecurrence,
        Set<Long> dailyGoalIds
    ) {
        if (tasks.isEmpty()) return "(none)\n";
        return tasks.stream()
            .map(task -> buildTaskEntry(task, date, todayRecurrence, dailyGoalIds))
            .collect(Collectors.joining());
    }

    private String buildTaskEntry(
        Task task,
        LocalDate date,
        Map<Long, GoalRecurrenceDay> todayRecurrence,
        Set<Long> dailyGoalIds
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("\n[%s] %s".formatted(task.getPid(), task.getTitle()));
        lines.add("  Goal: \"%s\" | Type: %s | Energy: %s".formatted(
            task.getGoal().getTitle(), task.getGoal().getType(), task.getGoal().getEnergyLevel()));
        lines.add("  Priority: %s | Duration: %dmin | Flexibility: %s".formatted(
            task.getPriority(),
            task.getDurationMinutes() != null ? task.getDurationMinutes() : 60,
            task.getFlexibility()));

        if (task.getDeadline() != null) {
            lines.add("  Deadline: %s%s".formatted(
                task.getDeadline(), task.getDeadline().equals(date) ? " ⚠️ TODAY" : ""));
        }
        if (task.getGoal().getTargetDate() != null) {
            lines.add("  Goal target date: %s".formatted(task.getGoal().getTargetDate()));
        }
        if (task.getNotes() != null && !task.getNotes().isBlank()) {
            lines.add("  Notes: %s".formatted(task.getNotes()));
        }

        GoalRecurrenceDay recDay = todayRecurrence.get(task.getGoal().getId());
        if (recDay != null) {
            String timeHint = recDay.getPreferredTime() != null
                ? " at %s".formatted(recDay.getPreferredTime()) : "";
            lines.add("  Recurring today%s".formatted(timeHint));
        } else if (dailyGoalIds.contains(task.getGoal().getId())) {
            lines.add("  Recurring today (daily habit)");
        }

        if (task.getMilestone() != null) {
            String currentMarker = task.getMilestone().getStatus() == MilestoneStatus.IN_PROGRESS
                ? " ← current milestone" : "";
            lines.add("  Milestone: \"%s\" (position %d%s)".formatted(
                task.getMilestone().getTitle(), task.getMilestone().getPosition(), currentMarker));
        }

        return lines.stream().collect(Collectors.joining("\n")) + "\n";
    }

    private String buildWeeklyRecurrenceSection(
        List<Task> flexibleTasks,
        LocalDate date,
        Map<Long, GoalRecurrence> dynamicRecurrenceMap,
        Map<Long, Integer> weeklyCountByTaskId
    ) {
        if (dynamicRecurrenceMap.isEmpty()) return "No dynamic-day habits in today's pool.\n";

        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        // +1 to include today itself as a remaining scheduling day
        int daysRemaining = (int) (weekEnd.toEpochDay() - date.toEpochDay()) + 1;

        return flexibleTasks.stream()
            .filter(t -> dynamicRecurrenceMap.containsKey(t.getGoal().getId()))
            .map(t -> {
                GoalRecurrence rec = dynamicRecurrenceMap.get(t.getGoal().getId());
                int quota = rec.getTimesPerPeriod() != null ? rec.getTimesPerPeriod() : 1;
                int done = weeklyCountByTaskId.getOrDefault(t.getId(), 0);
                int remaining = Math.max(0, quota - done);
                String urgency = remaining > 0 && daysRemaining <= remaining
                    ? " ⚠️ MUST schedule today to meet weekly quota" : "";
                return "  \"%s\" — quota: %dx/week | scheduled so far: %d | still needed: %d | days left in week: %d%s"
                    .formatted(t.getTitle(), quota, done, remaining, daysRemaining, urgency);
            })
            .collect(Collectors.joining("\n")) + "\n";
    }

    private String buildFrozenSection(List<DailyScheduleEntry> frozenEntries) {
        if (frozenEntries.isEmpty()) return "None — fresh schedule.\n";
        return (
            "Frozen — do not reassign their slots:\n" +
            frozenEntries
                .stream()
                .map(e ->
                    "  %s–%s: %s [DONE]\n".formatted(
                        e.getStartTime(),
                        e.getEndTime(),
                        e.getTask() != null
                            ? "\"" + e.getTask().getTitle() + "\""
                            : "(unknown)"
                    )
                )
                .collect(Collectors.joining())
        );
    }

    private String buildHistorySection(List<Task> recentHistory) {
        if (
            recentHistory.isEmpty()
        ) return "No history yet — new user. Prefer shorter tasks and conservative estimates.\n";
        return recentHistory
            .stream()
            .limit(historyLimit)
            .map(t -> {
                String actual = (t.getStartedAt() != null &&
                    t.getEndedAt() != null)
                    ? Duration.between(
                          t.getStartedAt(),
                          t.getEndedAt()
                      ).toMinutes() + "min actual"
                    : "duration unrecorded";
                return "  - \"%s\" (%s) — %s, est %smin, %s\n".formatted(
                    t.getTitle(),
                    t.getGoal().getTitle(),
                    t.getCompletionType() != null
                        ? t.getCompletionType()
                        : "FULL",
                    t.getDurationMinutes() != null
                        ? t.getDurationMinutes()
                        : "?",
                    actual
                );
            })
            .collect(Collectors.joining());
    }

    private String buildSkipsSection(List<Task> recentSkips) {
        if (recentSkips == null || recentSkips.isEmpty())
            return "No recent skips or misses.\n";
        return recentSkips.stream()
            .limit(historyLimit)
            .map(t -> "  - \"%s\" (%s) — %s%s\n".formatted(
                t.getTitle(),
                t.getGoal().getTitle(),
                t.getStatus(),
                t.getMissReason() != null ? ": " + t.getMissReason() : ""
            ))
            .collect(Collectors.joining());
    }

    private String buildChangesSection(List<ScheduleChange> recentChanges) {
        if (
            recentChanges.isEmpty()
        ) return "No recent changes — plan is stable.\n";
        return recentChanges
            .stream()
            .limit(changesLimit)
            .map(sc ->
                "  - %s%s\n".formatted(
                    sc.getChangeType(),
                    sc.getReason() != null ? ": " + sc.getReason() : ""
                )
            )
            .collect(Collectors.joining());
    }

    private String buildConnieLogsSection(List<UserConnieLog> logs) {
        if (logs == null || logs.isEmpty()) return "No patterns recorded yet.\n";
        return logs.stream()
            .map(log -> {
                String label = log.getType() == UserConnieLogType.EVOLUTIONARY
                    ? "Evolutionary summary (%s → %s, %d entries merged)".formatted(
                        log.getPeriodStart(), log.getPeriodEnd(),
                        log.getMergedCount() != null ? log.getMergedCount() : 0)
                    : "Daily analysis (%s)".formatted(log.getPeriodStart());
                List<String> parts = new ArrayList<>();
                parts.add("  [%s]".formatted(label));
                if (log.getObservedPatterns() != null && !log.getObservedPatterns().isBlank())
                    parts.add("  Observed: %s".formatted(log.getObservedPatterns().strip()));
                if (log.getStatedPreferences() != null && !log.getStatedPreferences().isBlank())
                    parts.add("  Stated: %s".formatted(log.getStatedPreferences().strip()));
                return parts.stream().collect(Collectors.joining("\n"));
            })
            .collect(Collectors.joining("\n\n")) + "\n";
    }

    private String buildCycleFocusSection(List<String> cycleFocusGoalTitles) {
        if (cycleFocusGoalTitles.isEmpty()) return "";
        String titles = cycleFocusGoalTitles
            .stream()
            .map(t -> "  - \"%s\"\n".formatted(t))
            .collect(Collectors.joining());
        return """

        ═══ ACTIVE CYCLE FOCUS ═══
        The user is in an active cycle. Their focus goals are:
        %sWhen capacity is limited and tasks compete for the same window, prefer tasks belonging to these focus goals. Non-focus goal tasks are still scheduled but yield to focus tasks when slots are tight.
        """.formatted(titles);
    }
}
