package com.rail.api.context;

import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.User;
import com.rail.api.entity.UserConnieLog;
import com.rail.api.entity.UserSchedulingProfile;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public record ConversationContext(
    User user,
    Chat chat,
    List<ChatMessage> recentHistory,
    Optional<IntentionProposal> activeProposal,
    String currentInput,
    ZonedDateTime now,
    Optional<UserSchedulingProfile> schedulingProfile,
    List<UserConnieLog> connieLogs
) {}
