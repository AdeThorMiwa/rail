package com.rail.api.connie;

import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.DailyScheduleEntryStatus;
import com.rail.api.entity.EnergyLevel;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.User;
import com.rail.api.repository.DailyScheduleEntryRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlgorithmicPatternAnalyzer {

    private static final LocalTime NOON = LocalTime.of(12, 0);
    private static final LocalTime EVENING_START = LocalTime.of(17, 0);

    @Value("${rail.connie.pattern-analysis.analysis-days:30}")
    private int analysisDays;

    private final DailyScheduleEntryRepository entryRepository;

    public AlgorithmicMetrics analyze(User user) {
        LocalDate since = LocalDate.now().minusDays(analysisDays);
        List<DailyScheduleEntry> entries = entryRepository.findTaskEntriesByUserSince(user, since);

        if (entries.isEmpty()) return emptyMetrics();

        Map<DayOfWeek, int[]> dayBuckets = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) dayBuckets.put(d, new int[3]);

        int[] morning = new int[2], afternoon = new int[2], evening = new int[2];

        Map<EnergyLevel, int[]> energyBuckets = new EnumMap<>(EnergyLevel.class);
        for (EnergyLevel e : EnergyLevel.values()) energyBuckets.put(e, new int[2]);

        Map<GoalType, int[]> goalTypeBuckets = new EnumMap<>(GoalType.class);
        for (GoalType t : GoalType.values()) goalTypeBuckets.put(t, new int[2]);

        Map<LocalDate, int[]> densityByDate = new HashMap<>();
        Map<String, Integer> skipReasonCounts = new HashMap<>();

        for (DailyScheduleEntry entry : entries) {
            LocalDate date = entry.getDailySchedule().getScheduledDate();
            DayOfWeek dow = date.getDayOfWeek();
            LocalTime start = entry.getStartTime();
            boolean completed = entry.getStatus() == DailyScheduleEntryStatus.COMPLETED;
            boolean missed = entry.getStatus() == DailyScheduleEntryStatus.MISSED;
            boolean skipped = entry.getStatus() == DailyScheduleEntryStatus.SKIPPED;

            int[] day = dayBuckets.get(dow);
            day[0]++;
            if (completed) day[1]++;
            if (missed || skipped) day[2]++;

            if (start != null) {
                int[] bucket = start.isBefore(NOON) ? morning
                    : start.isBefore(EVENING_START) ? afternoon : evening;
                bucket[0]++;
                if (completed) bucket[1]++;
            }

            if (entry.getTask() != null) {
                EnergyLevel el = entry.getTask().getGoal() != null
                    ? entry.getTask().getGoal().getEnergyLevel() : null;
                if (el != null) {
                    energyBuckets.get(el)[0]++;
                    if (completed) energyBuckets.get(el)[1]++;
                }

                GoalType gt = entry.getTask().getGoal() != null
                    ? entry.getTask().getGoal().getType() : null;
                if (gt != null) {
                    goalTypeBuckets.get(gt)[0]++;
                    if (completed) goalTypeBuckets.get(gt)[1]++;
                }

                if ((missed || skipped) && entry.getTask().getMissReason() != null) {
                    skipReasonCounts.merge(entry.getTask().getMissReason().trim(), 1, Integer::sum);
                }
            }

            densityByDate.computeIfAbsent(date, k -> new int[2]);
            densityByDate.get(date)[0]++;
            if (completed) densityByDate.get(date)[1]++;
        }

        Map<DayOfWeek, AlgorithmicMetrics.DayMetrics> dayMetrics = new EnumMap<>(DayOfWeek.class);
        dayBuckets.forEach((dow, b) -> {
            if (b[0] > 0) dayMetrics.put(dow, new AlgorithmicMetrics.DayMetrics(b[0], b[1], b[2]));
        });

        int[] light = new int[2], moderate = new int[2], heavy = new int[2];
        densityByDate.forEach((date, b) -> {
            int[] bucket = b[0] <= 3 ? light : b[0] <= 5 ? moderate : heavy;
            bucket[0] += b[0];
            bucket[1] += b[1];
        });

        String topSkips = skipReasonCounts.isEmpty() ? "None recorded"
            : skipReasonCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> "%s (%d×)".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));

        return new AlgorithmicMetrics(
            densityByDate.size(),
            entries.size(),
            dayMetrics,
            new AlgorithmicMetrics.TimeWindowMetrics(rate(morning), rate(afternoon), rate(evening)),
            toRateMap(energyBuckets),
            toGoalTypeRateMap(goalTypeBuckets),
            new AlgorithmicMetrics.DensityMetrics(rate(light), rate(moderate), rate(heavy)),
            topSkips
        );
    }

    public String format(AlgorithmicMetrics m) {
        if (m.daysAnalyzed() == 0) {
            return "Insufficient data — user is new or has no schedule history yet.";
        }

        String dayLines = m.byDayOfWeek().entrySet().stream()
            .filter(e -> e.getValue().scheduled() > 0)
            .map(e -> "  %s: %s (%d tasks)".formatted(
                e.getKey(), pct(e.getValue().completionRate()), e.getValue().scheduled()))
            .collect(Collectors.joining("\n"));

        String energyLines = m.byEnergyLevel().entrySet().stream()
            .map(e -> "  %s: %s (%d tasks)".formatted(
                e.getKey(), pct(e.getValue().rate()), e.getValue().scheduled()))
            .collect(Collectors.joining("\n"));

        String goalTypeLines = m.byGoalType().entrySet().stream()
            .map(e -> "  %s: %s (%d tasks)".formatted(
                e.getKey(), pct(e.getValue().rate()), e.getValue().scheduled()))
            .collect(Collectors.joining("\n"));

        return """
            Days analyzed: %d (of last %d) | Tasks scheduled: %d

            Day-of-week completion rates:
            %s

            Time-window completion rates:
              Morning (before 12:00): %s (%d tasks)
              Afternoon (12:00–17:00): %s (%d tasks)
              Evening (after 17:00): %s (%d tasks)

            Energy level completion rates:
            %s

            Goal type completion rates:
            %s

            Schedule density impact:
              Light days (1–3 tasks): %s (%d tasks)
              Moderate days (4–5 tasks): %s (%d tasks)
              Heavy days (6+ tasks): %s (%d tasks)

            Top skip/miss reasons: %s
            """.formatted(
                m.daysAnalyzed(), analysisDays, m.totalScheduledTasks(),
                dayLines,
                pct(m.byTimeWindow().morning().rate()), m.byTimeWindow().morning().scheduled(),
                pct(m.byTimeWindow().afternoon().rate()), m.byTimeWindow().afternoon().scheduled(),
                pct(m.byTimeWindow().evening().rate()), m.byTimeWindow().evening().scheduled(),
                energyLines,
                goalTypeLines,
                pct(m.byScheduleDensity().light().rate()), m.byScheduleDensity().light().scheduled(),
                pct(m.byScheduleDensity().moderate().rate()), m.byScheduleDensity().moderate().scheduled(),
                pct(m.byScheduleDensity().heavy().rate()), m.byScheduleDensity().heavy().scheduled(),
                m.topSkipReasons()
            ).strip();
    }

    private AlgorithmicMetrics.CompletionRate rate(int[] b) {
        return new AlgorithmicMetrics.CompletionRate(b[0], b[1]);
    }

    private Map<EnergyLevel, AlgorithmicMetrics.CompletionRate> toRateMap(Map<EnergyLevel, int[]> m) {
        Map<EnergyLevel, AlgorithmicMetrics.CompletionRate> result = new EnumMap<>(EnergyLevel.class);
        m.forEach((k, b) -> { if (b[0] > 0) result.put(k, new AlgorithmicMetrics.CompletionRate(b[0], b[1])); });
        return result;
    }

    private Map<GoalType, AlgorithmicMetrics.CompletionRate> toGoalTypeRateMap(Map<GoalType, int[]> m) {
        Map<GoalType, AlgorithmicMetrics.CompletionRate> result = new EnumMap<>(GoalType.class);
        m.forEach((k, b) -> { if (b[0] > 0) result.put(k, new AlgorithmicMetrics.CompletionRate(b[0], b[1])); });
        return result;
    }

    private AlgorithmicMetrics emptyMetrics() {
        var zero = new AlgorithmicMetrics.CompletionRate(0, 0);
        return new AlgorithmicMetrics(0, 0, Map.of(),
            new AlgorithmicMetrics.TimeWindowMetrics(zero, zero, zero),
            Map.of(), Map.of(),
            new AlgorithmicMetrics.DensityMetrics(zero, zero, zero),
            "None recorded");
    }

    private String pct(double rate) {
        return rate < 0 ? "n/a" : "%d%%".formatted(Math.round(rate * 100));
    }
}
