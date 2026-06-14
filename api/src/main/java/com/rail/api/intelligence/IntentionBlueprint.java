package com.rail.api.intelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rail.api.entity.IntentionType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntentionBlueprint(
    IntentionType intentionType,
    String title,
    String completionCriteria
) {}
