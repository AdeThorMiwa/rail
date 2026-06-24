package com.rail.api.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rail.api.chat.strategy.CyclePlanningStrategy;
import com.rail.api.chat.strategy.CycleRetroStrategy;
import com.rail.api.chat.strategy.EntityChatStrategy;
import com.rail.api.chat.strategy.GeneralChatStrategy;
import com.rail.api.chat.strategy.IntentionRefinementStrategy;
import com.rail.api.context.ContextManager;
import com.rail.api.context.ContextStrategy;
import com.rail.api.context.ConversationContext;
import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.CycleFocus;
import com.rail.api.entity.CycleStatus;
import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalStatus;
import com.rail.api.entity.Intention;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.IntentionProposalStatus;
import com.rail.api.entity.MessageSender;
import com.rail.api.entity.Milestone;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskFlexibility;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.User;
import com.rail.api.entity.UserCycle;
import com.rail.api.intelligence.RetroAnalysis;
import com.rail.api.repository.ChatMessageRepository;
import com.rail.api.repository.ChatRepository;
import com.rail.api.repository.CycleFocusRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.IntentionProposalRepository;
import com.rail.api.repository.MilestoneRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.ToolCallLogRepository;
import com.rail.api.repository.UserCycleRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("stub")
@Slf4j
public class LlmChatHandler implements ChatHandler {

    private static final DateTimeFormatter FILE_FMT =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final ChatModel chatModel;
    private final ConnieTools connieTools;
    private final ContextManager contextManager;
    private final IntentionProposalRepository proposalRepository;
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GoalRepository goalRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final UserCycleRepository cycleRepository;
    private final CycleFocusRepository cycleFocusRepository;
    private final ToolCallLogRepository toolCallLogRepository;
    private final MessageBuilder messageBuilder;
    private final ObjectMapper objectMapper;
    private final GeneralChatStrategy generalChatStrategy;
    private final IntentionRefinementStrategy intentionRefinementStrategy;

    @Value("${rail.llm.log-dir}")
    private String logDir;

    @Value("${rail.llm.log-enabled}")
    private boolean llmLogEnabled;

    @Value("${rail.llm.max-json-retries:2}")
    private int maxJsonRetries;

    @Value("${rail.connie.model.default:deepseek-v4-flash}")
    private String defaultConnieModel;

    @Override
    public ChatHandlerResult handle(Chat chat, String userInput) {
        ContextStrategy strategy;
        Optional<IntentionProposal> activeProposal = Optional.empty();

        if (chat.getEntityType() == ChatEntityType.CYCLE) {
            activeProposal = proposalRepository.findByChatAndStatus(
                chat,
                IntentionProposalStatus.REFINING
            );
            if (activeProposal.isPresent()) {
                strategy = intentionRefinementStrategy;
            } else {
                UserCycle cycle =
                    chat.getEntityId() != null
                        ? cycleRepository
                              .findByPidAndOwner(
                                  chat.getEntityId(),
                                  chat.getUser()
                              )
                              .orElse(null)
                        : null;
                strategy =
                    cycle != null && cycle.getStatus() == CycleStatus.IN_REVIEW
                        ? buildRetroStrategy(cycle, chat)
                        : buildCycleStrategy(chat);
            }
        } else if (
            chat.getEntityType() != null &&
            chat.getEntityType() != ChatEntityType.GLOBAL
        ) {
            strategy = buildEntityStrategy(chat);
        } else {
            activeProposal = proposalRepository.findByChatAndStatus(
                chat,
                IntentionProposalStatus.REFINING
            );
            strategy = activeProposal.isPresent()
                ? intentionRefinementStrategy
                : generalChatStrategy;
        }

        ConversationContext ctx = contextManager.build(
            chat,
            userInput,
            strategy,
            activeProposal.orElse(null)
        );

        List<Message> messages = buildMessages(ctx, strategy);

        // First attempt — with tools
        String rawLlmOutput = invokeLlm(messages, strategy, chat, true);
        writeLlmLog(strategy, messages, rawLlmOutput, 0);

        String blocksJson = parseBlocks(rawLlmOutput);

        // Retry loop — send the exact parse error back so the LLM can self-correct
        for (
            int attempt = 1;
            attempt <= maxJsonRetries && blocksJson == null;
            attempt++
        ) {
            String errorDetail = lastParseError(rawLlmOutput);
            List<Message> retryMessages = new ArrayList<>(messages);
            retryMessages.add(new AssistantMessage(rawLlmOutput));
            retryMessages.add(
                new UserMessage(
                    "Your previous response was not valid JSON. Parse error: " +
                        errorDetail +
                        "\n\n" +
                        "Reproduce the exact same information as valid JSON. " +
                        "The output must start with {\"blocks\":[ and end with ]}. " +
                        "Fix only the JSON syntax — do not change the content."
                )
            );
            rawLlmOutput = invokeLlm(retryMessages, strategy, chat, false);
            writeLlmLog(strategy, retryMessages, rawLlmOutput, attempt);
            blocksJson = parseBlocks(rawLlmOutput);
        }

        if (blocksJson == null) {
            log.error(
                "LLM produced invalid JSON after {} attempts — using fallback text block",
                maxJsonRetries + 1
            );
            var blocks = messageBuilder.blocks();
            blocks.add(
                messageBuilder.textBlock(
                    messageBuilder.textSpan(
                        "I had trouble formatting my response. Please try asking again."
                    )
                )
            );
            blocksJson = messageBuilder.toJson(blocks);
        }

        String userBlocks = buildUserBlocks(userInput);

        return new ChatHandlerResult(
            userBlocks,
            blocksJson,
            "default",
            rawLlmOutput
        );
    }

