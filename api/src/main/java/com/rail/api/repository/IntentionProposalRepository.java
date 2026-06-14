package com.rail.api.repository;

import com.rail.api.entity.Chat;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.IntentionProposalStatus;
import com.rail.api.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentionProposalRepository
    extends JpaRepository<IntentionProposal, Long>
{
    Optional<IntentionProposal> findByChatAndStatus(
        Chat chat,
        IntentionProposalStatus status
    );
    Optional<IntentionProposal> findByPidAndOwner(UUID pid, User owner);
}
