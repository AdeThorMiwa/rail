package com.rail.api.repository;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository
    extends JpaRepository<ChatMessage, Long>
{
    List<ChatMessage> findByChatOrderByCreatedAtAsc(Chat chat);
    List<ChatMessage> findByChatIn(List<Chat> chats);
    List<ChatMessage> findByChatOrderByCreatedAtDesc(
        Chat chat,
        Pageable pageable
    );
    Optional<ChatMessage> findByPidAndChat(UUID pid, Chat chat);
    Optional<ChatMessage> findByPid(UUID pid);
}