    private EntityChatStrategy buildEntityStrategy(Chat chat) {
        ChatEntityType entityType = chat.getEntityType();
        String entityContext;
        String relatedContext;
        switch (entityType) {
            case GOAL -> {
                entityContext = goalRepository
                    .findByPidAndOwner(chat.getEntityId(), chat.getUser())
                    .map(goal -> {
                        StringBuilder sb = new StringBuilder();
                        appendIntentionSection(sb, goal.getIntention());
                        appendGoalSection(sb, goal, true);
                        appendMilestoneSection(sb, goal);
                        appendPendingTasksSection(sb, goal);
                        return sb.toString().strip();
                    })
                    .orElse("Goal: (not found)");
                relatedContext = goalRepository
                    .findByPidAndOwner(chat.getEntityId(), chat.getUser())
                    .map(goal -> buildTaskChatSummaries(chat.getUser(), goal))
                    .orElse("");
            }
            case TASK -> {
                entityContext = taskRepository
                    .findByPid(chat.getEntityId())
                    .map(task -> {
                        StringBuilder sb = new StringBuilder();
                        Goal goal = task.getGoal();
                        if (goal != null) {
                            appendIntentionSection(sb, goal.getIntention());
                            appendGoalSection(sb, goal, false);
                            appendMilestoneSection(sb, goal);
                        }
                        appendTaskSection(sb, task, true);
                        if (task.getRescheduledFrom() != null) {
                            sb.append(
                                "  (continued from a previously partial task)\n"
                            );
                        }
                        return sb.toString().strip();
                    })
                    .orElse("Task: (not found)");
                relatedContext = taskRepository
                    .findByPid(chat.getEntityId())
                    .flatMap(task -> Optional.ofNullable(task.getGoal()))
                    .map(goal -> buildGoalChatSummary(chat.getUser(), goal))
                    .orElse("");
            }
            default -> {
                entityContext = "Entity type: " + entityType.name();
                relatedContext = "";
            }
        }
        return new EntityChatStrategy(
            entityType,
            entityContext,
            relatedContext,
            defaultConnieModel
        );
    }

