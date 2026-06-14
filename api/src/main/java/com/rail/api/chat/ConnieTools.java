package com.rail.api.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rail.api.entity.Chat;
import com.rail.api.entity.CycleFocus;
import com.rail.api.entity.CycleStatus;
import com.rail.api.entity.Goal;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.IntentionProposalStatus;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.UserCycle;
import com.rail.api.intelligence.IntentionSynthesis;
import com.rail.api.intelligence.RetroAnalysis;
import com.rail.api.repository.ChatRepository;
import com.rail.api.repository.CycleFocusRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.IntentionProposalRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.UserCycleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConnieTools {

    static final String CHAT_ID_KEY = "chatId";

    private final ChatRepository chatRepository;
    private final IntentionProposalRepository proposalRepository;
    private final UserCycleRepository cycleRepository;
    private final GoalRepository goalRepository;
    private final CycleFocusRepository cycleFocusRepository;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    @Tool(
        description = """
        Saves the current understanding of the user's intention to the database.
        Call this only when you have gathered meaningful new information worth persisting — not every turn.
        DO NOT try to create multiple proposals at once! Only one active proposal can exist at once.
        """
    )
    public String updateProposal(
        IntentionSynthesis synthesis,
        ToolContext toolContext
    ) {
        Object rawChatId =
            toolContext != null
                ? toolContext.getContext().get(CHAT_ID_KEY)
                : null;
        if (rawChatId == null) {
            log.error(
                "updateProposal called with missing chatId in ToolContext"
            );
            return "ERROR: tool context missing — proposal was NOT saved. Do NOT show a confirm button.";
        }

        UUID chatPid;
        try {
            chatPid = UUID.fromString(rawChatId.toString());
        } catch (IllegalArgumentException e) {
            log.error(
                "updateProposal called with invalid chatId: {}",
                rawChatId
            );
            return "ERROR: invalid chatId — proposal was NOT saved. Do NOT show a confirm button.";
        }

        Chat chat = chatRepository.findByPid(chatPid).orElse(null);
        if (chat == null) {
            log.error("updateProposal: chat not found for pid {}", chatPid);
            return "ERROR: chat not found — proposal was NOT saved. Do NOT show a confirm button.";
        }

        IntentionProposal proposal = resolve(chat);
        proposal.setSynthesis(synthesis);
        proposalRepository.saveAndFlush(proposal);

        String pid = proposal.getPid().toString();
        log.info("updateProposal: saved proposal {} for chat {}", pid, chatPid);
        return pid;
    }

    private IntentionProposal resolve(Chat chat) {
        return proposalRepository
            .findByChatAndStatus(chat, IntentionProposalStatus.REFINING)
            .filter(p -> p.getStatus() == IntentionProposalStatus.REFINING)
            .orElseGet(() ->
                IntentionProposal.builder()
                    .owner(chat.getUser())
                    .chat(chat)
                    .status(IntentionProposalStatus.REFINING)
                    .build()
            );
    }

    @Tool(
        description = """
        Saves the user's chosen focus goals for their current cycle.
        Call this when the user has committed to which goals to focus on.
        goalPids: list of goal pids (UUIDs) from the AVAILABLE GOALS list.
        This REPLACES any previously saved focus goals for this cycle.
        Returns a short confirmation string on success, or an ERROR: message on failure.
        """
    )
    @Transactional
    public String setCycleFocus(
        List<String> goalPids,
        ToolContext toolContext
    ) {
        Object rawChatId =
            toolContext != null
                ? toolContext.getContext().get(CHAT_ID_KEY)
                : null;
        if (rawChatId == null) {
            log.error(
                "setCycleFocus called with missing chatId in ToolContext"
            );
            return "ERROR: tool context missing — focus was NOT saved.";
        }

        UUID chatPid;
        try {
            chatPid = UUID.fromString(rawChatId.toString());
        } catch (IllegalArgumentException e) {
            return "ERROR: invalid chatId — focus was NOT saved.";
        }

        Chat chat = chatRepository.findByPid(chatPid).orElse(null);
        if (chat == null) {
            log.error("setCycleFocus: chat not found for pid {}", chatPid);
            return "ERROR: chat not found — focus was NOT saved.";
        }

        UUID cyclePid = chat.getEntityId();
        if (cyclePid == null) {
            return "ERROR: chat has no entity — focus was NOT saved.";
        }

        UserCycle cycle = cycleRepository
            .findByPidAndOwner(cyclePid, chat.getUser())
            .orElse(null);
        if (cycle == null) {
            log.error(
                "setCycleFocus: cycle {} not found for user {}",
                cyclePid,
                chat.getUser().getPid()
            );
            return "ERROR: cycle not found — focus was NOT saved.";
        }

        List<Goal> goals = new ArrayList<>();
        for (String pidStr : goalPids) {
            try {
                UUID goalPid = UUID.fromString(pidStr.trim());
                goalRepository
                    .findByPidAndOwner(goalPid, chat.getUser())
                    .ifPresent(goals::add);
            } catch (IllegalArgumentException ignored) {
                log.warn("setCycleFocus: invalid goal pid '{}'", pidStr);
            }
        }

        if (goals.isEmpty()) {
            return "ERROR: none of the provided pids matched active goals for this user.";
        }

        cycleFocusRepository.deleteAll(
            cycleFocusRepository.findByCycleOrderByPositionAsc(cycle)
        );

        for (int i = 0; i < goals.size(); i++) {
            cycleFocusRepository.save(
                CycleFocus.builder()
                    .cycle(cycle)
                    .goal(goals.get(i))
                    .position(i)
                    .build()
            );
        }

        String names = goals
            .stream()
            .map(Goal::getTitle)
            .collect(Collectors.joining(", "));
        log.info(
            "setCycleFocus: set {} focuses for cycle {}",
            goals.size(),
            cyclePid
        );
        return "Focus goals saved: " + names;
    }

    @Tool(
        description = """
        Records the user's carry-over decisions for FLEXIBLE PENDING tasks from the prior cycle.
        keepPids: task pids the user wants to keep — they remain PENDING and will reappear in the schedule.
        dropPids: task pids the user wants to drop — they are marked SKIPPED and removed from the schedule.
        Call this once when the user has decided on all carry-over tasks.
        Returns a short confirmation string.
        """
    )
    @Transactional
    public String resolveCarryOvers(
        List<String> keepPids,
        List<String> dropPids,
        ToolContext toolContext
    ) {
        Object rawChatId =
            toolContext != null
                ? toolContext.getContext().get(CHAT_ID_KEY)
                : null;
        if (rawChatId == null) {
            return "ERROR: tool context missing — carry-overs were NOT saved.";
        }

        UUID chatPid;
        try {
            chatPid = UUID.fromString(rawChatId.toString());
        } catch (IllegalArgumentException e) {
            return "ERROR: invalid chatId — carry-overs were NOT saved.";
        }

        Chat chat = chatRepository.findByPid(chatPid).orElse(null);
        if (chat == null) {
            return "ERROR: chat not found — carry-overs were NOT saved.";
        }

        List<String> dropped = new ArrayList<>();
        for (String pidStr : dropPids) {
            try {
                UUID taskPid = UUID.fromString(pidStr.trim());
                taskRepository.findByPid(taskPid).ifPresent(task -> {
                    if (taskBelongsToUser(task, chat.getUser())) {
                        task.setStatus(TaskStatus.SKIPPED);
                        taskRepository.save(task);
                        dropped.add(task.getTitle());
                    }
                });
            } catch (IllegalArgumentException ignored) {}
        }

        int kept = 0;
        for (String pidStr : keepPids) {
            try {
                UUID taskPid = UUID.fromString(pidStr.trim());
                if (
                    taskRepository
                        .findByPid(taskPid)
                        .filter(t -> taskBelongsToUser(t, chat.getUser()))
                        .isPresent()
                ) {
                    kept++;
                }
            } catch (IllegalArgumentException ignored) {}
        }

        log.info(
            "resolveCarryOvers: kept={} dropped={} for chat {}",
            kept,
            dropped.size(),
            chatPid
        );
        StringBuilder result = new StringBuilder("Carry-overs resolved.");
        if (kept > 0) result.append(" Kept ").append(kept).append(" task(s).");
        if (!dropped.isEmpty()) result
            .append(" Dropped: ")
            .append(String.join(", ", dropped))
            .append(".");
        return result.toString();
    }

    @Tool(
        description = """
        Concludes the cycle retrospective. Call this exactly once after carry-overs are resolved
        and you are ready to show the wrap card. Transitions the cycle to COMPLETED and persists
        the retro summary and key wins for future cycle context.
        summary: 2-3 sentence summary of the cycle retrospective.
        keyWins: list of key wins from this cycle — max 5 items.
        """
    )
    @Transactional
    public String concludeRetro(
        String summary,
        List<String> keyWins,
        ToolContext toolContext
    ) {
        Object rawChatId =
            toolContext != null
                ? toolContext.getContext().get(CHAT_ID_KEY)
                : null;
        if (rawChatId == null) {
            log.error(
                "concludeRetro called with missing chatId in ToolContext"
            );
            return "ERROR: tool context missing — retro was NOT concluded.";
        }

        UUID chatPid;
        try {
            chatPid = UUID.fromString(rawChatId.toString());
        } catch (IllegalArgumentException e) {
            return "ERROR: invalid chatId — retro was NOT concluded.";
        }

        Chat chat = chatRepository.findByPid(chatPid).orElse(null);
        if (chat == null) {
            log.error("concludeRetro: chat not found for pid {}", chatPid);
            return "ERROR: chat not found — retro was NOT concluded.";
        }

        UUID cyclePid = chat.getEntityId();
        if (cyclePid == null) {
            return "ERROR: chat has no entity — retro was NOT concluded.";
        }

        UserCycle cycle = cycleRepository
            .findByPidAndOwner(cyclePid, chat.getUser())
            .orElse(null);
        if (cycle == null) {
            log.error(
                "concludeRetro: cycle {} not found for user {}",
                cyclePid,
                chat.getUser().getPid()
            );
            return "ERROR: cycle not found — retro was NOT concluded.";
        }

        RetroAnalysis existing = null;
        if (cycle.getRetroAnalysis() != null) {
            try {
                existing = objectMapper.readValue(
                    cycle.getRetroAnalysis(),
                    RetroAnalysis.class
                );
            } catch (Exception e) {
                log.warn(
                    "concludeRetro: could not deserialize existing retro analysis: {}",
                    e.getMessage()
                );
            }
        }

        RetroAnalysis updated = new RetroAnalysis(
            existing != null ? existing.focusGoals() : List.of(),
            existing != null ? existing.habitStats() : List.of(),
            existing != null ? existing.abstinenceStats() : List.of(),
            summary,
            keyWins != null ? keyWins : List.of(),
            Instant.now()
        );

        try {
            cycle.setRetroAnalysis(objectMapper.writeValueAsString(updated));
        } catch (Exception e) {
            log.warn(
                "concludeRetro: failed to serialize updated retro analysis: {}",
                e.getMessage()
            );
        }

        cycle.setStatus(CycleStatus.COMPLETED);
        cycleRepository.save(cycle);
        log.info(
            "concludeRetro: cycle {} completed for user {}",
            cyclePid,
            chat.getUser().getPid()
        );
        return "Retro concluded. Cycle is now COMPLETED.";
    }

    private boolean taskBelongsToUser(
        Task task,
        com.rail.api.entity.User user
    ) {
        try {
            return (
                task.getGoal() != null &&
                task.getGoal().getIntention() != null &&
                user.equals(task.getGoal().getIntention().getOwner())
            );
        } catch (Exception e) {
            return false;
        }
    }
}
