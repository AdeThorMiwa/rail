package com.rail.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rail.api.entity.Chat;
import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalStatus;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.Intention;
import com.rail.api.entity.IntentionStatus;
import com.rail.api.entity.Milestone;
import com.rail.api.entity.NextGoalProposal;
import com.rail.api.entity.NextGoalProposalStatus;
import com.rail.api.entity.OccurrenceStatus;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.User;
import com.rail.api.entity.UserConnieLog;
import com.rail.api.entity.UserConnieLogType;
import com.rail.api.event.NextGoalProposalCreatedEvent;
import com.rail.api.intelligence.GoalBlueprint;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.GoalTargetRepository;
import com.rail.api.repository.MilestoneRepository;
import com.rail.api.repository.NextGoalProposalRepository;
import com.rail.api.repository.TaskOccurrenceRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.TaskTargetRepository;
import com.rail.api.repository.UserConnieLogRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NextGoalGenerationService {

    @Value("${rail.connie.model.refiner:deepseek-v4-pro}")
    private String model;

    @Value("${rail.llm.max-json-retries:2}")
    private int maxRetries;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final GoalRepository goalRepository;
    private final GoalTargetRepository goalTargetRepository;
    private final TaskRepository taskRepository;
    private final TaskTargetRepository taskTargetRepository;
    private final TaskOccurrenceRepository occurrenceRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserSchedulingProfileRepository profileRepository;
    private final UserConnieLogRepository connieLogRepository;
    private final NextGoalProposalRepository nextGoalProposalRepository;
    private final ChatService chatService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void generateNextGoal(User user, UUID completedGoalPid) {
        Goal completedGoal = goalRepository.findByPid(completedGoalPid).orElse(null);
        if (completedGoal == null) {
            log.warn("[next-goal] completed goal {} not found — skipping", completedGoalPid);
            return;
        }
        Intention intention = completedGoal.getIntention();

        if (intention.getStatus() != IntentionStatus.ACTIVE) {
            log.info("[next-goal] skipping — intention {} is {}", intention.getPid(), intention.getStatus());
            return;
        }

        if (goalRepository.findByIntentionAndStatus(intention, GoalStatus.ACTIVE).isPresent()) {
            log.info("[next-goal] skipping — active goal already exists for intention {}", intention.getPid());
            return;
        }

        String systemPrompt = buildSystemPrompt(user, intention);
        GoalBlueprint blueprint = callLlm(systemPrompt);

        if (blueprint == null) {
            log.error("[next-goal] LLM failed to produce a valid GoalBlueprint for intention {}", intention.getPid());
            return;
        }

        Chat globalChat = chatService.getChat(user);

        NextGoalProposal proposal = nextGoalProposalRepository.saveAndFlush(
            NextGoalProposal.builder()
                .owner(user)
                .intention(intention)
                .chat(globalChat)
                .goalBlueprint(blueprint)
                .status(NextGoalProposalStatus.REFINING)
                .build()
        );

        log.info("[next-goal] created proposal {} for intention {}", proposal.getPid(), intention.getPid());
        eventPublisher.publishEvent(new NextGoalProposalCreatedEvent(user, proposal.getPid()));
    }

    private GoalBlueprint callLlm(String systemPrompt) {
        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage("Generate the next goal blueprint.")
        );

        log.info("[next-goal] calling LLM model={}", model);
        String raw = invoke(messages);
        GoalBlueprint blueprint = parse(raw);

        for (
            int attempt = 1;
            attempt <= maxRetries && blueprint == null;
            attempt++
        ) {
            var retry = new ArrayList<Message>(messages);
            retry.add(new AssistantMessage(raw != null ? raw : ""));
            retry.add(
                new UserMessage(
                    "Your response was not valid JSON matching the GoalBlueprint schema. " +
                        "Return ONLY the JSON object with no explanation or markdown fences."
                )
            );
            log.warn(
                "[next-goal] parse failed — retrying (attempt {})",
                attempt
            );
            raw = invoke(retry);
            blueprint = parse(raw);
        }

        return blueprint;
    }

    private String invoke(List<Message> messages) {
        var options = OpenAiChatOptions.builder()
            .model(model)
            .responseFormat(
                new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, null)
            )
            .reasoningEffort("high")
            .build();
        var sb = new StringBuilder();
        try {
            ChatClient.create(chatModel)
                .prompt()
                .messages(messages)
                .options(options)
                .stream()
                .content()
                .doOnNext(sb::append)
                .blockLast();
        } catch (Exception e) {
            log.error("[next-goal] LLM stream error: {}", e.getMessage(), e);
            return null;
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private GoalBlueprint parse(String raw) {
        if (raw == null) return null;
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed
                .replaceFirst("^```[a-zA-Z]*\\n?", "")
                .replaceAll("\\n?```$", "")
                .strip();
        }
        try {
            return objectMapper.readValue(trimmed, GoalBlueprint.class);
        } catch (Exception e) {
            log.warn(
                "[next-goal] failed to parse blueprint: {}",
                e.getMessage()
            );
            return null;
        }
    }

    private String buildSystemPrompt(User user, Intention intention) {
        var sb = new StringBuilder();

        sb.append(
            "You are Rail's Goal Generation engine. A user just completed a goal within an ongoing intention.\n"
        );
        sb.append(
            "Your job is to generate the next goal blueprint that moves the user meaningfully forward on their intention.\n\n"
        );

        sb.append(intentionSection(intention)).append("\n\n");
        sb.append(completedGoalsSection(intention)).append("\n\n");
        sb.append(userProfileSection(user)).append("\n\n");
        sb.append(connieLogsSection(user)).append("\n\n");
        sb.append(blueprintSchemaSection());

        return sb.toString();
    }

    private String intentionSection(Intention intention) {
        return """
        ════════════════════════════════════════
        INTENTION
        ════════════════════════════════════════
        Title: %s
        Type: %s
        Completion criteria: %s
        Context from refinement conversation:
        %s
        """.formatted(
            intention.getTitle(),
            intention.getType(),
            intention.getCompletionCriteria() != null
                ? intention.getCompletionCriteria()
                : "none (unbounded)",
            intention.getContext() != null
                ? intention.getContext()
                : "not available"
        );
    }

    private String completedGoalsSection(Intention intention) {
        List<Goal> goals =
            goalRepository.findByIntentionAndStatusOrderByCreatedAtAsc(
                intention,
                GoalStatus.COMPLETED
            );

        if (goals.isEmpty()) return "";

        var sb = new StringBuilder();
        sb.append("════════════════════════════════════════\n");
        sb.append("COMPLETED GOALS (oldest → newest)\n");
        sb.append("════════════════════════════════════════\n\n");

        for (int i = 0; i < goals.size(); i++) {
            sb.append(goalSection(i + 1, goals.get(i)));
            if (i < goals.size() - 1) sb.append("\n");
        }

        return sb.toString();
    }

    private String goalSection(int index, Goal goal) {
        var sb = new StringBuilder();
        sb.append("Goal %d: \"%s\"\n".formatted(index, goal.getTitle()));
        sb.append(
            "  Type: %s  Energy: %s\n".formatted(
                goal.getType(),
                goal.getEnergyLevel()
            )
        );
        sb.append(
            "  Estimated: %dh  Actual: %dh\n".formatted(
                goal.getEstimatedTotalHours(),
                goal.getActualTotalHours()
            )
        );

        if (goal.getCreatedAt() != null) sb.append(
            "  Started: %s\n".formatted(
                goal.getCreatedAt().toString().substring(0, 10)
            )
        );
        if (goal.getCompletedAt() != null) sb.append(
            "  Completed: %s\n".formatted(
                goal.getCompletedAt().toString().substring(0, 10)
            )
        );
        if (goal.getTargetDate() != null) sb.append(
            "  Target date was: %s\n".formatted(goal.getTargetDate())
        );
        if (
            goal.getCompletionNotes() != null &&
            !goal.getCompletionNotes().isBlank()
        ) sb.append(
            "  Completion notes: %s\n".formatted(goal.getCompletionNotes())
        );
        if (
            goal.getBlockReason() != null && !goal.getBlockReason().isBlank()
        ) sb.append("  Was blocked: %s\n".formatted(goal.getBlockReason()));

        sb.append("\n").append(milestonesSection(goal));
        sb.append("\n").append(tasksSection(goal));

        return sb.toString();
    }

    private String milestonesSection(Goal goal) {
        List<Milestone> milestones =
            milestoneRepository.findByGoalOrderByPosition(goal);
        if (milestones.isEmpty()) return "";

        var sb = new StringBuilder("  Milestones:\n");
        for (Milestone m : milestones) {
            String tick =
                m.getStatus() == com.rail.api.entity.MilestoneStatus.DONE
                    ? "✓"
                    : "✗";
            String achieved =
                m.getAchievedAt() != null
                    ? " — achieved %s".formatted(
                          m.getAchievedAt().toString().substring(0, 10)
                      )
                    : " — not reached";
            sb.append(
                "    [%s] %s%s\n".formatted(tick, m.getTitle(), achieved)
            );
        }
        return sb.toString();
    }

    private String tasksSection(Goal goal) {
        List<Task> tasks = taskRepository.findByGoal(goal);
        if (tasks.isEmpty()) return "";

        boolean isRecurring =
            goal.getType() == GoalType.HABIT ||
            goal.getType() == GoalType.ABSTINENCE;

        if (isRecurring) {
            return habitStreakSection(goal, tasks);
        }

        var sb = new StringBuilder("  Tasks:\n");
        for (Task t : tasks) {
            sb.append("    \"%s\"  %s".formatted(t.getTitle(), t.getStatus()));
            if (t.getCompletionType() != null) sb.append(
                " (%s)".formatted(t.getCompletionType())
            );
            if (
                t.getCompletionNote() != null &&
                !t.getCompletionNote().isBlank()
            ) sb.append(" — \"%s\"".formatted(t.getCompletionNote()));
            if (
                t.getMissReason() != null && !t.getMissReason().isBlank()
            ) sb.append(
                " — missed because: \"%s\"".formatted(t.getMissReason())
            );
            sb.append("\n");
        }

        // QUANTIFIED: show target vs actual with percentage and per-task breakdown
        goalTargetRepository.findByGoal(goal).ifPresent(gt -> {
            BigDecimal actual =
                taskTargetRepository.sumActualValueByGoalAndTaskStatus(
                    goal,
                    TaskStatus.DONE
                );
            if (actual == null) actual = BigDecimal.ZERO;
            int pct =
                gt.getTargetValue().compareTo(BigDecimal.ZERO) > 0
                    ? actual
                          .multiply(BigDecimal.valueOf(100))
                          .divide(
                              gt.getTargetValue(),
                              0,
                              java.math.RoundingMode.HALF_UP
                          )
                          .intValue()
                    : 0;
            sb.append(
                "  Quantified progress: %s / %s %s (%d%%)\n".formatted(
                    actual.toPlainString(),
                    gt.getTargetValue().toPlainString(),
                    gt.getUnit(),
                    pct
                )
            );
            sb.append("  Per-task contributions:\n");
            for (Task t : tasks) {
                taskTargetRepository.findByTask(t).ifPresent(tt -> {
                    String actual2 =
                        tt.getActualValue() != null
                            ? tt.getActualValue().toPlainString()
                            : "—";
                    sb.append(
                        "    \"%s\"  estimated: %s  actual: %s  status: %s\n".formatted(
                            t.getTitle(),
                            tt.getEstimatedValue().toPlainString(),
                            actual2,
                            t.getStatus()
                        )
                    );
                });
            }
        });

        return sb.toString();
    }

    private String habitStreakSection(Goal goal, List<Task> tasks) {
        List<com.rail.api.entity.TaskOccurrence> occurrences =
            occurrenceRepository.findByTaskInOrderByOccurrenceDateDesc(tasks);

        long fullDone = occurrences
            .stream()
            .filter(
                o ->
                    o.getStatus() == OccurrenceStatus.DONE &&
                    o.getCompletionType() !=
                        com.rail.api.entity.TaskCompletionType.PARTIAL
            )
            .count();
        long partial = occurrences
            .stream()
            .filter(
                o ->
                    o.getStatus() == OccurrenceStatus.DONE &&
                    o.getCompletionType() ==
                        com.rail.api.entity.TaskCompletionType.PARTIAL
            )
            .count();
        long missed = occurrences
            .stream()
            .filter(o -> o.getStatus() == OccurrenceStatus.MISSED)
            .count();
        long skipped = occurrences
            .stream()
            .filter(o -> o.getStatus() == OccurrenceStatus.SKIPPED)
            .count();
        long total = occurrences.size();

        long completed = fullDone + partial;
        int pct = total > 0 ? (int) Math.round((completed * 100.0) / total) : 0;

        var sb = new StringBuilder(
            "  Habit occurrences (%d scheduled):\n".formatted(total)
        );
        sb.append(
            "    Done: %d  Partial: %d  Missed: %d  Skipped: %d  (%d%% completion rate)\n".formatted(
                fullDone,
                partial,
                missed,
                skipped,
                pct
            )
        );

        List<String> partialNotes = occurrences
            .stream()
            .filter(
                o ->
                    o.getCompletionType() ==
                        com.rail.api.entity.TaskCompletionType.PARTIAL &&
                    o.getCompletionNote() != null &&
                    !o.getCompletionNote().isBlank()
            )
            .map(o -> "\"%s\"".formatted(o.getCompletionNote()))
            .distinct()
            .toList();
        if (!partialNotes.isEmpty()) sb.append(
            "    Partial notes: %s\n".formatted(String.join(", ", partialNotes))
        );

        List<String> missNotes = occurrences
            .stream()
            .filter(
                o ->
                    o.getStatus() == OccurrenceStatus.MISSED &&
                    o.getCompletionNote() != null &&
                    !o.getCompletionNote().isBlank()
            )
            .map(o -> "\"%s\"".formatted(o.getCompletionNote()))
            .distinct()
            .toList();
        if (!missNotes.isEmpty()) sb.append(
            "    Miss notes: %s\n".formatted(String.join(", ", missNotes))
        );

        return sb.toString();
    }

    private String userProfileSection(User user) {
        return profileRepository
            .findByUser(user)
            .map(p ->
                """
                ════════════════════════════════════════
                USER PROFILE
                ════════════════════════════════════════
                Name: %s
                Timezone: %s
                Wake: %s · Sleep: %s
                Deep work: %s–%s
                Energy pattern: %s
                """.formatted(
                    user.getDisplayName(),
                    p.getTimezone(),
                    p.getWakeTime(),
                    p.getSleepTime(),
                    p.getDeepWorkStart(),
                    p.getDeepWorkEnd(),
                    p.getEnergyPattern()
                )
            )
            .orElse("");
    }

    private String connieLogsSection(User user) {
        List<UserConnieLog> logs =
            connieLogRepository.findByUserOrderByCreatedAtAsc(user);
        if (logs.isEmpty()) return "";

        var sb = new StringBuilder();
        sb.append("════════════════════════════════════════\n");
        sb.append("CONNIE'S KNOWLEDGE OF THIS USER\n");
        sb.append("════════════════════════════════════════\n\n");

        for (UserConnieLog log : logs) {
            String label =
                log.getType() == UserConnieLogType.EVOLUTIONARY
                    ? "Evolutionary summary (%d entries merged, %s → %s)".formatted(
                          log.getMergedCount() != null
                              ? log.getMergedCount()
                              : 0,
                          log.getPeriodStart(),
                          log.getPeriodEnd()
                      )
                    : "Daily analysis (%s)".formatted(log.getPeriodStart());
            sb.append("── %s ──\n".formatted(label));
            if (
                log.getObservedPatterns() != null &&
                !log.getObservedPatterns().isBlank()
            ) sb.append(
                "Observed:\n%s\n".formatted(log.getObservedPatterns().strip())
            );
            if (
                log.getStatedPreferences() != null &&
                !log.getStatedPreferences().isBlank()
            ) sb.append(
                "Stated preferences:\n%s\n".formatted(
                    log.getStatedPreferences().strip()
                )
            );
            sb.append("\n");
        }

        return sb.toString().strip();
    }

    private String blueprintSchemaSection() {
        return """
        ════════════════════════════════════════
        OUTPUT
        ════════════════════════════════════════
        Return a single JSON object matching this schema exactly. No explanation, no markdown fences.

        {
          "goalType": "HABIT | ABSTINENCE | PROJECT | TASK | QUANTIFIED",
          "energyLevel": "DEEP | LIGHT | ADMIN",
          "estimatedHours": 40,
          "daysUntilTarget": 30,
          "target": {"targetValue": 1000.00, "unit": "usd"},
          "earliestStartDate": "2007-12-03",
          "recurrence": {
            "frequency": "DAILY | WEEKLY | MONTHLY | CUSTOM",
            "timesPerPeriod": 1,
            "days": [{"dayOfWeek": "MONDAY", "preferredTime": "08:00"}]
          },
          "milestones": [{"title": "checkpoint", "daysUntilTarget": 14}],
          "tasks": [
            {
              "title": "Task title",
              "notes": null,
              "durationMinutes": 30,
              "priority": "HIGH | MEDIUM | LOW",
              "milestoneIndex": 0,
              "estimatedValue": null,
              "flexibility": "FLEXIBLE | FIXED",
              "fixedTime": null,
              "deadline": null
            }
          ]
        }

        RULES:
        - recurrence: required for HABIT and ABSTINENCE, null otherwise
        - target: required for QUANTIFIED, null otherwise
        - milestones: use streak milestones for habits (7, 30, 90 days), empty for TASK
        - tasks for HABIT/ABSTINENCE: ONE task per distinct activity — the scheduler repeats it automatically
        - tasks for PROJECT/TASK/QUANTIFIED: 3–7 concrete tasks for the first meaningful unit of work
        - flexibility: "FIXED" only when a specific clock time is known, otherwise "FLEXIBLE"
        - fixedTime: "HH:mm" 24-hour only for FIXED tasks, null otherwise
        - daysUntilTarget: days from today to the goal's deadline; null if no deadline
        - earliestStartDate: the earliest any task from this goal should start
        - Look at what the user struggled with or skipped in prior goals and adjust the next goal accordingly
        """;
    }
}
