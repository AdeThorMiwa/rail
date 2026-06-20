package com.rail.api.context;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.UserConnieLog;
import com.rail.api.entity.UserSchedulingProfile;
import com.rail.api.repository.ChatMessageRepository;
import com.rail.api.repository.UserConnieLogRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContextManager {

    @Value("${rail.connie.pattern-analysis.context-limit:7}")
    private int connieLogsContextLimit;

    private final ChatMessageRepository messageRepository;
    private final UserSchedulingProfileRepository schedulingProfileRepository;
    private final UserConnieLogRepository connieLogRepository;

    public ConversationContext build(
        Chat chat,
        String userInput,
        ContextStrategy strategy,
        IntentionProposal activeProposal
    ) {
        List<ChatMessage> history = strategy.fetchHistory(chat, messageRepository);
        Optional<UserSchedulingProfile> schedulingProfile =
            schedulingProfileRepository.findByUser(chat.getUser());
        List<UserConnieLog> recentLogs = connieLogRepository.findRecentN(chat.getUser(), connieLogsContextLimit);
        // findRecentN returns newest-first; reverse to oldest-first for the LLM
        List<UserConnieLog> connieLogs = new ArrayList<>(recentLogs);
        java.util.Collections.reverse(connieLogs);

        return new ConversationContext(
            chat.getUser(),
            chat,
            history,
            Optional.ofNullable(activeProposal),
            userInput,
            ZonedDateTime.now(),
            schedulingProfile,
            connieLogs
        );
    }
}
