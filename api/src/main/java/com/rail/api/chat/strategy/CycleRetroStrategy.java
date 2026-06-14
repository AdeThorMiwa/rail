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

public class CycleRetroStrategy implements ContextStrategy {

    private final UserCycle cycle;
    private final RetroAnalysis retroAnalysis;
    private final List<Task> carryOverCandidates;
    private final String priorRetroSummary;

    public CycleRetroStrategy(
        UserCycle cycle,
        RetroAnalysis retroAnalysis,
        List<Task> carryOverCandidates,
        String priorRetroSummary
    ) {
        this.cycle = cycle;
        this.retroAnalysis = retroAnalysis;
        this.carryOverCandidates = carryOverCandidates;
        this.priorRetroSummary = priorRetroSummary;
    }

    @Override
    public List<ChatMessage> fetchHistory(Chat chat, ChatMessageRepository repo) {
        return ContextStrategy.lastN(chat, repo, 40);
    }

    @Override
    public String systemPrompt(ConversationContext ctx) {
        if (cycle == null) {
            return """
                You are Connie. This cycle could not be found.
                Apologise briefly and ask the user to return to the home screen.
                Every response MUST be valid JSON: {"blocks":[{"type":"text","spans":[{"type":"text","text":"..."}]}]}
                """;
        }

        return """
            You are Connie, a warm productivity companion in the Rail app.
            The user's cycle has just ended — you're running their retrospective together.
            Be warm, celebratory of wins, honest about shortfalls, and forward-looking.

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
            """.formatted(buildContextBlock());
    }

    private String buildContextBlock() {
        StringBuilder sb = new StringBuilder();

        sb.append("════════════════════════════════════════\n");
        sb.append("CYCLE CONTEXT\n");
        sb.append("════════════════════════════════════════\n\n");
        sb.append("Cycle: \"").append(cycle.getTitle()).append("\"\n");
        sb.append("Period: ").append(cycle.getStartDate()).append(" → ").append(cycle.getEndDate()).append("\n");
        sb.append("Status: in review\n");

        sb.append("\n════════════════════════════════════════\n");
        sb.append("CYCLE STATS\n");
        sb.append("════════════════════════════════════════\n\n");

        if (retroAnalysis == null) {
            sb.append("Stats not available — use the conversation to surface wins and lessons.\n");
        } else {
            appendFocusGoalStats(sb);
            appendHabitStats(sb);
            appendAbstinenceStats(sb);
        }

        String carryOverSection = buildCarryOverSection();
        if (!carryOverSection.isBlank()) {
            sb.append("\n").append(carryOverSection);
        }

        String priorSection = buildPriorRetroSection();
        if (!priorSection.isBlank()) {
            sb.append("\n").append(priorSection);
        }

        return sb.toString().strip();
    }

    private void appendFocusGoalStats(StringBuilder sb) {
        List<RetroAnalysis.FocusGoalStats> stats = retroAnalysis.focusGoals();
        if (stats == null || stats.isEmpty()) return;

        sb.append("FOCUS GOAL COMPLETION (PROJECT / TASK / QUANTIFIED):\n");
        for (RetroAnalysis.FocusGoalStats g : stats) {
            int pct = (int) Math.round(g.completionRate() * 100);
            sb.append("  - \"").append(g.goalTitle()).append("\" [").append(g.goalType()).append("]\n");
            sb.append("    Tasks: ").append(g.completedTasks()).append(" done / ")
              .append(g.totalTasks()).append(" total (").append(pct).append("%)");
            if (g.skippedTasks() > 0) {
                sb.append(", ").append(g.skippedTasks()).append(" skipped");
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private void appendHabitStats(StringBuilder sb) {
        List<RetroAnalysis.HabitStats> stats = retroAnalysis.habitStats();
        if (stats == null || stats.isEmpty()) return;

        sb.append("HABIT ADHERENCE:\n");
        for (RetroAnalysis.HabitStats h : stats) {
            int pct = (int) Math.round(h.adherenceRate() * 100);
            sb.append("  - \"").append(h.goalTitle()).append("\"\n");
            sb.append("    ").append(h.done()).append(" done / ")
              .append(h.totalOccurrences()).append(" total (").append(pct).append("% adherence)");
            if (h.missed() > 0) sb.append(", ").append(h.missed()).append(" missed");
            if (h.skipped() > 0) sb.append(", ").append(h.skipped()).append(" skipped");
            sb.append("\n");
        }
        sb.append("\n");
    }

    private void appendAbstinenceStats(StringBuilder sb) {
        List<RetroAnalysis.AbstinenceStats> stats = retroAnalysis.abstinenceStats();
        if (stats == null || stats.isEmpty()) return;

        sb.append("ABSTINENCE TRACKING:\n");
        for (RetroAnalysis.AbstinenceStats a : stats) {
            int pct = (int) Math.round(a.resistanceRate() * 100);
            sb.append("  - \"").append(a.goalTitle()).append("\"\n");
            sb.append("    ").append(pct).append("% resistance rate");
            if (a.lapses() > 0) sb.append(" — ").append(a.lapses()).append(" lapse(s)");
            sb.append("\n");
        }
        sb.append("\n");
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
