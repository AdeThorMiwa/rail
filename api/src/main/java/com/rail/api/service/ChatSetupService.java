package com.rail.api.service;

import com.rail.api.chat.MessageBuilder;
import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.MessageSender;
import com.rail.api.entity.User;
import com.rail.api.repository.ChatMessageRepository;
import com.rail.api.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatSetupService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageBuilder mb;

    @Transactional
    public void initialize(User user) {
        if (chatRepository.existsByUserAndEntityTypeAndEntityIdIsNull(user, ChatEntityType.GLOBAL)) return;

        Chat chat = chatRepository.save(
            Chat.builder().user(user).entityType(ChatEntityType.GLOBAL).build()
        );

        String welcomeBlocks = buildWelcomeBlocks(user);
        messageRepository.save(
            ChatMessage.builder()
                .chat(chat)
                .sender(MessageSender.CONNIE)
                .blocks(welcomeBlocks)
                .variant("default")
                .build()
        );
    }

    private String buildWelcomeBlocks(User user) {
        String firstName = user.getDisplayName().split(" ")[0];
        var blocks = mb.blocks();
        blocks.add(
            mb.textBlock(
                mb.textSpan(
                    "Hey " + firstName + "! I'm Connie — Rail's conductor. 🎩"
                ),
                mb.textSpan(
                    " I'm here to help you turn what you want to work on into a schedule that actually works."
                ),
                mb.textSpan(
                    " Tell me something you want to work towards — we'll build from there. 🌱"
                )
            )
        );
        return mb.toJson(blocks);
    }
}
