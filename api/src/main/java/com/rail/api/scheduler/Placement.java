package com.rail.api.scheduler;

import com.rail.api.entity.Task;
import java.time.LocalTime;

record Placement(LocalTime start, LocalTime end, Task task, String schedulingNote) {}
