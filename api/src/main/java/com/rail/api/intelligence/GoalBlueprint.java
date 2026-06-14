package com.rail.api.intelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rail.api.entity.EnergyLevel;
import com.rail.api.entity.GoalRecurrenceFrequency;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.TaskPriority;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoalBlueprint(
    GoalType goalType,
    EnergyLevel energyLevel,
    Long estimatedHours,
    Integer daysUntilTarget,
    TargetBlueprint target,
    RecurrenceBlueprint recurrence,
    List<MilestoneBlueprint> milestones,
    List<TaskBlueprint> tasks,
    LocalDate earliestStartDate
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TargetBlueprint(BigDecimal targetValue, String unit) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecurrenceBlueprint(
        GoalRecurrenceFrequency frequency,
        Integer timesPerPeriod,
        List<RecurrenceDayBlueprint> days
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecurrenceDayBlueprint(
        DayOfWeek dayOfWeek,
        LocalTime preferredTime
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MilestoneBlueprint(String title, Integer daysUntilTarget) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TaskBlueprint(
        String title,
        String notes,
        Integer durationMinutes,
        TaskPriority priority,
        Integer milestoneIndex,
        BigDecimal estimatedValue,
        String flexibility,
        String fixedTime,
        String deadline
    ) {}
}
