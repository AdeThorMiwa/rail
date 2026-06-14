package com.rail.api.event;

import com.rail.api.entity.ChatEntityType;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.IntentionRepository;
import com.rail.api.repository.MilestoneRepository;
import com.rail.api.repository.TaskRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final IntentionRepository intentionRepository;
    private final MilestoneRepository milestoneRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(NotificationEvent event) {
        String label = resolveLabel(event.entityType(), event.entityId());
        String preview = preview(event.rawText());

        Map<String, Object> payload = Map.of(
            "id", UUID.randomUUID().toString(),
            "entityType", event.entityType().name(),
            "entityId", event.entityId() != null ? event.entityId().toString() : "",
            "label", label,
            "preview", preview,
            "timestamp", Instant.now().toString()
        );

        eventPublisher.publishEvent(
            new SsePublishEvent(event.userPid(), "notification", payload)
        );
    }

    private String resolveLabel(ChatEntityType entityType, UUID entityId) {
        if (entityId == null) return "Connie";
        if (entityType == ChatEntityType.GOAL) {
            return goalRepository.findByPid(entityId).map(g -> g.getTitle()).orElse("Goal");
        } else if (entityType == ChatEntityType.TASK) {
            return taskRepository.findByPid(entityId).map(t -> t.getTitle()).orElse("Task");
        } else if (entityType == ChatEntityType.INTENTION) {
            return intentionRepository.findByPid(entityId).map(i -> i.getTitle()).orElse("Intention");
        } else if (entityType == ChatEntityType.MILESTONE) {
            return milestoneRepository.findByPid(entityId).map(m -> m.getTitle()).orElse("Milestone");
        } else if (entityType == ChatEntityType.DAILY_SCHEDULE) {
            return "Daily Schedule";
        } else {
            return "Connie";
        }
    }

    private String preview(String rawText) {
        if (rawText == null || rawText.isBlank()) return "New message from Connie";
        return rawText.length() <= 120 ? rawText : rawText.substring(0, 120) + "…";
    }
}
