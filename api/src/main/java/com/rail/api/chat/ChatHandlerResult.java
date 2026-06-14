package com.rail.api.chat;

public record ChatHandlerResult(
    String userBlocks,
    String connieBlocks,
    String connieVariant,
    String connieRawText
) {}
