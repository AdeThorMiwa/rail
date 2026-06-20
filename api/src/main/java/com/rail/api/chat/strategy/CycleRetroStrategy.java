package com.rail.api.chat.strategy;

import com.rail.api.context.ContextStrategy;
import com.rail.api.context.ConversationContext;
import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.Task;
import com.rail.api.entity.UserCycle;
import com.rail.api.intelligence.RetroAnalysis;
import com.rail.api.repository.ChatMessageRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CycleRetroStrategy implements ContextStrategy {

    private final UserCycle cycle;
    private final RetroAnalysis retroAnalysis;
    private final List<Task> carryOverCandidates;
    private final String priorRetroSummary;
    private final String connieModel;

    public CycleRetroStrategy(
        UserCycle cycle,
        RetroAnalysis retroAnalysis,
        List<Task> carryOverCandidates,
        String priorRetroSummary,
        String connieModel
    ) {
        this.cycle = cycle;
        this.retroAnalysis = retroAnalysis;
        this.carryOverCandidates = carryOverCandidates;
        this.priorRetroSummary = priorRetroSummary;
        this.connieModel = connieModel;
    }

    @Override
    public String model() { return connieModel; }

    @Override
    public List<ChatMessage> fetchHistory(Chat chat, ChatMessageRepository repo) {
        return ContextStrategy.lastN(chat, repo, 40);
    }

    @Override
    public String systemPrompt(ConversationContext ctx) {
        if (cycle == null) {
            return """
                You are Connie, Rail's intelligence layer (Retrospective Guide mode). This cycle could not be found.
                Apologise briefly and ask the user to return to the home screen.
                Every response MUST be valid JSON: {"blocks":[{"type":"text","spans":[{"type":"text","text":"..."}]}]}
                """;
        }

        return """
            You are Connie, Rail's intelligence layer. In this session you are the Retrospective Guide — you run the end-of-cycle retrospective, surface wins and lessons, and wrap up the cycle.
            The user's cycle has just ended. Be warm, celebratory of wins, honest about shortfalls, and forward-looking.

            %s

            ════════════════════════════════════════
            YOUR ROLE
            ════════════════════════════════════════

            OPENING MESSAGE (trigger: "Cycle ended. Start the retrospective."):
            - Open warmly. Acknowledge the cycle has ended.
            - Share the headline stats goal by goal — be specific and honest. Use the numbers.
            - End with ONE open question: "How are you feeling about this cycle overall?"
            - Keep it tight. Celebration + reflection, not a lecture.

            PHASE 1 — REFLECTION:
            - Listen. Ask 1–2 follow-up questions max.
            - Surface specific wins from the stats: tasks completed, streaks held.
            - Be compassionate about gaps: "you skipped a few workouts — what got in the way?"
            - Help the user name their key wins and a lesson. You'll use these in the wrap card.

            PHASE 2 — CARRY-OVERS (only if tasks are listed in CARRY-OVER TASKS):
            Present the unfinished FLEXIBLE PENDING tasks, grouped by goal.
            For each group: name the goal, list tasks briefly, ask "keep or drop?".
            Once decided, call resolveCarryOvers with keepPids and dropPids.
            Skip Phase 2 entirely if there are no carry-over tasks.

            PHASE 3 — WRAP-UP:
            After reflection (and carry-overs if applicable):
            1. Summarise the cycle in 2–3 sentences — this becomes your summary field.
            2. Gather the key wins from the conversation (max 5 items).
            3. Call concludeRetro with the summary text and key wins list.
            4. In the SAME response as the concludeRetro tool call, include a wrap_card block
               followed immediately by an actions block.

            WRAP CARD FORMAT (include in blocks array):
            {
              "type": "wrap_card",
              "cycleTitle": "<cycle title>",
              "period": "<startDate formatted as 'Mon D'> – <endDate formatted as 'Mon D'>",
              "focusGoals": [
                {"title": "...", "completionRate": 0.75, "completed": 6, "total": 8}
              ],
              "habitHighlights": [
                {"title": "...", "adherenceRate": 0.85}
              ],
              "keyWins": ["...", "..."],
              "summary": "<2–3 sentence summary>"
            }

            Follow it immediately with an actions block:
            {
              "type": "actions",
              "items": [{
                "id": "next-cycle",
                "label": "Start Next Cycle →",
                "style": "primary",
                "command": "navigation.navigate",
                "params": {"path": "/cycles"}
              }]
            }

            STATED PREFERENCES:
            If during the retrospective the user reveals a clear, durable preference about how they want their schedule or habits managed, call updatePreference silently. Common examples: "I realised I work better with fewer tasks", "mornings were my best time", "I should stop scheduling gym on Fridays". Do not mention to the user that you saved it.

            RULES:
            - Use exact numbers from CYCLE STATS. Do not invent rates.
            - completionRate / adherenceRate / resistanceRate must be decimal 0.0–1.0.
            - Only include habitHighlights if HABIT or ABSTINENCE goals are listed in stats.
            - focusGoals in wrap card: only PROJECT, TASK, QUANTIFIED goals (not HABIT/ABSTINENCE).
            - Call resolveCarryOvers only if carry-over tasks exist.
            - Call concludeRetro exactly once — in the final wrap-up response.
            - Include wrap_card and actions in the SAME response as the concludeRetro tool call.

            ════════════════════════════════════════
            OUTPUT FORMAT
            ════════════════════════════════════════

            Every response MUST be valid JSON: {"blocks":[...]}

            Text block:
            {"type":"text","spans":[{"type":"text","text":"Your response here."}]}

            SELF-CHECK before returning:
            - Starts with {"blocks":[ and ends with ]}?
            - No markdown?
            Fix every failure before returning.
            """.formatted(ContextStrategy.userProfileSection(ctx) + "\n\n" + ContextStrategy.connieLogsSection(ctx) + "\n\n" + buildContextBlock());
    }

    private String buildContextBlock() {
        String cycleContext = """
            ════════════════════════════════════════
            CYCLE CONTEXT
            ════════════════════════════════════════

            Cycle: "%s"
            Period: %s → %s
            Status: in review
            """.formatted(cycle.getTitle(), cycle.getStartDate(), cycle.getEndDate());

        String statsBody = retroAnalysis == null
            ? "Stats not available — use the conversation to surface wins and lessons.\n"
            : buildFocusGoalStats() + buildHabitStats() + buildAbstinenceStats();

        String cycleStats = """
            ════════════════════════════════════════
            CYCLE STATS
            ════════════════════════════════════════

            %s""".formatted(statsBody);

        return Stream.of(cycleContext, cycleStats, buildCarryOverSection(), buildPriorRetroSection())
            .filter(s -> !s.isBlank())
            .collect(Collectors.joining("\n"))
            .strip();
    }

    private String buildFocusGoalStats() {
        List<RetroAnalysis.FocusGoalStats> stats = retroAnalysis.focusGoals();
        if (stats == null || stats.isEmpty()) return "";

        String rows = stats.stream()
            .map(g -> {
                int pct = (int) Math.round(g.completionRate() * 100);
                String skipped = g.skippedTasks() > 0 ? ", %d skipped".formatted(g.skippedTasks()) : "";
                return "  - \"%s\" [%s]\n    Tasks: %d done / %d total (%d%%%s)".formatted(
                    g.goalTitle(), g.goalType(), g.completedTasks(), g.totalTasks(), pct, skipped);
            })
            .collect(Collectors.joining("\n"));

        return "FOCUS GOAL COMPLETION (PROJECT / TASK / QUANTIFIED):\n%s\n\n".formatted(rows);
    }

    private String buildHabitStats() {
        List<RetroAnalysis.HabitStats> stats = retroAnalysis.habitStats();
        if (stats == null || stats.isEmpty()) return "";

        String rows = stats.stream()
            .map(h -> {
                int pct = (int) Math.round(h.adherenceRate() * 100);
                String missed = h.missed() > 0 ? ", %d missed".formatted(h.missed()) : "";
                String skipped = h.skipped() > 0 ? ", %d skipped".formatted(h.skipped()) : "";
                return "  - \"%s\"\n    %d done / %d total (%d%% adherence%s%s)".formatted(
                    h.goalTitle(), h.done(), h.totalOccurrences(), pct, missed, skipped);
            })
            .collect(Collectors.joining("\n"));

        return "HABIT ADHERENCE:\n%s\n\n".formatted(rows);
    }

    private String buildAbstinenceStats() {
        List<RetroAnalysis.AbstinenceStats> stats = retroAnalysis.abstinenceStats();
        if (stats == null || stats.isEmpty()) return "";

        String rows = stats.stream()
            .map(a -> {
                int pct = (int) Math.round(a.resistanceRate() * 100);
                String lapses = a.lapses() > 0 ? " — %d lapse(s)".formatted(a.lapses()) : "";
                return "  - \"%s\"\n    %d%% resistance rate%s".formatted(a.goalTitle(), pct, lapses);
            })
            .collect(Collectors.joining("\n"));

        return "ABSTINENCE TRACKING:\n%s\n\n".formatted(rows);
    }

    private String buildCarryOverSection() {
        if (carryOverCandidates == null || carryOverCandidates.isEmpty()) return "";

        String taskList = carryOverCandidates.stream()
            .map(t -> "  - \"%s\" [goal:\"%s\" · %dmin · pid:%s]".formatted(
                t.getTitle(),
                t.getGoal().getTitle(),
                t.getDurationMinutes() != null ? t.getDurationMinutes() : 60,
                t.getPid()
            ))
            .collect(Collectors.joining("\n"));

        return """
            ════════════════════════════════════════
            CARRY-OVER TASKS (unfinished from this cycle)
            ════════════════════════════════════════

            These FLEXIBLE PENDING tasks were not completed. Ask the user whether to keep or drop each one.
            HABIT and ABSTINENCE tasks are excluded — they recur automatically.

            %s

            """.formatted(taskList);
    }

    private String buildPriorRetroSection() {
        if (priorRetroSummary == null || priorRetroSummary.isBlank()) return "";

        return """
            ════════════════════════════════════════
            PRIOR CYCLE CONTEXT
            ════════════════════════════════════════

            Summary from the previous cycle's retrospective (for longitudinal reflection):
            "%s"

            """.formatted(priorRetroSummary);
    }
}
