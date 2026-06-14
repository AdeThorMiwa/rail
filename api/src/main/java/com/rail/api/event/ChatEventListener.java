package com.rail.api.event;

import com.rail.api.chat.MessageBuilder;
import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskCompletionType;
import com.rail.api.entity.TaskFlexibility;
import com.rail.api.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final ChatService chatService;
    private final MessageBuilder messageBuilder;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCyclePlanningOpen(CyclePlanningOpenEvent event) {
        try {
            chatService.sendOpeningLlmMessage(
                event.user(),
                ChatEntityType.CYCLE,
                event.cyclePid(),
                "New cycle created. Show me my options."
            );
        } catch (Exception e) {
            // Opening message is best-effort — cycle was already created successfully
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCycleRetroOpen(CycleRetroOpenEvent event) {
        try {
            chatService.sendOpeningLlmMessage(
                event.user(),
                ChatEntityType.CYCLE,
                event.cyclePid(),
                "Cycle ended. Start the retrospective."
            );
        } catch (Exception e) {
            // Opening message is best-effort — cycle transition already committed
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onIntentionConfirmed(IntentionConfirmedEvent event) {
        try {
            String trigger =
                "[SYSTEM: The user just confirmed and created their intention %s ]".formatted(
                    event.intentionTitle()
                );

            chatService.sendOpeningLlmMessage(
                event.user(),
                event.sourceChatEntityType(),
                event.sourceChatEntityId(),
                trigger
            );
        } catch (Exception e) {
            // Best-effort — intention was already created successfully
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTaskCreated(TaskCreatedEvent event) {
        for (Task task : event.tasks()) {
            if (task.getNotes() == null || task.getNotes().isBlank()) continue;
            var blocks = messageBuilder.blocks();
            blocks.add(
                messageBuilder.textBlock(
                    messageBuilder.textSpan(task.getNotes())
                )
            );
            chatService.recordEvent(
                event.user(),
                ChatEntityType.TASK,
                task.getPid(),
                blocks.toString(),
                task.getNotes()
            );
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onScheduleChange(ScheduleChangeEvent event) {
        var task = event.change().getTask();
        if (task == null) return;

        switch (event.change().getChangeType()) {
            case TASK_SKIPPED -> handleSkipped(event, task);
            case TASK_COMPLETED -> handleCompleted(event, task);
            default -> {
                /* no chat message for other change types */
            }
        }
    }

    private void handleSkipped(
        ScheduleChangeEvent event,
        com.rail.api.entity.Task task
    ) {
        String reason = event.change().getReason();
        boolean isFixed = task.getFlexibility() == TaskFlexibility.FIXED;

        StringBuilder text = new StringBuilder("You skipped **")
            .append(task.getTitle())
            .append("**");
        if (reason != null && !reason.isBlank()) text.append(" — ").append(
            reason
        );
        text.append(". ");
        text.append(
            isFixed
                ? "That one can't be rescheduled, so it's marked as missed."
                : "I'll carry it over to tomorrow's schedule."
        );

        var blocks = messageBuilder.blocks();
        blocks.add(
            messageBuilder.textBlock(messageBuilder.textSpan(text.toString()))
        );
        chatService.recordEvent(
            event.user(),
            ChatEntityType.TASK,
            task.getPid(),
            blocks.toString(),
            task.getTitle()
        );
    }

    private void handleCompleted(
        ScheduleChangeEvent event,
        com.rail.api.entity.Task task
    ) {
        // Only post a Connie message for HABIT partial completions — ask what stopped them
        if (task.getCompletionType() != TaskCompletionType.PARTIAL) return;
        if (task.getGoal().getType() != GoalType.HABIT) return;

        String text =
            "Good work making a start on **" +
            task.getTitle() +
            "**. " +
            "What got in the way? I'll keep it in mind when planning tomorrow.";

        var blocks = messageBuilder.blocks();
        blocks.add(messageBuilder.textBlock(messageBuilder.textSpan(text)));
        chatService.recordEvent(
            event.user(),
            ChatEntityType.TASK,
            task.getPid(),
            blocks.toString(),
            task.getTitle()
        );
    }
}
