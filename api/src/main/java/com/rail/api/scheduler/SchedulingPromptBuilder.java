package com.rail.api.scheduler;

import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.GoalRecurrenceDay;
import com.rail.api.entity.MilestoneStatus;
import com.rail.api.entity.ScheduleChange;
import com.rail.api.entity.Task;
import com.rail.api.entity.UserSchedulingProfile;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class SchedulingPromptBuilder {

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
        List<ScheduleChange> recentChanges,
        boolean isRecompute,
        List<String> cycleFocusGoalTitles,
        int bufferMinutes
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
        You are Rail's intelligent daily scheduler. Select and order tasks to build the best possible day.

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
        ═══ ALREADY DONE TODAY ═══
        %s
        ═══ COMPLETION HISTORY (last 14 days) ═══
        %s
        ═══ RECENT PLAN CHANGES (last 7 days) ═══
        %s%s
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
            buildFrozenSection(frozenEntries),
            buildHistorySection(recentHistory),
            buildChangesSection(recentChanges),
            buildCycleFocusSection(cycleFocusGoalTitles),
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
        return tasks
            .stream()
            .map(task -> {
                var sb = new StringBuilder();
                sb.append(
                    "\n[%s] %s\n".formatted(task.getPid(), task.getTitle())
                );
                sb.append(
                    "  Goal: \"%s\" | Type: %s | Energy: %s\n".formatted(
                        task.getGoal().getTitle(),
                        task.getGoal().getType(),
                        task.getGoal().getEnergyLevel()
                    )
                );
                sb.append(
                    "  Priority: %s | Duration: %dmin | Flexibility: %s\n".formatted(
                        task.getPriority(),
                        task.getDurationMinutes() != null
                            ? task.getDurationMinutes()
                            : 60,
                        task.getFlexibility()
                    )
                );

                if (task.getDeadline() != null) {
                    sb.append(
                        "  Deadline: %s%s\n".formatted(
                            task.getDeadline(),
                            task.getDeadline().equals(date) ? " ⚠️ TODAY" : ""
                        )
                    );
                }
                if (task.getGoal().getTargetDate() != null) {
                    sb.append(
                        "  Goal target date: %s\n".formatted(
                            task.getGoal().getTargetDate()
                        )
                    );
                }
                if (task.getNotes() != null && !task.getNotes().isBlank()) {
                    sb.append("  Notes: %s\n".formatted(task.getNotes()));
                }

                GoalRecurrenceDay recDay = todayRecurrence.get(
                    task.getGoal().getId()
                );
                if (recDay != null) {
                    sb.append("  Recurring today");
                    if (recDay.getPreferredTime() != null) sb.append(
                        " at %s".formatted(recDay.getPreferredTime())
                    );
                    sb.append("\n");
                } else if (dailyGoalIds.contains(task.getGoal().getId())) {
                    sb.append("  Recurring today (daily habit)\n");
                }

                if (task.getMilestone() != null) {
                    sb.append(
                        "  Milestone: \"%s\" (position %d%s)\n".formatted(
                            task.getMilestone().getTitle(),
                            task.getMilestone().getPosition(),
                            task.getMilestone().getStatus() ==
                                MilestoneStatus.IN_PROGRESS
                                ? ") ← current milestone"
                                : ""
                        )
                    );
                }

                return sb.toString();
            })
            .collect(Collectors.joining());
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
            .limit(20)
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

    private String buildChangesSection(List<ScheduleChange> recentChanges) {
        if (
            recentChanges.isEmpty()
        ) return "No recent changes — plan is stable.\n";
        return recentChanges
            .stream()
            .limit(10)
            .map(sc ->
                "  - %s%s\n".formatted(
                    sc.getChangeType(),
                    sc.getReason() != null ? ": " + sc.getReason() : ""
                )
            )
            .collect(Collectors.joining());
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
