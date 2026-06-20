package com.rail.api.repository;

import com.rail.api.entity.User;
import com.rail.api.entity.UserConnieLog;
import com.rail.api.entity.UserConnieLogType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserConnieLogRepository extends JpaRepository<UserConnieLog, Long> {

    List<UserConnieLog> findByUserOrderByCreatedAtAsc(User user);

    /** Returns the N most recent logs, newest-first. Callers should reverse for oldest-first display. */
    @Query("""
        SELECT l FROM UserConnieLog l
        WHERE l.user = :user
        ORDER BY l.createdAt DESC
        LIMIT :n
    """)
    List<UserConnieLog> findRecentN(@Param("user") User user, @Param("n") int n);

    long countByUser(User user);

    /** Returns the most recent ANALYSIS entry for a user — used for appending stated preferences. */
    Optional<UserConnieLog> findTopByUserAndTypeOrderByCreatedAtDesc(User user, UserConnieLogType type);

    /** Returns the oldest N entries for pruning — ordered oldest first. */
    @Query("""
        SELECT l FROM UserConnieLog l
        WHERE l.user = :user
        ORDER BY l.createdAt ASC
        LIMIT :n
    """)
    List<UserConnieLog> findOldestN(@Param("user") User user, @Param("n") int n);
}
