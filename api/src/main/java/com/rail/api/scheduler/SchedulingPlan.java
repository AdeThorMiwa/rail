package com.rail.api.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SchedulingPlan(
    List<SelectedTask> selected,
    List<ScheduleConflict> conflicts,
    String dayNotes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SelectedTask(
        String taskPid,
        int order,
        String energyZone,
        boolean mustScheduleToday,
        String schedulingNote
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScheduleConflict(
        String taskPid,
        String reason
    ) {}
}
