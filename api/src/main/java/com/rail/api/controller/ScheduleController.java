package com.rail.api.controller;

import com.rail.api.component.DtoMapper;
import com.rail.api.component.UserResolver;
import com.rail.api.dto.DailyScheduleDto;
import com.rail.api.dto.ScheduleEntryDto;
import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleStatus;
import com.rail.api.entity.TaskCompletionType;
import com.rail.api.entity.User;
import com.rail.api.repository.DailyScheduleEntryRepository;
import com.rail.api.repository.DailyScheduleRepository;
import com.rail.api.scheduler.ScheduleGenerationLock;
import com.rail.api.scheduler.ScheduleGenerationService;
import com.rail.api.service.ScheduleEntryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleEntryService scheduleEntryService;
    private final UserResolver userResolver;
    private final ScheduleGenerationService scheduleGenerationService;
    private final ScheduleGenerationLock lock;
    private final DailyScheduleRepository scheduleRepository;
    private final DailyScheduleEntryRepository entryRepository;
    private final DtoMapper dtoMapper;

    public record CompleteEntryRequest(
        TaskCompletionType completionType,
        String completionNote,
        BigDecimal actualValue
    ) {}

    public record SkipEntryRequest(String reason) {}

    public record SlipEntryRequest(String note) {}

    @PostMapping("/today")
    public ResponseEntity<DailyScheduleDto> today(
        @AuthenticationPrincipal UUID userPid
    ) {
        User user = userResolver.resolve(userPid);
        LocalDate date = scheduleGenerationService.todayFor(user);
        var existingSchedule = scheduleRepository.findByUserAndScheduledDate(
            user,
            date
        );

        if (existingSchedule.isPresent()) {
            var s = existingSchedule.get();
            var status = s.getStatus();

            if (
                status == DailyScheduleStatus.PLANNED ||
                status == DailyScheduleStatus.IN_PROGRESS ||
                status == DailyScheduleStatus.COMPLETED
            ) {
                return ResponseEntity.ok(toDto(s));
            }

            if (
                lock.isLocked(user.getPid(), date) &&
                status == DailyScheduleStatus.GENERATING
            ) {
                return ResponseEntity.ok(toDto(s));
            }

            // GENERATING + no lock (died) or FAILED — reset existing record and retry
            DailySchedule reset = scheduleGenerationService.resetToGenerating(
                s
            );
            scheduleGenerationService.generateAsync(user, date);
            return ResponseEntity.ok(toDto(reset));
        }

        DailySchedule schedule = scheduleGenerationService.createNew(
            user,
            date
        );
        scheduleGenerationService.generateAsync(user, date);
        return ResponseEntity.ok(toDto(schedule));
    }

    private DailyScheduleDto toDto(DailySchedule schedule) {
        return dtoMapper.toDailyScheduleDto(
            schedule,
            entryRepository.findByDailyScheduleOrderByStartTime(schedule)
        );
    }

    @PostMapping("/entries/{entryPid}/complete")
    public ResponseEntity<ScheduleEntryDto> completeEntry(
        @PathVariable UUID entryPid,
        @AuthenticationPrincipal UUID userPid,
        @RequestBody CompleteEntryRequest body
    ) {
        return ResponseEntity.ok(
            scheduleEntryService.complete(
                entryPid,
                userResolver.resolve(userPid),
                body.completionType(),
                body.completionNote(),
                body.actualValue()
            )
        );
    }

    @PostMapping("/entries/{entryPid}/skip")
    public ResponseEntity<ScheduleEntryDto> skipEntry(
        @PathVariable UUID entryPid,
        @AuthenticationPrincipal UUID userPid,
        @RequestBody SkipEntryRequest body
    ) {
        return ResponseEntity.ok(
            scheduleEntryService.skip(
                entryPid,
                userResolver.resolve(userPid),
                body.reason()
            )
        );
    }

    @PostMapping("/entries/{entryPid}/slip")
    public ResponseEntity<ScheduleEntryDto> slipEntry(
        @PathVariable UUID entryPid,
        @AuthenticationPrincipal UUID userPid,
        @RequestBody SlipEntryRequest body
    ) {
        return ResponseEntity.ok(
            scheduleEntryService.slip(
                entryPid,
                userResolver.resolve(userPid),
                body.note()
            )
        );
    }
}
