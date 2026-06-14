package com.rail.api.repository;

import com.rail.api.entity.Intention;
import com.rail.api.entity.IntentionStatus;
import com.rail.api.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentionRepository extends JpaRepository<Intention, Long> {
    List<Intention> findByOwnerAndStatusInOrderByCreatedAtDesc(
        User owner,
        Collection<IntentionStatus> statuses
    );
    Optional<Intention> findByPidAndOwner(UUID pid, User owner);

    Optional<Intention> findByPid(UUID pid);
}
