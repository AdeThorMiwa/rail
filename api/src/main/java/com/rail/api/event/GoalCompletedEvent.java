package com.rail.api.event;

import com.rail.api.entity.User;
import java.util.UUID;

public record GoalCompletedEvent(User user, UUID goalPid) {}
