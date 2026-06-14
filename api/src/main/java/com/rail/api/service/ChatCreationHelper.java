package com.rail.api.service;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.User;
import com.rail.api.repository.ChatRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatCreationHelper {

    private final ChatRepository chatRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Chat tryCreate(User user, ChatEntityType entityType, UUID entityId) {
        return chatRepository.saveAndFlush(
            Chat.builder().user(user).entityType(entityType).entityId(entityId).build()
        );
    }
}
