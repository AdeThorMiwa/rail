package com.rail.api.controller;

import com.rail.api.component.UserResolver;
import com.rail.api.dto.ChatMessageDto;
import com.rail.api.dto.GoalDto;
import com.rail.api.service.ChatService;
import com.rail.api.service.GoalService;
import java.util.List;
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
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final ChatService chatService;
    private final UserResolver userResolver;

    @GetMapping("/{pid}")
    public GoalDto get(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable UUID pid
    ) {
        return goalService.get(userResolver.resolve(userPid), pid);
    }

    @GetMapping("/{pid}/activity")
    public List<ChatMessageDto> getActivity(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable UUID pid
    ) {
        return chatService.getGoalActivity(userResolver.resolve(userPid), pid);
    }

    @PostMapping("/{pid}/complete")
    public ResponseEntity<Void> complete(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable UUID pid
    ) {
        goalService.complete(userResolver.resolve(userPid), pid);
        return ResponseEntity.noContent().build();
    }

    public record SlipRequest(String note) {}

    @PostMapping("/{pid}/slip")
    public ResponseEntity<Void> slip(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable UUID pid,
        @RequestBody SlipRequest body
    ) {
        goalService.slip(userResolver.resolve(userPid), pid, body.note());
        return ResponseEntity.noContent().build();
    }
}
