package com.rail.api.chat;

import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.User;
import com.rail.api.service.ChatService;
import com.rail.api.sse.SseService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAsyncService {

    private final ChatService chatService;
    private final SseService sseService;

    @Async
    public void sendAsync(User user, String rawInput, UUID replyToId) {
        sendAsync(user, ChatEntityType.GLOBAL, null, rawInput, replyToId);
    }

    @Async
    public void sendAsync(User user, ChatEntityType entityType, UUID entityId, String rawInput, UUID replyToId) {
        sseService.push(user.getPid(), "thinking_start", Map.of());
        try {
            chatService.send(user, entityType, entityId, rawInput, replyToId);
        } catch (Exception e) {
            log.error(
                "Async send failed for user {}: {}",
                user.getPid(),
                e.getMessage(),
                e
            );
            sseService.push(user.getPid(), "thinking_stop", Map.of());
            sseService.push(
                user.getPid(),
                "error",
                Map.of("message", "Something went wrong — please try again.")
            );
        }
    }
}
