package com.rail.api.intelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntentionSynthesis(
    IntentionBlueprint intention,
    GoalBlueprint goal
) {}
