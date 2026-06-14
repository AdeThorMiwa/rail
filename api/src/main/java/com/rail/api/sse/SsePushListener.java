package com.rail.api.sse;

import com.rail.api.event.SsePublishEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SsePushListener {

    private final SseService sseService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSsePublish(SsePublishEvent event) {
        sseService.push(event.userPid(), event.eventType(), event.payload());
    }
}
