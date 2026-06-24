package com.rail.api.context;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.NextGoalProposal;
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
        List<ChatMessage> history = strategy.fetchHistory(chat, messageRepository, activeProposal);
        return buildContext(chat, userInput, history, Optional.ofNullable(activeProposal), Optional.empty());
    }

    public ConversationContext build(
        Chat chat,
        String userInput,
        ContextStrategy strategy,
        NextGoalProposal activeNextGoalProposal
    ) {
        List<ChatMessage> history = strategy.fetchHistory(chat, messageRepository);
        return buildContext(chat, userInput, history, Optional.empty(), Optional.ofNullable(activeNextGoalProposal));
    }

    private ConversationContext buildContext(
        Chat chat,
        String userInput,
        List<ChatMessage> history,
        Optional<IntentionProposal> activeProposal,
        Optional<NextGoalProposal> activeNextGoalProposal
    ) {
        Optional<UserSchedulingProfile> schedulingProfile =
            schedulingProfileRepository.findByUser(chat.getUser());
        List<UserConnieLog> recentLogs = connieLogRepository.findRecentN(chat.getUser(), connieLogsContextLimit);
        List<UserConnieLog> connieLogs = new ArrayList<>(recentLogs);
        java.util.Collections.reverse(connieLogs);

        return new ConversationContext(
            chat.getUser(),
            chat,
            history,
            activeProposal,
            activeNextGoalProposal,
            userInput,
            ZonedDateTime.now(),
            schedulingProfile,
            connieLogs
        );
    }
}
