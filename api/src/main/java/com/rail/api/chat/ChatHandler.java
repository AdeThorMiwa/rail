package com.rail.api.chat;

import com.rail.api.entity.Chat;

public interface ChatHandler {
    ChatHandlerResult handle(Chat chat, String userInput);
}