    private CycleRetroStrategy buildRetroStrategy(UserCycle cycle, Chat chat) {
        RetroAnalysis retroAnalysis = null;
        if (cycle.getRetroAnalysis() != null) {
            try {
                retroAnalysis = objectMapper.readValue(
                    cycle.getRetroAnalysis(),
                    RetroAnalysis.class
                );
            } catch (Exception e) {
                log.warn(
                    "buildRetroStrategy: could not parse retro analysis for cycle {}: {}",
                    cycle.getPid(),
                    e.getMessage()
                );
            }
        }

        List<Goal> focusGoals = cycleFocusRepository
            .findByCycleOrderByPositionAsc(cycle)
            .stream()
            .map(CycleFocus::getGoal)
            .toList();
        List<Task> carryOvers = focusGoals.isEmpty()
            ? List.of()
            : taskRepository.findCarryOverCandidates(
                  focusGoals,
                  TaskFlexibility.FLEXIBLE
              );

        String priorRetroSummary = cycleRepository
            .findTopByOwnerAndStatusInOrderByEndDateDesc(
                chat.getUser(),
                List.of(CycleStatus.COMPLETED)
            )
            .flatMap(prior -> {
                if (prior.getRetroAnalysis() == null) return Optional.empty();
                try {
                    RetroAnalysis priorAnalysis = objectMapper.readValue(
                        prior.getRetroAnalysis(),
                        RetroAnalysis.class
                    );
                    return Optional.ofNullable(priorAnalysis.summary());
                } catch (Exception e) {
                    return Optional.<String>empty();
                }
            })
            .orElse(null);

        return new CycleRetroStrategy(
            cycle,
            retroAnalysis,
            carryOvers,
            priorRetroSummary,
            defaultConnieModel
        );
    }

    private CyclePlanningStrategy buildCycleStrategy(Chat chat) {
        UserCycle cycle = cycleRepository
            .findByPidAndOwner(chat.getEntityId(), chat.getUser())
            .orElse(null);
        if (cycle == null) {
            return new CyclePlanningStrategy(
                null,
                List.of(),
                List.of(),
                List.of(),
                defaultConnieModel
            );
        }
        List<Goal> activeGoals = goalRepository.findByOwnerAndStatus(
            chat.getUser(),
            GoalStatus.ACTIVE
        );
        List<CycleFocus> focuses =
            cycleFocusRepository.findByCycleOrderByPositionAsc(cycle);
        List<Task> carryOvers = loadCarryOverCandidates(chat.getUser());
        return new CyclePlanningStrategy(
            cycle,
            activeGoals,
            focuses,
            carryOvers,
            defaultConnieModel
        );
    }

    private List<Task> loadCarryOverCandidates(User user) {
        return cycleRepository
            .findTopByOwnerAndStatusInOrderByEndDateDesc(
                user,
                List.of(CycleStatus.COMPLETED, CycleStatus.IN_REVIEW)
            )
            .map(prior -> {
                List<Goal> priorGoals = cycleFocusRepository
                    .findByCycleOrderByPositionAsc(prior)
                    .stream()
                    .map(CycleFocus::getGoal)
                    .toList();
                if (priorGoals.isEmpty()) return List.<Task>of();
                return taskRepository.findCarryOverCandidates(
                    priorGoals,
                    TaskFlexibility.FLEXIBLE
                );
            })
            .orElse(List.of());
    }

    private String buildGoalChatSummary(User user, Goal goal) {
        return chatRepository
            .findByUserAndEntityTypeAndEntityId(
                user,
                ChatEntityType.GOAL,
                goal.getPid()
            )
            .map(goalChat -> {
                List<ChatMessage> msgs = chatMessageRepository
                    .findByChatOrderByCreatedAtDesc(
                        goalChat,
                        org.springframework.data.domain.PageRequest.of(0, 10)
                    )
                    .stream()
                    .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                    .toList();
                if (msgs.isEmpty()) return "";
                StringBuilder sb = new StringBuilder("Recent goal chat:\n");
                msgs.stream()
                    .filter(
                        m -> m.getRawText() != null && !m.getRawText().isBlank()
                    )
                    .forEach(m ->
                        sb
                            .append("  [")
                            .append(
                                m.getSender() == MessageSender.USER
                                    ? "User"
                                    : "Connie"
                            )
                            .append("] ")
                            .append(truncate(m.getRawText(), 120))
                            .append("\n")
                    );
                return sb.toString().strip();
            })
            .orElse("");
    }

