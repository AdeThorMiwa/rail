package com.rail.api.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rail.api.connie.ConnieProfileService;
import com.rail.api.entity.Chat;
import com.rail.api.entity.CycleFocus;
import com.rail.api.entity.CycleStatus;
import com.rail.api.entity.Goal;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.IntentionProposalStatus;
import com.rail.api.entity.NextGoalProposal;
import com.rail.api.entity.NextGoalProposalStatus;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.UserCycle;
import com.rail.api.intelligence.GoalBlueprint;
import com.rail.api.intelligence.IntentionSynthesis;
import com.rail.api.intelligence.RetroAnalysis;
import com.rail.api.entity.ToolCallLog;
import com.rail.api.repository.ChatRepository;
import com.rail.api.repository.CycleFocusRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.IntentionProposalRepository;
import com.rail.api.repository.NextGoalProposalRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.ToolCallLogRepository;
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
    private final NextGoalProposalRepository nextGoalProposalRepository;
    private final UserCycleRepository cycleRepository;
    private final GoalRepository goalRepository;
    private final CycleFocusRepository cycleFocusRepository;
    private final TaskRepository taskRepository;
    private final ToolCallLogRepository toolCallLogRepository;
    private final ConnieProfileService connieProfileService;
    private final ObjectMapper objectMapper;

    @Tool(
        description = """
        Registers a newly detected intention from the user and hands off to the Intention Refiner.
        Call this IMMEDIATELY when you detect a goal, achievement, task, or reminder intent — before asking any clarifying question.
        title: the user's raw intention in plain words, exactly as you understood it.
        Returns the proposal UUID on success, or an ERROR: message on failure.
        """
    )
    @Transactional
    public String captureIntention(String title, ToolContext toolContext) {
        log.info("captureIntention called — title={}", title);

        if (title == null || title.isBlank()) {
            return "ERROR: title is required — intention was NOT captured.";
        }

        Object rawChatId = toolContext != null ? toolContext.getContext().get(CHAT_ID_KEY) : null;
        if (rawChatId == null) {
            log.error("captureIntention called with missing chatId in ToolContext");
            return "ERROR: tool context missing — intention was NOT captured.";
        }

        UUID chatPid;
        try {
            chatPid = UUID.fromString(rawChatId.toString());
        } catch (IllegalArgumentException e) {
            log.error("captureIntention called with invalid chatId: {}", rawChatId);
            return "ERROR: invalid chatId — intention was NOT captured.";
        }

        Chat chat = chatRepository.findByPid(chatPid).orElse(null);
        if (chat == null) {
            log.error("captureIntention: chat not found for pid {}", chatPid);
            return "ERROR: chat not found — intention was NOT captured.";
        }

        boolean alreadyActive = proposalRepository
            .findByChatAndStatus(chat, IntentionProposalStatus.REFINING)
            .isPresent();
        if (alreadyActive) {
            log.warn("captureIntention: REFINING proposal already exists for chat {}", chatPid);
            return "ERROR: an active intention is already being refined — do NOT call captureIntention again.";
        }

        IntentionProposal proposal = IntentionProposal.builder()
            .owner(chat.getUser())
            .chat(chat)
            .status(IntentionProposalStatus.REFINING)
            .synthesis(new com.rail.api.intelligence.IntentionSynthesis(
                new com.rail.api.intelligence.IntentionBlueprint(null, title, null),
                null
            ))
            .build();
        proposalRepository.saveAndFlush(proposal);

        String pid = proposal.getPid().toString();
        log.info("captureIntention: created proposal {} for title='{}'", pid, title);
        logToolCall(chat, "captureIntention", "{\"title\":" + escapeJson(title) + "}", pid);
        return pid;
    }

    @Tool(
        description = """
        Saves the current understanding of the user's intention to the database.
        Call this only when the synthesis is COMPLETE — both intention AND goal fields must be fully populated.
        A null or missing goal field is NOT valid; wait until you have enough information to fill both fields.
        DO NOT try to create multiple proposals at once! Only one active proposal can exist at once.

        context: A lossless, detailed, comprehensive plain-text summary of everything the user shared
        during this refinement conversation — their goals, motivations, constraints, background, any
        documents or PRDs they pasted, nuances, edge cases, and any other information that would help
        Rail generate better goals for this intention in the future. Write it as a rich narrative, not
        a bullet list. This field is REQUIRED when showing a confirmation screen — do not include a
        Confirm Intention button if context is null or blank.
        """
    )
    public String updateProposal(
        IntentionSynthesis synthesis,
        String context,
        ToolContext toolContext
    ) {
        log.info("updateProposal called — intention={}, goal={}, hasContext={}",
            synthesis != null && synthesis.intention() != null ? synthesis.intention().title() : "null",
            synthesis != null ? synthesis.goal() : "null (synthesis itself is null)",
            context != null && !context.isBlank());

        if (synthesis == null || synthesis.goal() == null) {
            log.warn("updateProposal rejected — goal is null. intention={}",
                synthesis != null && synthesis.intention() != null ? synthesis.intention().title() : "null");
            return "ERROR: synthesis is incomplete (goal is missing) — proposal was NOT saved. Gather more information before calling updateProposal. Do NOT show a confirm button.";
        }
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
        if (context != null && !context.isBlank()) {
            proposal.setContext(context);
        }
        proposalRepository.saveAndFlush(proposal);

        String pid = proposal.getPid().toString();
        log.info("updateProposal: saved proposal {} — goalType={}, tasks={}, recurrence={}",
            pid,
            synthesis.goal().goalType(),
            synthesis.goal().tasks() != null ? synthesis.goal().tasks().size() : 0,
            synthesis.goal().recurrence());
        try {
            String argsJson = objectMapper.writeValueAsString(
                java.util.Map.of("synthesis", synthesis, "context", context != null ? context : "")
            );
            logToolCall(chat, "updateProposal", argsJson, pid);
        } catch (Exception e) {
            logToolCall(chat, "updateProposal", "{}", pid);
        }
        return pid;
    }

    @Tool(
        description = """
        Saves the updated blueprint for the next goal currently being refined.
        Call this when you are ready to show the confirmation screen — the blueprint must be fully populated.
        context: a concise summary of any adjustments the user requested during this refinement conversation.
        Returns the proposal UUID on success, or an ERROR: message on failure.
        Do NOT show a Confirm Goal button if this returns an ERROR.
        """
    )
    @Transactional
    public String updateNextGoalProposal(
        GoalBlueprint blueprint,
        String context,
        ToolContext toolContext
    ) {
        log.info("updateNextGoalProposal called — goalType={}, tasks={}",
            blueprint != null ? blueprint.goalType() : "null",
            blueprint != null && blueprint.tasks() != null ? blueprint.tasks().size() : 0);

        if (blueprint == null) {
            return "ERROR: blueprint is null — proposal was NOT saved. Do NOT show a Confirm Goal button.";
        }

        Object rawChatId = toolContext != null ? toolContext.getContext().get(CHAT_ID_KEY) : null;
        if (rawChatId == null) {
            log.error("updateNextGoalProposal called with missing chatId in ToolContext");
            return "ERROR: tool context missing — proposal was NOT saved. Do NOT show a Confirm Goal button.";
        }

        UUID chatPid;
        try {
            chatPid = UUID.fromString(rawChatId.toString());
        } catch (IllegalArgumentException e) {
            log.error("updateNextGoalProposal called with invalid chatId: {}", rawChatId);
            return "ERROR: invalid chatId — proposal was NOT saved. Do NOT show a Confirm Goal button.";
        }

        Chat chat = chatRepository.findByPid(chatPid).orElse(null);
        if (chat == null) {
            log.error("updateNextGoalProposal: chat not found for pid {}", chatPid);
            return "ERROR: chat not found — proposal was NOT saved. Do NOT show a Confirm Goal button.";
        }

        NextGoalProposal proposal = nextGoalProposalRepository
            .findByChatAndStatus(chat, NextGoalProposalStatus.REFINING)
            .orElse(null);
        if (proposal == null) {
            log.error("updateNextGoalProposal: no REFINING proposal found for chat {}", chatPid);
            return "ERROR: no active goal proposal found — proposal was NOT saved. Do NOT show a Confirm Goal button.";
        }

        proposal.setGoalBlueprint(blueprint);
        if (context != null && !context.isBlank()) {
            proposal.setContext(context);
        }
        nextGoalProposalRepository.saveAndFlush(proposal);

        String pid = proposal.getPid().toString();
        log.info("updateNextGoalProposal: saved proposal {} — goalType={}, tasks={}",
            pid, blueprint.goalType(),
            blueprint.tasks() != null ? blueprint.tasks().size() : 0);
        try {
            String argsJson = objectMapper.writeValueAsString(
                java.util.Map.of("blueprint", blueprint, "context", context != null ? context : "")
            );
            logToolCall(chat, "updateNextGoalProposal", argsJson, pid);
        } catch (Exception e) {
            logToolCall(chat, "updateNextGoalProposal", "{}", pid);
        }
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
        String result = "Focus goals saved: " + names;
        logToolCall(chat, "setCycleFocus", "{\"goalPids\":" + escapeJson(String.join(",", goalPids)) + "}", result);
        return result;
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
        String keptPart = kept > 0 ? " Kept %d task(s).".formatted(kept) : "";
        String droppedPart = !dropped.isEmpty() ? " Dropped: %s.".formatted(String.join(", ", dropped)) : "";
        String result = "Carry-overs resolved." + keptPart + droppedPart;
        logToolCall(chat, "resolveCarryOvers", "{\"keep\":" + keepPids.size() + ",\"drop\":" + dropPids.size() + "}", result);
        return result;
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
        String result = "Retro concluded. Cycle is now COMPLETED.";
        logToolCall(chat, "concludeRetro", "{\"summary\":" + escapeJson(summary) + "}", result);
        return result;
    }

    @Tool(
        description = """
        Records a scheduling preference or behavioural pattern the user explicitly stated during conversation.
        Call this when the user says something that reveals a clear, durable preference about how they want their time or tasks managed.
        Examples: "I don't like gym on Mondays", "I prefer creative work in the mornings", "batch my admin on Fridays", "I work better with fewer tasks per day".
        Do NOT call this for general conversation or vague statements — only for specific, actionable preferences the scheduler or planner should remember.
        Returns "saved" on success or an ERROR: message on failure.
        """
    )
    @Transactional
    public String updatePreference(String preference, ToolContext toolContext) {
        Object rawChatId =
            toolContext != null
                ? toolContext.getContext().get(CHAT_ID_KEY)
                : null;
        if (
            rawChatId == null
        ) return "ERROR: tool context missing — preference was NOT saved.";

        UUID chatPid;
        try {
            chatPid = UUID.fromString(rawChatId.toString());
        } catch (IllegalArgumentException e) {
            return "ERROR: invalid chatId — preference was NOT saved.";
        }

        Chat chat = chatRepository.findByPid(chatPid).orElse(null);
        if (
            chat == null
        ) return "ERROR: chat not found — preference was NOT saved.";

        connieProfileService.appendStatedPreference(chat.getUser(), preference);

        log.info(
            "updatePreference: saved preference for user {}",
            chat.getUser().getPid()
        );
        logToolCall(chat, "updatePreference", "{\"preference\":" + escapeJson(preference) + "}", "saved");
        return "saved";
    }

    private void logToolCall(Chat chat, String toolName, String arguments, String result) {
        try {
            toolCallLogRepository.save(ToolCallLog.builder()
                .chat(chat)
                .callId(UUID.randomUUID().toString())
                .toolName(toolName)
                .arguments(arguments)
                .result(result)
                .build());
        } catch (Exception e) {
            log.warn("logToolCall: failed to persist tool call log for {}: {}", toolName, e.getMessage());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
