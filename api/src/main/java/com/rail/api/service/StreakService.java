package com.rail.api.service;

import com.rail.api.dto.HabitStatsDto;
import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalRecurrence;
import com.rail.api.entity.GoalRecurrenceFrequency;
import com.rail.api.entity.OccurrenceStatus;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskOccurrence;
import com.rail.api.repository.GoalRecurrenceDayRepository;
import com.rail.api.repository.GoalRecurrenceRepository;
import com.rail.api.repository.TaskOccurrenceRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final TaskRepository taskRepository;
    private final TaskOccurrenceRepository occurrenceRepository;
    private final GoalRecurrenceRepository recurrenceRepository;
    private final GoalRecurrenceDayRepository recurrenceDayRepository;
    private final UserSchedulingProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public HabitStatsDto computeStats(Goal goal) {
        var owner = goal.getIntention().getOwner();
        ZoneId zone = profileRepository
            .findByUser(owner)
            .map(p -> ZoneId.of(p.getTimezone()))
            .orElse(ZoneId.of("UTC"));
        LocalDate today = LocalDate.now(zone);
        List<Task> tasks = taskRepository.findByGoal(goal);
        if (tasks.isEmpty()) return empty(today);

        List<TaskOccurrence> allOccurrences =
            occurrenceRepository.findByTaskInOrderByOccurrenceDateDesc(tasks);

        GoalRecurrence recurrence = recurrenceRepository
            .findByGoal(goal)
            .orElse(null);
        Set<DayOfWeek> scheduledDays = scheduledDays(recurrence);
        int timesPerPeriod =
            recurrence != null ? recurrence.getTimesPerPeriod() : 7;
        GoalRecurrenceFrequency frequency =
            recurrence != null
                ? recurrence.getFrequency()
                : GoalRecurrenceFrequency.DAILY;

        Map<LocalDate, OccurrenceStatus> statusByDate = allOccurrences
            .stream()
            .collect(
                Collectors.toMap(
                    TaskOccurrence::getOccurrenceDate,
                    TaskOccurrence::getStatus,
                    (a, b) -> a == OccurrenceStatus.DONE ? a : b
                )
            );

        boolean weeklyMode =
            frequency == GoalRecurrenceFrequency.WEEKLY &&
            scheduledDays.isEmpty();

        int currentStreak = weeklyMode
            ? computeWeeklyCurrentStreak(statusByDate, timesPerPeriod, today)
            : computeDailyCurrentStreak(
                  statusByDate,
                  scheduledDays,
                  today,
                  frequency == GoalRecurrenceFrequency.DAILY
              );

        int bestStreak = weeklyMode
            ? computeWeeklyBestStreak(statusByDate, timesPerPeriod, today)
            : computeDailyBestStreak(
                  statusByDate,
                  scheduledDays,
                  today,
                  frequency == GoalRecurrenceFrequency.DAILY
              );

        LocalDate weekStart = today.with(
            WeekFields.of(Locale.getDefault()).dayOfWeek(),
            1
        );
        int thisWeekDone = 0,
            missedThisWeek = 0;
        List<String> weekDots = new ArrayList<>();
        LocalDate goalCreatedAt = goal.getCreatedAt().atZone(zone).toLocalDate();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            if (day.isBefore(goalCreatedAt)) continue;

            boolean isScheduled =
                scheduledDays.isEmpty() ||
                scheduledDays.contains(day.getDayOfWeek());
            if (day.isAfter(today)) {
                weekDots.add("UPCOMING");
            } else if (day.equals(today)) {
                OccurrenceStatus s = statusByDate.get(today);
                if (s == OccurrenceStatus.DONE) {
                    weekDots.add("DONE");
                    if (isScheduled) thisWeekDone++;
                } else {
                    weekDots.add("TODAY");
                }
            } else {
                OccurrenceStatus s = statusByDate.get(day);
                if (s == OccurrenceStatus.DONE) {
                    weekDots.add("DONE");
                    if (isScheduled) thisWeekDone++;
                } else if (isScheduled) {
                    weekDots.add("MISS");
                    missedThisWeek++;
                } else {
                    weekDots.add("UPCOMING");
                }
            }
        }

        List<String> monthDots = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            boolean isScheduled =
                scheduledDays.isEmpty() ||
                scheduledDays.contains(day.getDayOfWeek());
            if (day.isAfter(today)) {
                monthDots.add("UPCOMING");
            } else if (day.equals(today)) {
                OccurrenceStatus s = statusByDate.get(today);
                monthDots.add(s == OccurrenceStatus.DONE ? "DONE" : "TODAY");
            } else {
                OccurrenceStatus s = statusByDate.get(day);
                if (s == OccurrenceStatus.DONE) {
                    monthDots.add("DONE");
                } else if (isScheduled) {
                    monthDots.add("MISS");
                } else {
                    monthDots.add("UPCOMING");
                }
            }
        }

        long totalScheduledPast = countScheduledDaysPast(
            scheduledDays,
            today,
            allOccurrences
        );
        long totalDone = allOccurrences
            .stream()
            .filter(o -> o.getStatus() == OccurrenceStatus.DONE)
            .count();
        double allTimeRate =
            totalScheduledPast > 0
                ? (double) totalDone / totalScheduledPast
                : 0.0;

        int scheduledThisWeek = (int) weekDots
            .stream()
            .filter(d -> !d.equals("UPCOMING"))
            .count();
        double thisWeekRate =
            scheduledThisWeek > 0
                ? (double) thisWeekDone / scheduledThisWeek
                : 0.0;

        int slipsTotal = (int) allOccurrences
            .stream()
            .filter(o -> o.getStatus() == OccurrenceStatus.MISSED)
            .count();

        return new HabitStatsDto(
            currentStreak,
            bestStreak,
            thisWeekDone,
            timesPerPeriod,
            missedThisWeek,
            thisWeekRate,
            allTimeRate,
            weekDots,
            monthDots,
            slipsTotal
        );
    }

    // ── Day-based streak (DAILY frequency or WEEKLY with specific days) ──────────

    private int computeDailyCurrentStreak(
        Map<LocalDate, OccurrenceStatus> statusByDate,
        Set<DayOfWeek> scheduledDays,
        LocalDate today,
        boolean graceAllowed
    ) {
        int streak = 0;
        boolean graceUsed = false;
        for (int i = 0; i < 365; i++) {
            LocalDate day = today.minusDays(i + 1);
            boolean isScheduled =
                scheduledDays.isEmpty() ||
                scheduledDays.contains(day.getDayOfWeek());
            if (!isScheduled) continue;
            if (statusByDate.getOrDefault(day, null) == OccurrenceStatus.DONE) {
                streak++;
                graceUsed = false; // grace resets after a successful day
            } else if (graceAllowed && !graceUsed) {
                graceUsed = true; // forgive one miss, don't increment
            } else {
                break;
            }
        }
        if (
            statusByDate.getOrDefault(today, null) == OccurrenceStatus.DONE
        ) streak++;
        return streak;
    }

    private int computeDailyBestStreak(
        Map<LocalDate, OccurrenceStatus> statusByDate,
        Set<DayOfWeek> scheduledDays,
        LocalDate today,
        boolean graceAllowed
    ) {
        if (statusByDate.isEmpty()) return 0;
        LocalDate earliest = statusByDate
            .keySet()
            .stream()
            .min(LocalDate::compareTo)
            .orElse(today);
        int best = 0,
            current = 0;
        boolean graceUsed = false;
        LocalDate cursor = earliest;
        while (!cursor.isAfter(today)) {
            boolean isScheduled =
                scheduledDays.isEmpty() ||
                scheduledDays.contains(cursor.getDayOfWeek());
            if (!isScheduled) {
                cursor = cursor.plusDays(1);
                continue;
            }
            if (
                statusByDate.getOrDefault(cursor, null) == OccurrenceStatus.DONE
            ) {
                current++;
                best = Math.max(best, current);
                graceUsed = false;
            } else if (graceAllowed && !graceUsed) {
                graceUsed = true;
            } else {
                current = 0;
                graceUsed = false;
            }
            cursor = cursor.plusDays(1);
        }
        return best;
    }

    // ── Week-based streak (WEEKLY frequency, no specific days) ──────────────────

    private int computeWeeklyCurrentStreak(
        Map<LocalDate, OccurrenceStatus> statusByDate,
        int timesPerPeriod,
        LocalDate today
    ) {
        WeekFields wf = WeekFields.of(Locale.getDefault());
        LocalDate weekStart = today.with(wf.dayOfWeek(), 1);
        int streak = 0;

        // Count current week
        if (
            doneCountInWeek(statusByDate, weekStart, today) >= timesPerPeriod
        ) streak++;

        // Walk back through prior complete weeks
        for (int w = 1; w <= 52; w++) {
            LocalDate prevStart = weekStart.minusWeeks(w);
            LocalDate prevEnd = prevStart.plusDays(6);
            if (
                doneCountInWeek(statusByDate, prevStart, prevEnd) >=
                timesPerPeriod
            ) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private int computeWeeklyBestStreak(
        Map<LocalDate, OccurrenceStatus> statusByDate,
        int timesPerPeriod,
        LocalDate today
    ) {
        if (statusByDate.isEmpty()) return 0;
        WeekFields wf = WeekFields.of(Locale.getDefault());
        LocalDate earliest = statusByDate
            .keySet()
            .stream()
            .min(LocalDate::compareTo)
            .orElse(today);
        LocalDate firstWeekStart = earliest.with(wf.dayOfWeek(), 1);
        LocalDate currentWeekStart = today.with(wf.dayOfWeek(), 1);
        int best = 0,
            current = 0;
        LocalDate cursor = firstWeekStart;
        while (!cursor.isAfter(currentWeekStart)) {
            LocalDate end = cursor.equals(currentWeekStart)
                ? today
                : cursor.plusDays(6);
            if (doneCountInWeek(statusByDate, cursor, end) >= timesPerPeriod) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 0;
            }
            cursor = cursor.plusWeeks(1);
        }
        return best;
    }

    private int doneCountInWeek(
        Map<LocalDate, OccurrenceStatus> statusByDate,
        LocalDate start,
        LocalDate end
    ) {
        int count = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (
                statusByDate.getOrDefault(d, null) == OccurrenceStatus.DONE
            ) count++;
            d = d.plusDays(1);
        }
        return count;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private long countScheduledDaysPast(
        Set<DayOfWeek> scheduledDays,
        LocalDate today,
        List<TaskOccurrence> allOccurrences
    ) {
        if (allOccurrences.isEmpty()) return 0;
        LocalDate earliest = allOccurrences
            .stream()
            .map(TaskOccurrence::getOccurrenceDate)
            .min(LocalDate::compareTo)
            .orElse(today);
        long count = 0;
        LocalDate cursor = earliest;
        while (!cursor.isAfter(today)) {
            if (
                scheduledDays.isEmpty() ||
                scheduledDays.contains(cursor.getDayOfWeek())
            ) count++;
            cursor = cursor.plusDays(1);
        }
        return count;
    }

    private Set<DayOfWeek> scheduledDays(GoalRecurrence recurrence) {
        if (recurrence == null) return Set.of();
        if (
            recurrence.getFrequency() == GoalRecurrenceFrequency.DAILY
        ) return Set.of();
        return recurrenceDayRepository
            .findByGoalRecurrence(recurrence)
            .stream()
            .map(rd -> rd.getDayOfWeek())
            .collect(Collectors.toSet());
    }

    public boolean isScheduledToday(Goal goal, DayOfWeek today) {
        GoalRecurrence rec = recurrenceRepository.findByGoal(goal).orElse(null);
        if (rec == null) return false;
        if (rec.getFrequency() == GoalRecurrenceFrequency.DAILY) return true;
        return recurrenceDayRepository
            .findByGoalRecurrence(rec)
            .stream()
            .anyMatch(rd -> rd.getDayOfWeek() == today);
    }

    private HabitStatsDto empty(LocalDate today) {
        WeekFields wf = WeekFields.of(Locale.getDefault());
        LocalDate weekStart = today.with(wf.dayOfWeek(), 1);
        List<String> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            week.add(d.equals(today) ? "TODAY" : "UPCOMING");
        }
        List<String> month = new ArrayList<>();
        for (int i = 0; i < 30; i++) month.add("UPCOMING");
        return new HabitStatsDto(0, 0, 0, 7, 0, 0.0, 0.0, week, month, 0);
    }
}
