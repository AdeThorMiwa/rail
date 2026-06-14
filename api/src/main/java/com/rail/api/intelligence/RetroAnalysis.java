package com.rail.api.intelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RetroAnalysis(
    List<FocusGoalStats> focusGoals,
    List<HabitStats> habitStats,
    List<AbstinenceStats> abstinenceStats,
    String summary,
    List<String> keyWins,
    Instant completedAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FocusGoalStats(
        String goalPid,
        String goalTitle,
        String goalType,
        int totalTasks,
        int completedTasks,
        int skippedTasks,
        double completionRate
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HabitStats(
        String goalPid,
        String goalTitle,
        int totalOccurrences,
        int done,
        int skipped,
        int missed,
        double adherenceRate
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbstinenceStats(
        String goalPid,
        String goalTitle,
        int totalDays,
        int resistedDays,
        int lapses,
        double resistanceRate
    ) {}
}
