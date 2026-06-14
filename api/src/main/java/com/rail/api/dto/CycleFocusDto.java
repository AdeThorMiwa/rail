package com.rail.api.dto;

import com.rail.api.entity.GoalType;
import java.util.UUID;

public record CycleFocusDto(UUID goalPid, String title, GoalType type, int position) {}
