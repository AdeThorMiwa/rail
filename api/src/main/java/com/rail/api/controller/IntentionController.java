package com.rail.api.controller;

import com.rail.api.component.UserResolver;
import com.rail.api.dto.ConfirmIntentionRequest;
import com.rail.api.dto.IntentionDto;
import com.rail.api.dto.UpdateIntentionStatusRequest;
import com.rail.api.service.IntentionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/intentions")
@RequiredArgsConstructor
public class IntentionController {

    private final IntentionService intentionService;
    private final UserResolver userResolver;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IntentionDto confirm(
        @AuthenticationPrincipal UUID userPid,
        @Valid @RequestBody ConfirmIntentionRequest req
    ) {
        return intentionService.confirmProposal(
            userResolver.resolve(userPid),
            req.proposalId()
        );
    }

    @GetMapping
    public List<IntentionDto> list(@AuthenticationPrincipal UUID userPid) {
        return intentionService.list(userResolver.resolve(userPid));
    }

    @GetMapping("/{pid}")
    public IntentionDto get(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable UUID pid
    ) {
        return intentionService.get(userResolver.resolve(userPid), pid);
    }

    @PatchMapping("/{pid}/status")
    public IntentionDto updateStatus(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable UUID pid,
        @Valid @RequestBody UpdateIntentionStatusRequest req
    ) {
        return intentionService.updateStatus(
            userResolver.resolve(userPid),
            pid,
            req.status()
        );
    }
}
