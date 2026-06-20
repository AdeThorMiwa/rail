package com.rail.api.connie;

import com.rail.api.entity.EnergyLevel;
import com.rail.api.entity.GoalType;
import java.time.DayOfWeek;
import java.util.Map;

public record AlgorithmicMetrics(
    int daysAnalyzed,
    int totalScheduledTasks,
    Map<DayOfWeek, DayMetrics> byDayOfWeek,
    TimeWindowMetrics byTimeWindow,
    Map<EnergyLevel, CompletionRate> byEnergyLevel,
    Map<GoalType, CompletionRate> byGoalType,
    DensityMetrics byScheduleDensity,
    String topSkipReasons
) {
    public record DayMetrics(int scheduled, int completed, int missed) {
        public double completionRate() {
            int acted = completed + missed;
            return acted == 0 ? -1 : (double) completed / acted;
        }
    }

    public record CompletionRate(int scheduled, int completed) {
        public double rate() {
            return scheduled == 0 ? -1 : (double) completed / scheduled;
        }
    }

    public record TimeWindowMetrics(
        CompletionRate morning,   // before 12:00
        CompletionRate afternoon, // 12:00–17:00
        CompletionRate evening    // after 17:00
    ) {}

    public record DensityMetrics(
        CompletionRate light,   // 1–3 tasks/day
        CompletionRate moderate, // 4–5 tasks/day
        CompletionRate heavy    // 6+ tasks/day
    ) {}
}
