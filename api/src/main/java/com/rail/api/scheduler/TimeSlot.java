package com.rail.api.scheduler;

import java.time.LocalTime;

record TimeSlot(LocalTime start, LocalTime end) {}