    private String buildTaskChatSummaries(User user, Goal goal) {
        List<Task> tasks = taskRepository.findByGoalAndStatus(
            goal,
            TaskStatus.PENDING
        );
        StringBuilder sb = new StringBuilder();
        tasks
            .stream()
            .limit(5)
            .forEach(task ->
                chatRepository
                    .findByUserAndEntityTypeAndEntityId(
                        user,
                        ChatEntityType.TASK,
                        task.getPid()
                    )
                    .ifPresent(taskChat -> {
                        List<ChatMessage> msgs = chatMessageRepository
                            .findByChatOrderByCreatedAtDesc(
                                taskChat,
                                org.springframework.data.domain.PageRequest.of(
                                    0,
                                    3
                                )
                            )
                            .stream()
                            .sorted(
                                Comparator.comparing(ChatMessage::getCreatedAt)
                            )
                            .toList();
                        List<ChatMessage> withText = msgs
                            .stream()
                            .filter(
                                m ->
                                    m.getRawText() != null &&
                                    !m.getRawText().isBlank()
                            )
                            .toList();
                        if (!withText.isEmpty()) {
                            sb.append("Task \"")
                                .append(task.getTitle())
                                .append("\":\n");
                            withText.forEach(m ->
                                sb
                                    .append("  [")
                                    .append(
                                        m.getSender() == MessageSender.USER
                                            ? "User"
                                            : "Connie"
                                    )
                                    .append("] ")
                                    .append(truncate(m.getRawText(), 100))
                                    .append("\n")
                            );
                        }
                    })
            );
        return sb.toString().strip();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }

    private void appendIntentionSection(StringBuilder sb, Intention intention) {
        if (intention == null) return;
        sb.append("Intention: \"").append(intention.getTitle()).append("\"\n");
        if (
            intention.getCompletionCriteria() != null &&
            !intention.getCompletionCriteria().isBlank()
        ) {
            sb.append("  Success looks like: ")
                .append(intention.getCompletionCriteria())
                .append("\n");
        }
        sb.append("  Status: ").append(intention.getStatus()).append("\n");
        sb.append("\n");
    }

    private void appendGoalSection(
        StringBuilder sb,
        Goal goal,
        boolean isPrimary
    ) {
        String prefix = isPrimary ? "[PRIMARY] " : "";
        sb.append(prefix)
            .append("Goal: \"")
            .append(goal.getTitle())
            .append("\"\n");
        sb.append("  Type: ").append(goal.getType()).append("\n");
        sb.append("  Status: ").append(goal.getStatus()).append("\n");
        if (goal.getEnergyLevel() != null) {
            sb.append("  Energy level: ")
                .append(goal.getEnergyLevel())
                .append("\n");
        }
        if (
            goal.getEstimatedTotalHours() != null &&
            goal.getEstimatedTotalHours() > 0
        ) {
            sb.append("  Estimated total: ")
                .append(goal.getEstimatedTotalHours())
                .append("h\n");
        }
        sb.append("\n");
    }

    private void appendMilestoneSection(StringBuilder sb, Goal goal) {
        List<Milestone> milestones =
            milestoneRepository.findByGoalOrderByPosition(goal);
        if (milestones.isEmpty()) return;
        sb.append("Milestones:\n");
        milestones.forEach(m -> {
            sb.append("  - \"").append(m.getTitle()).append("\"");
            sb.append(" [").append(m.getStatus()).append("]");
            if (m.getTargetDate() != null) {
                sb.append(" due ").append(m.getTargetDate());
            }
            sb.append("\n");
        });
        sb.append("\n");
    }

    private void appendPendingTasksSection(StringBuilder sb, Goal goal) {
        List<Task> pending = taskRepository.findByGoalAndStatus(
            goal,
            TaskStatus.PENDING
        );
        if (pending.isEmpty()) return;
        sb.append("Pending tasks (").append(pending.size()).append("):\n");
        pending
            .stream()
            .limit(5)
            .forEach(t -> appendTaskSection(sb, t, false));
        if (pending.size() > 5) {
            sb.append("  ... and ")
                .append(pending.size() - 5)
                .append(" more\n");
        }
        sb.append("\n");
    }

