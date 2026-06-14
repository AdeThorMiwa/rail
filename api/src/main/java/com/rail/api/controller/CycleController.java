package com.rail.api.controller;

import com.rail.api.component.UserResolver;
import com.rail.api.dto.CycleFocusDto;
import com.rail.api.dto.UserCycleDto;
import com.rail.api.service.CycleService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cycles")
@RequiredArgsConstructor
public class CycleController {

    private final CycleService cycleService;
    private final UserResolver userResolver;

    @GetMapping("/active")
    public ResponseEntity<UserCycleDto> getActive(@AuthenticationPrincipal UUID userPid) {
        return cycleService.getActive(userResolver.resolve(userPid))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    public record CreateCycleRequest(
        LocalDate startDate,
        LocalDate endDate,
        LocalTime reviewTime,
        String title
    ) {}

    @GetMapping("/{pid}/focuses")
    public ResponseEntity<List<CycleFocusDto>> getFocuses(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable UUID pid
    ) {
        return ResponseEntity.ok(cycleService.getFocuses(userResolver.resolve(userPid), pid));
    }

    @PostMapping
    public ResponseEntity<UserCycleDto> create(
        @AuthenticationPrincipal UUID userPid,
        @RequestBody CreateCycleRequest body
    ) {
        var cycle = cycleService.create(
            userResolver.resolve(userPid),
            body.startDate(),
            body.endDate(),
            body.reviewTime(),
            body.title()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(cycle);
    }
}
