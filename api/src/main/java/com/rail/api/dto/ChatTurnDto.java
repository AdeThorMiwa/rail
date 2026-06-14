package com.rail.api.dto;

public record ChatTurnDto(
    ChatMessageDto userMessage,
    ChatMessageDto connieMessage
) {}
