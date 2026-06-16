package com.rail.api.scheduler;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.ScheduleChange;
import com.rail.api.entity.ScheduleChangeType;
import com.rail.api.repository.ScheduleChangeRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ScheduleChangeLogger {

    private final ScheduleChangeRepository changeRepository;

    void log(
        DailySchedule schedule,
        List<DailyScheduleEntry> oldEntries,
        List<DailyScheduleEntry> newEntries
    ) {
        Map<String, DailyScheduleEntry> oldByPid = oldEntries.stream()
            .filter(e -> e.getTask() != null)
            .collect(Collectors.toMap(e -> e.getTask().getPid().toString(), e -> e, (a, b) -> a));
        Map<String, DailyScheduleEntry> newByPid = newEntries.stream()
            .filter(e -> e.getTask() != null)
            .collect(Collectors.toMap(e -> e.getTask().getPid().toString(), e -> e, (a, b) -> a));

        List<ScheduleChange> changes = new ArrayList<>();

        for (var entry : newByPid.entrySet()) {
            if (!oldByPid.containsKey(entry.getKey())) {
                changes.add(ScheduleChange.builder()
                    .dailySchedule(schedule)
                    .task(entry.getValue().getTask())
                    .changeType(ScheduleChangeType.TASK_ADDED)
                    .reason("Added via new intention")
                    .build());
            }
        }

        for (var entry : newByPid.entrySet()) {
            DailyScheduleEntry old = oldByPid.get(entry.getKey());
            if (old != null) {
                boolean same = old.getStartTime().equals(entry.getValue().getStartTime())
                    && old.getEndTime().equals(entry.getValue().getEndTime());
                if (!same) {
                    changes.add(ScheduleChange.builder()
                        .dailySchedule(schedule)
                        .task(entry.getValue().getTask())
                        .changeType(ScheduleChangeType.TIME_SHIFTED)
                        .previousValue("{\"startTime\":\"%s\",\"endTime\":\"%s\"}".formatted(old.getStartTime(), old.getEndTime()))
                        .newValue("{\"startTime\":\"%s\",\"endTime\":\"%s\"}".formatted(entry.getValue().getStartTime(), entry.getValue().getEndTime()))
                        .build());
                }
            }
        }

        if (!changes.isEmpty()) changeRepository.saveAll(changes);
    }
}
