package com.rail.api.event;

import com.rail.api.entity.User;
import java.util.UUID;

public record CyclePlanningOpenEvent(User user, UUID cyclePid) {}
