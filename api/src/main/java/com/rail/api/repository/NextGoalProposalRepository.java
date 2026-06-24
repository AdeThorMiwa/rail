package com.rail.api.repository;

import com.rail.api.entity.Chat;
import com.rail.api.entity.NextGoalProposal;
import com.rail.api.entity.NextGoalProposalStatus;
import com.rail.api.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NextGoalProposalRepository extends JpaRepository<NextGoalProposal, Long> {

    Optional<NextGoalProposal> findByChatAndStatus(Chat chat, NextGoalProposalStatus status);

    Optional<NextGoalProposal> findByPidAndOwner(UUID pid, User owner);
}
