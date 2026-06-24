package com.rail.api.scheduler;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.DailyScheduleEntryType;
import com.rail.api.entity.Task;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class ScheduleEntryAssembler {

    List<DailyScheduleEntry> assemble(
        DailySchedule schedule,
        List<Placement> allPlacements,
        List<DailyScheduleEntry> frozenEntries,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart,
        LocalTime dayEnd,
        int bufferMinutes
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
                    deepStart,
                    bufferMinutes
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
                deepEnd,
                bufferMinutes
            )
        );

        // Post-deep outer window [deepEnd, dayEnd]
        if (deepEnd.isBefore(dayEnd)) {
            entries.addAll(
                buildWindowEntries(
                    schedule,
                    allOccupied,
                    placementByStart,
                    frozenStarts,
                    deepEnd,
                    dayEnd,
                    bufferMinutes
                )
            );
        }

        // FIXED tasks outside all managed windows (edge case)
        for (Placement p : allPlacements) {
            boolean inDay =
                !p.start().isBefore(effectiveStart) &&
                !p.end().isAfter(dayEnd);
            if (!inDay && !frozenStarts.contains(p.start())) {
                entries.add(
                    buildTaskEntry(schedule, p.task(), p.start(), p.end(), p.schedulingNote())
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
        LocalTime windowEnd,
        int bufferMinutes
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

            entries.add(buildTaskEntry(schedule, p.task(), p.start(), p.end(), p.schedulingNote()));

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

    private DailyScheduleEntry buildTaskEntry(
        DailySchedule schedule,
        Task task,
        LocalTime start,
        LocalTime end,
        String schedulingNote
    ) {
        return DailyScheduleEntry.builder()
            .dailySchedule(schedule)
            .task(task)
            .entryType(DailyScheduleEntryType.TASK)
            .startTime(start)
            .endTime(end)
            .notes(schedulingNote)
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

    private static LocalTime minTime(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}
