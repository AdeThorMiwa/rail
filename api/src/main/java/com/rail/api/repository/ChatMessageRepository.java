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
    List<ChatMessage> findByChatOrderByCreatedAtDesc(Chat chat, Pageable pageable);

    // Cursor-based pagination for single chat (uses internal Long id as keyset — no ties)
    List<ChatMessage> findByChatOrderByIdDesc(Chat chat, Pageable pageable);
    List<ChatMessage> findByChatAndIdLessThanOrderByIdDesc(Chat chat, Long beforeId, Pageable pageable);

    // Cursor-based pagination for multi-chat merge (goal activity uses createdAt as keyset)
    List<ChatMessage> findByChatAndCreatedAtBeforeOrderByCreatedAtDesc(Chat chat, java.time.Instant before, Pageable pageable);
    List<ChatMessage> findByChatInOrderByCreatedAtDesc(List<Chat> chats, Pageable pageable);
    List<ChatMessage> findByChatInAndCreatedAtBeforeOrderByCreatedAtDesc(List<Chat> chats, java.time.Instant before, Pageable pageable);

    Optional<ChatMessage> findByPidAndChat(UUID pid, Chat chat);
    Optional<ChatMessage> findByPid(UUID pid);
}
