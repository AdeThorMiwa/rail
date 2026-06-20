package com.rail.api.connie;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConniePatternAnalystClient {

    @Value("${rail.connie.model.analyst:deepseek-v4-flash}")
    private String analystModel;

    private final ChatModel chatModel;

    public String enrichObservations(String algorithmicSummary, String userName) {
        String systemPrompt = """
            You are Connie, Rail's intelligence layer. In this session you are the Pattern Analyst — you generate rich, human-readable behavioural observations from a user's scheduling data.

            Your observations will be stored in Connie's memory and read by other Connie modes (Scheduler, Intention Refiner, Cycle Planner, etc.) to make smarter, more personalised decisions. Write as if briefing a thoughtful colleague who will use this to help the user.

            Rules:
            - Write 3–6 observations as a plain numbered list. One sentence each.
            - Synthesise meaning — do not just restate the numbers.
            - Focus on what is actionable: what the scheduler should do differently, what patterns to honour, what to watch for.
            - Note any change or trajectory if the data suggests it (e.g. "completion has been improving").
            - If data is thin or inconclusive, say so briefly and note what to watch.
            - Do not mention the user by name. Write in third person ("this user", "they").
            - Plain text only. No markdown, no headers.
            """;

        String userMessage = """
            User: %s

            Algorithmic analysis (last 30 days):
            %s

            Write 3–6 observations about this user's patterns that will help Connie make smarter scheduling and planning decisions.
            """.formatted(userName, algorithmicSummary);

        try {
            var options = OpenAiChatOptions.builder()
                .model(analystModel)
                .build();

            return ChatClient.create(chatModel)
                .prompt()
                .messages(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)))
                .options(options)
                .call()
                .content();
        } catch (Exception e) {
            log.warn("[pattern-analyst] LLM enrichment failed: {}", e.getMessage());
            return null;
        }
    }

    public String mergeIntoEvolutionary(List<String> observedPatternsList, List<String> statedPreferencesList, String userName) {
        String combinedPatterns = observedPatternsList.stream()
            .filter(s -> s != null && !s.isBlank())
            .reduce("", (a, b) -> a + "\n---\n" + b)
            .strip();

        String combinedPrefs = statedPreferencesList.stream()
            .filter(s -> s != null && !s.isBlank())
            .reduce("", (a, b) -> a + "\n" + b)
            .strip();

        String systemPrompt = """
            You are Connie, Rail's intelligence layer. In this session you are the Pattern Analyst — you merge multiple historical observation records into a single evolutionary summary.

            The records span a period of time and are ordered oldest to newest. Your job is to synthesise them into one coherent summary that captures:
            1. Patterns that have been consistent throughout
            2. Patterns that have changed — and in what direction (improving, declining, shifting)
            3. Any notable evolution in the user's behaviour over the period

            Rules:
            - Write in plain numbered list format, one observation per line. No markdown, no headers.
            - Focus on trajectory and evolution, not just the average state.
            - For stated preferences: consolidate into a deduplicated list. If preferences contradict across time, note the most recent one and flag that it changed.
            - Keep the result concise — 4–8 observations for patterns, deduplicated list for preferences.
            - Write in third person ("this user", "they").
            """;

        String userMessage = """
            User: %s

            Observation records (oldest first):
            %s

            Stated preferences across the period:
            %s

            Merge these into one evolutionary summary with two sections:
            PATTERNS: (numbered list of evolved observations)
            STATED PREFERENCES: (deduplicated, most-recent-wins list)
            """.formatted(userName, combinedPatterns, combinedPrefs.isBlank() ? "None" : combinedPrefs);

        try {
            var options = OpenAiChatOptions.builder()
                .model(analystModel)
                .build();

            return ChatClient.create(chatModel)
                .prompt()
                .messages(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)))
                .options(options)
                .call()
                .content();
        } catch (Exception e) {
            log.warn("[pattern-analyst] merge LLM call failed: {}", e.getMessage());
            return null;
        }
    }
}
