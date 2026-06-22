package com.rail.api.controller;

import com.rail.api.component.UserResolver;
import com.rail.api.dto.DailyScheduleDto;
import com.rail.api.dto.ScheduleEntryDto;
import com.rail.api.entity.TaskCompletionType;
import com.rail.api.entity.User;
import com.rail.api.scheduler.DailyScheduler;
import com.rail.api.scheduler.ScheduleGenerationService;
import com.rail.api.service.ScheduleEntryService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final DailyScheduler dailyScheduler;
    private final ScheduleEntryService scheduleEntryService;
    private final UserResolver userResolver;
    private final ScheduleGenerationService scheduleGenerationService;

    public record CompleteEntryRequest(
        TaskCompletionType completionType,
        String completionNote,
        BigDecimal actualValue
    ) {}

    public record SkipEntryRequest(String reason) {}

    public record SlipEntryRequest(String note) {}

    @GetMapping("/today")
    public ResponseEntity<DailyScheduleDto> today(@AuthenticationPrincipal UUID userPid) {
        User user = userResolver.resolve(userPid);
        return dailyScheduler.getToday(user)
            .map(ResponseEntity::ok)
            .orElseGet(() -> {
                scheduleGenerationService.generateTodayIfMissing(user);
                return ResponseEntity.notFound().build();
            });
    }

    @PostMapping("/entries/{entryPid}/complete")
    public ResponseEntity<ScheduleEntryDto> completeEntry(
        @PathVariable UUID entryPid,
        @AuthenticationPrincipal UUID userPid,
        @RequestBody CompleteEntryRequest body
    ) {
        return ResponseEntity.ok(scheduleEntryService.complete(
            entryPid,
            userResolver.resolve(userPid),
            body.completionType(),
            body.completionNote(),
            body.actualValue()
        ));
    }

    @PostMapping("/entries/{entryPid}/skip")
    public ResponseEntity<ScheduleEntryDto> skipEntry(
        @PathVariable UUID entryPid,
        @AuthenticationPrincipal UUID userPid,
        @RequestBody SkipEntryRequest body
    ) {
        return ResponseEntity.ok(scheduleEntryService.skip(
            entryPid,
            userResolver.resolve(userPid),
            body.reason()
        ));
    }

    @PostMapping("/entries/{entryPid}/slip")
    public ResponseEntity<ScheduleEntryDto> slipEntry(
        @PathVariable UUID entryPid,
        @AuthenticationPrincipal UUID userPid,
        @RequestBody SlipEntryRequest body
    ) {
        return ResponseEntity.ok(scheduleEntryService.slip(
            entryPid,
            userResolver.resolve(userPid),
            body.note()
        ));
    }
}
