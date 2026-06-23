package com.rail.api.controller;

import com.rail.api.chat.ChatAsyncService;
import com.rail.api.component.RateLimitService;
import com.rail.api.component.UserResolver;
import com.rail.api.dto.ChatMessageDto;
import com.rail.api.dto.ResolveActionBlockRequest;
import com.rail.api.dto.SendMessageRequest;
import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.User;
import com.rail.api.service.ChatService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ChatAsyncService chatAsyncService;
    private final UserResolver userResolver;
    private final RateLimitService rateLimitService;

    @GetMapping("/messages")
    public List<ChatMessageDto> getMessages(
        @AuthenticationPrincipal UUID userPid,
        @RequestParam(required = false) UUID before
    ) {
        User user = userResolver.resolve(userPid);
        return chatService.getMessages(user, ChatEntityType.GLOBAL, null, before);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void send(
        @AuthenticationPrincipal UUID userPid,
        @RequestBody SendMessageRequest request
    ) {
        rateLimitService.checkChat(userPid);
        User user = userResolver.resolve(userPid);
        chatAsyncService.sendAsync(
            user,
            request.rawInput(),
            request.replyToId()
        );
    }

    @PatchMapping("/messages/{pid}/action-block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveActionBlock(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable UUID pid,
        @RequestBody ResolveActionBlockRequest request
    ) {
        User user = userResolver.resolve(userPid);
        chatService.resolveActionBlock(
            user,
            pid,
            request.tappedItemId(),
            request.resolvedItems()
        );
    }

    // Entity-scoped chat endpoints — read and send messages in a specific entity's chat.
    // The rollup view (goal chat showing task events) is handled at the query/UI layer,
    // not here — each endpoint returns only messages belonging to that entity's chat.

    @GetMapping("/{entityType}/{entityId}/messages")
    public List<ChatMessageDto> getEntityMessages(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable ChatEntityType entityType,
        @PathVariable UUID entityId,
        @RequestParam(required = false) UUID before
    ) {
        User user = userResolver.resolve(userPid);
        return chatService.getMessages(user, entityType, entityId, before);
    }

    @PostMapping("/{entityType}/{entityId}/messages")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendEntityMessage(
        @AuthenticationPrincipal UUID userPid,
        @PathVariable ChatEntityType entityType,
        @PathVariable UUID entityId,
        @RequestBody SendMessageRequest request
    ) {
        rateLimitService.checkChat(userPid);
        User user = userResolver.resolve(userPid);
        chatAsyncService.sendAsync(user, entityType, entityId, request.rawInput(), request.replyToId());
    }

}
