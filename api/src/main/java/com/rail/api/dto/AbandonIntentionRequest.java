package com.rail.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AbandonIntentionRequest(@NotBlank String reason) {}
