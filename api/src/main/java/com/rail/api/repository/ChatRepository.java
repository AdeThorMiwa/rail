package com.rail.api.repository;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByPid(UUID pid);

    Optional<Chat> findByUserAndEntityTypeAndEntityIdIsNull(User user, ChatEntityType entityType);

    Optional<Chat> findByUserAndEntityTypeAndEntityId(User user, ChatEntityType entityType, UUID entityId);

    List<Chat> findByUserAndEntityTypeAndEntityIdIn(User user, ChatEntityType entityType, List<UUID> entityIds);

    boolean existsByUserAndEntityTypeAndEntityIdIsNull(User user, ChatEntityType entityType);
}
