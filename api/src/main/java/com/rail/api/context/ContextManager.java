package com.rail.api.context;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.UserSchedulingProfile;
import com.rail.api.repository.ChatMessageRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContextManager {

    private final ChatMessageRepository messageRepository;
    private final UserSchedulingProfileRepository schedulingProfileRepository;

    public ConversationContext build(
        Chat chat,
        String userInput,
        ContextStrategy strategy,
        IntentionProposal activeProposal
    ) {
        List<ChatMessage> history = strategy.fetchHistory(chat, messageRepository);
        Optional<UserSchedulingProfile> schedulingProfile =
            schedulingProfileRepository.findByUser(chat.getUser());

        return new ConversationContext(
            chat.getUser(),
            chat,
            history,
            Optional.ofNullable(activeProposal),
            userInput,
            ZonedDateTime.now(),
            schedulingProfile
        );
    }
}
