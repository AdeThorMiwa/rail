package com.rail.api.dto;

import java.math.BigDecimal;

public record GoalTargetDto(
    BigDecimal targetValue,
    BigDecimal currentValue,
    String unit
) {}
