package com.rail.api.event;

import com.rail.api.entity.ScheduleChange;
import com.rail.api.entity.User;

public record ScheduleChangeEvent(User user, ScheduleChange change) {}
