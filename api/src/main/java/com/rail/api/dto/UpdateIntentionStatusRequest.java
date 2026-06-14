package com.rail.api.dto;

import com.rail.api.entity.IntentionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateIntentionStatusRequest(@NotNull IntentionStatus status) {}
