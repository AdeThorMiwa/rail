package com.rail.api.scheduler;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskPriority;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TaskTimePlacer {

    private final ScheduleEntryAssembler entryAssembler;

    List<DailyScheduleEntry> place(
        DailySchedule schedule,
        List<Task> fixedTasks,
        SchedulingPlan plan,
        Map<String, Task> flexibleTaskByPid,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart,
        LocalTime dayEnd,
        int bufferMinutes,
        List<DailyScheduleEntry> frozenEntries
    ) {
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

        // Anchor FIXED tasks at their entity fixedTime (valid anywhere in the day)
        List<Placement> fixedPlacements = new ArrayList<>();
        for (Task t : fixedTasks) {
            if (t.getFixedTime() == null) continue;
            // If by the time the scheduler is about to run, a fixed task time has already passed
            // We only still want to surface it if its a high or critical task, otherwise we do not schedule at all
            if (
                t.getFixedTime().isBefore(effectiveStart) &&
                (t.getPriority() != TaskPriority.HIGH ||
                    t.getPriority() != TaskPriority.CRITICAL)
            ) continue;
            int dur =
                t.getDurationMinutes() != null ? t.getDurationMinutes() : 60;
            LocalTime start = t.getFixedTime();
            LocalTime end = start.plusMinutes(dur);
            fixedPlacements.add(new Placement(start, end, t));
            occupied.add(new TimeSlot(start, end));
        }

        // Place DEEP_WINDOW tasks within [deepStart, deepEnd]
        List<Placement> deepPlacements = placeTasksInWindow(
            deepSelected,
            flexibleTaskByPid,
            deepStart.isAfter(effectiveStart) ? deepStart : effectiveStart,
            deepEnd,
            occupied,
            bufferMinutes
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
            flexibleTaskByPid,
            deepEnd.isAfter(effectiveStart) ? deepEnd : effectiveStart,
            dayEnd,
            occupied,
            bufferMinutes
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
                flexibleTaskByPid,
                effectiveStart,
                deepStart,
                occupied,
                bufferMinutes
            );
            outerPlacements.addAll(preDeep);
        }

        List<Placement> allPlacements = new ArrayList<>();
        allPlacements.addAll(fixedPlacements);
        allPlacements.addAll(deepPlacements);
        allPlacements.addAll(outerPlacements);
        allPlacements.sort(Comparator.comparing(Placement::start));

        return entryAssembler.assemble(
            schedule,
            allPlacements,
            frozenEntries,
            deepStart,
            deepEnd,
            effectiveStart,
            dayEnd,
            bufferMinutes
        );
    }

    private List<Placement> placeTasksInWindow(
        List<SchedulingPlan.SelectedTask> selected,
        Map<String, Task> taskByPid,
        LocalTime windowStart,
        LocalTime windowEnd,
        List<TimeSlot> occupied,
        int bufferMinutes
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

    private static LocalTime maxTime(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }
}