    private void appendTaskSection(
        StringBuilder sb,
        Task task,
        boolean isPrimary
    ) {
        String prefix = isPrimary ? "[PRIMARY] " : "  - ";
        sb.append(prefix)
            .append("Task: \"")
            .append(task.getTitle())
            .append("\"\n");
        if (isPrimary) {
            sb.append("  Status: ").append(task.getStatus()).append("\n");
            if (task.getDurationMinutes() != null) {
                sb.append("  Duration: ")
                    .append(task.getDurationMinutes())
                    .append(" minutes\n");
            }
            sb.append("  Flexibility: ")
                .append(task.getFlexibility())
                .append("\n");
            if (task.getFixedTime() != null) {
                sb.append("  Fixed time: ")
                    .append(task.getFixedTime())
                    .append("\n");
            }
        } else {
            if (task.getDurationMinutes() != null) {
                sb.append(" (")
                    .append(task.getDurationMinutes())
                    .append(" min)");
            }
            sb.append("\n");
        }
    }

    private String invokeLlm(
        List<Message> messages,
        ContextStrategy strategy,
        Chat chat,
        boolean withTools
    ) {
        var optionsBuilder = OpenAiChatOptions.builder()
            .model(strategy.model())
            .responseFormat(
                new ResponseFormat(
                    ResponseFormat.Type.JSON_OBJECT,
                    (String) null
                )
            )
            .reasoningEffort("high");

        var promptBuilder = ChatClient.create(chatModel)
            .prompt()
            .messages(messages)
            .options(optionsBuilder.build());

        if (withTools) {
            promptBuilder = promptBuilder
                .tools(connieTools)
                .toolContext(
                    Map.of(ConnieTools.CHAT_ID_KEY, chat.getPid().toString())
                );
        }

        return promptBuilder.call().content();
    }

