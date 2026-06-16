package com.rail.api.scheduler;

record LlmResult(SchedulingPlan plan, boolean usedFallback) {}
