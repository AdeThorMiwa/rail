package com.rail.api.event;

import com.rail.api.entity.User;
import java.util.UUID;

public record CycleRetroOpenEvent(User user, UUID cyclePid) {}
