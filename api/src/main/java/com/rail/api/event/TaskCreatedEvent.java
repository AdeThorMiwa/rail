package com.rail.api.event;

import com.rail.api.entity.Task;
import com.rail.api.entity.User;
import java.util.List;

public record TaskCreatedEvent(User user, List<Task> tasks) {}