    /** Strips markdown fences and tries to parse blocks JSON. Returns null if invalid. */
    private String parseBlocks(String raw) {
        if (raw == null) return null;
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n?", "");
            trimmed = trimmed.replaceAll("\\n?```$", "").strip();
        }
        try {
            var node = objectMapper.readTree(trimmed);
            if (node.isArray()) return trimmed;
            if (node.isObject()) {
                if (node.has("blocks") && node.get("blocks").isArray()) {
                    return node.get("blocks").toString();
                }
                return objectMapper.createArrayNode().add(node).toString();
            }
        } catch (Exception e) {
            log.warn(
                "JSON parse failed: {} — first 300 chars: {}",
                e.getMessage(),
                trimmed.substring(0, Math.min(300, trimmed.length()))
            );
        }
        return null;
    }

    /** Returns a short description of why the raw string is not valid blocks JSON. */
    private String lastParseError(String raw) {
        if (raw == null || raw.isBlank()) return "empty response";
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n?", "");
            trimmed = trimmed.replaceAll("\\n?```$", "").strip();
        }
        try {
            objectMapper.readTree(trimmed);
            return "parsed as JSON but not a blocks array or {\"blocks\":[...]} object";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private List<Message> buildMessages(
        ConversationContext ctx,
        ContextStrategy strategy
    ) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(strategy.systemPrompt(ctx)));

        // Anchor for tool log fetch: proposal start for intention refinement (so captureIntention
        // logs — created before the first CONNIE message — are not missed), otherwise the oldest
        // message in the history window.
        java.time.Instant toolLogStart = ctx.activeProposal()
            .map(com.rail.api.entity.IntentionProposal::getCreatedAt)
            .orElseGet(() -> ctx.recentHistory().isEmpty()
                ? java.time.Instant.now()
                : ctx.recentHistory().get(0).getCreatedAt());
        var toolLogs = toolCallLogRepository
            .findByChatAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(ctx.chat(), toolLogStart);
        int toolIdx = 0;

        for (ChatMessage msg : ctx.recentHistory()) {
            // Before each CONNIE message, inject tool call/response pairs whose timestamps
            // fall before this message — these are the tool calls made during that turn.
            if (msg.getSender() == MessageSender.CONNIE) {
                while (
                    toolIdx < toolLogs.size() &&
                    toolLogs
                        .get(toolIdx)
                        .getCreatedAt()
                        .isBefore(msg.getCreatedAt())
                ) {
                    var log = toolLogs.get(toolIdx++);

                    messages.add(
                        AssistantMessage.builder()
                            .toolCalls(
                                List.of(
                                    new AssistantMessage.ToolCall(
                                        log.getCallId(),
                                        "function",
                                        log.getToolName(),
                                        log.getArguments() != null
                                            ? log.getArguments()
                                            : "{}"
                                    )
                                )
                            )
                            .build()
                    );

                    messages.add(
                        ToolResponseMessage.builder()
                            .responses(
                                List.of(
                                    new ToolResponseMessage.ToolResponse(
                                        log.getCallId(),
                                        log.getToolName(),
                                        log.getResult() != null
                                            ? log.getResult()
                                            : ""
                                    )
                                )
                            )
                            .build()
                    );
                }
            }

            String text = msg.getRawText() != null ? msg.getRawText() : "";
            if (msg.getSender() == MessageSender.USER) {
                messages.add(new UserMessage(text));
            } else {
                messages.add(new AssistantMessage(text));
            }
        }

        messages.add(new UserMessage(ctx.currentInput()));
        return messages;
    }

    private String buildUserBlocks(String input) {
        var blocks = messageBuilder.blocks();
        blocks.add(messageBuilder.textBlock(messageBuilder.textSpan(input)));
        return messageBuilder.toJson(blocks);
    }

    private void writeLlmLog(
        ContextStrategy strategy,
        List<Message> messages,
        String rawOutput,
        int attempt
    ) {
        if (!llmLogEnabled) return;
        try {
            Path dir = Paths.get(logDir);
            Files.createDirectories(dir);
            String suffix = attempt > 0 ? "_retry" + attempt : "";
            String filename =
                LocalDateTime.now().format(FILE_FMT) +
                "_" +
                strategy.getClass().getSimpleName() +
                suffix +
                ".log";
            StringBuilder sb = new StringBuilder();
            sb.append("strategy : ")
                .append(strategy.getClass().getSimpleName())
                .append("\n");
            sb.append("model    : ").append(strategy.model()).append("\n");
            sb.append("attempt  : ").append(attempt).append("\n");
            sb.append("time     : ").append(LocalDateTime.now()).append("\n\n");
            for (Message msg : messages) {
                switch (msg) {
                    case SystemMessage m -> {
                        sb.append("=== SYSTEM ===\n");
                        sb.append(m.getText()).append("\n\n");
                    }
                    case ToolResponseMessage m -> {
                        sb.append("=== TOOL RESULT ===\n");
                        m.getResponses().forEach(r ->
                            sb.append("[").append(r.name()).append(" id=").append(r.id()).append("]\n")
                              .append(r.responseData()).append("\n")
                        );
                        sb.append("\n");
                    }
                    case AssistantMessage m when !m.getToolCalls().isEmpty() -> {
                        sb.append("=== ASSISTANT (tool calls) ===\n");
                        m.getToolCalls().forEach(tc ->
                            sb.append("[").append(tc.name()).append(" id=").append(tc.id()).append("]\n")
                              .append(tc.arguments()).append("\n")
                        );
                        sb.append("\n");
                    }
                    case AssistantMessage m -> {
                        sb.append("=== ASSISTANT ===\n");
                        sb.append(m.getText()).append("\n\n");
                    }
                    default -> {
                        sb.append("=== USER ===\n");
                        sb.append(msg.getText()).append("\n\n");
                    }
                }
            }
            sb.append("=== RESPONSE ===\n");
            sb.append(rawOutput).append("\n");
            Files.writeString(dir.resolve(filename), sb.toString());
        } catch (IOException e) {
            log.warn("Failed to write LLM log: {}", e.getMessage());
        }
    }
}
