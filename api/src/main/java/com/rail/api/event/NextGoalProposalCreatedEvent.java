package com.rail.api.event;

import com.rail.api.entity.User;
import java.util.UUID;

public record NextGoalProposalCreatedEvent(User user, UUID proposalPid) {}
