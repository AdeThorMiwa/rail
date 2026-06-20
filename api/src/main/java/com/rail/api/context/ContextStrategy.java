package com.rail.api.context;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.UserConnieLog;
import com.rail.api.entity.UserConnieLogType;
import com.rail.api.repository.ChatMessageRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

public interface ContextStrategy {

    List<ChatMessage> fetchHistory(Chat chat, ChatMessageRepository repo);

    String systemPrompt(ConversationContext ctx);

    default String model() {
        return "deepseek-v4-flash";
    }

    static List<ChatMessage> lastN(Chat chat, ChatMessageRepository repo, int n) {
        return repo.findByChatOrderByCreatedAtDesc(chat, PageRequest.of(0, n))
            .stream()
            .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
            .toList();
    }

    static String connieLogsSection(ConversationContext ctx) {
        List<UserConnieLog> logs = ctx.connieLogs();
        if (logs == null || logs.isEmpty()) return "";

        String header = """
            ════════════════════════════════════════
            CONNIE'S KNOWLEDGE OF THIS USER
            ════════════════════════════════════════

            The following is Connie's accumulated understanding of this user — built from observed behavioural patterns and things they have explicitly stated. Older entries appear first; more recent entries carry more weight. Use this to make smarter, more personalised decisions.

            """;

        String body = logs.stream()
            .map(log -> {
                String typeLabel = log.getType() == UserConnieLogType.EVOLUTIONARY
                    ? "Evolutionary summary (%d entries merged, %s → %s)".formatted(
                        log.getMergedCount() != null ? log.getMergedCount() : 0,
                        log.getPeriodStart(), log.getPeriodEnd())
                    : "Daily analysis (%s)".formatted(log.getPeriodStart());

                List<String> parts = new ArrayList<>();
                parts.add("── %s ──".formatted(typeLabel));
                if (log.getObservedPatterns() != null && !log.getObservedPatterns().isBlank())
                    parts.add("Observed:\n%s".formatted(log.getObservedPatterns().strip()));
                if (log.getStatedPreferences() != null && !log.getStatedPreferences().isBlank())
                    parts.add("Stated preferences:\n%s".formatted(log.getStatedPreferences().strip()));
                return parts.stream().collect(Collectors.joining("\n"));
            })
            .collect(Collectors.joining("\n\n"));

        return (header + body).strip();
    }

    static String userProfileSection(ConversationContext ctx) {
        String scheduleLines = ctx.schedulingProfile()
            .map(p -> """
                Timezone: %s
                Wake: %s · Sleep: %s
                Deep work: %s–%s
                Energy pattern: %s
                """.formatted(
                    p.getTimezone(),
                    p.getWakeTime(), p.getSleepTime(),
                    p.getDeepWorkStart(), p.getDeepWorkEnd(),
                    p.getEnergyPattern()
                ))
            .orElse("");

        return """
            ════════════════════════════════════════
            USER PROFILE
            ════════════════════════════════════════

            Name: %s
            %s""".formatted(ctx.user().getDisplayName(), scheduleLines).strip();
    }
}
