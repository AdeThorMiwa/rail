package com.rail.api.repository;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ToolCallLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCallLogRepository extends JpaRepository<ToolCallLog, Long> {
    List<ToolCallLog> findByChatAndCreatedAtAfterOrderByCreatedAtAsc(Chat chat, Instant after);
    List<ToolCallLog> findByChatAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(Chat chat, Instant from);
}
