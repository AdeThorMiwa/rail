package com.rail.api.event;

import com.rail.api.chat.MessageBuilder;
import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.NextGoalProposal;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskCompletionType;
import com.rail.api.entity.TaskFlexibility;
import com.rail.api.repository.NextGoalProposalRepository;
import com.rail.api.service.ChatService;
import com.rail.api.service.CycleService;
import com.rail.api.sse.SseService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventListener {

    private final ChatService chatService;
    private final CycleService cycleService;
    private final SseService sseService;
    private final MessageBuilder messageBuilder;
    private final NextGoalProposalRepository nextGoalProposalRepository;

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
        if (event.sourceChatEntityType() == ChatEntityType.CYCLE
                && event.sourceChatEntityId() != null
                && event.goalPid() != null) {
            try {
                cycleService.addFocusGoal(
                    event.user(),
                    event.sourceChatEntityId(),
                    event.goalPid()
                );
            } catch (Exception e) {
                log.warn("addFocusGoal failed for cycle {}: {}", event.sourceChatEntityId(), e.getMessage());
            }
        }

        try {
            String trigger =
                "[SYSTEM: The user just confirmed and created their intention \"%s\". The goal has been added to the cycle focus automatically. Now pick up the conversation — if the user mentioned other intentions earlier that haven't been captured yet, call captureIntention for the next one and ask your first clarifying question. If there are no more pending intentions, ask if there is anything else they want to capture this cycle.]"
                    .formatted(event.intentionTitle());

            sseService.push(event.user().getPid(), "thinking_start", Map.of());
            try {
                chatService.sendOpeningLlmMessage(
                    event.user(),
                    event.sourceChatEntityType(),
                    event.sourceChatEntityId(),
                    trigger
                );
            } catch (Exception e) {
                log.error("sendOpeningLlmMessage failed after intention confirmation: {}", e.getMessage(), e);
                sseService.push(event.user().getPid(), "thinking_stop", Map.of());
            }
        } catch (Exception e) {
            log.error("onIntentionConfirmed chat trigger failed: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNextGoalProposalCreated(NextGoalProposalCreatedEvent event) {
        NextGoalProposal proposal = nextGoalProposalRepository
            .findByPidAndOwner(event.proposalPid(), event.user())
            .orElse(null);
        if (proposal == null) {
            log.warn("onNextGoalProposalCreated: proposal {} not found", event.proposalPid());
            return;
        }
        String trigger = "[SYSTEM: A new goal blueprint has been generated for your intention \"%s\". Present it to the user.]"
            .formatted(proposal.getIntention().getTitle());
        try {
            chatService.sendOpeningLlmMessage(event.user(), ChatEntityType.GLOBAL, null, trigger);
        } catch (Exception e) {
            log.error("onNextGoalProposalCreated: failed to send opening message for proposal {}: {}",
                event.proposalPid(), e.getMessage(), e);
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
