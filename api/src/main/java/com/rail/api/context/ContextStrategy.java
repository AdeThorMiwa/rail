package com.rail.api.context;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatMessage;
import com.rail.api.repository.ChatMessageRepository;
import java.util.Comparator;
import java.util.List;
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
}
