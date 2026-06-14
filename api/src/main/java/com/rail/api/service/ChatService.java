package com.rail.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rail.api.chat.ChatHandler;
import com.rail.api.chat.ChatHandlerResult;
import com.rail.api.component.DtoMapper;
import com.rail.api.dto.ChatMessageDto;
import com.rail.api.entity.Chat;
import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.Goal;
import com.rail.api.entity.MessageSender;
import com.rail.api.entity.User;
import com.rail.api.event.NotificationEvent;
import com.rail.api.event.SsePublishEvent;
import com.rail.api.repository.ChatMessageRepository;
import com.rail.api.repository.ChatRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.TaskRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final ChatHandler chatHandler;
    private final ObjectMapper objectMapper;
    private final DtoMapper dtoMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatCreationHelper chatCreationHelper;

    public Chat getOrCreateChat(User user, ChatEntityType entityType, UUID entityId) {
        Optional<Chat> existing = entityId == null
            ? chatRepository.findByUserAndEntityTypeAndEntityIdIsNull(user, entityType)
            : chatRepository.findByUserAndEntityTypeAndEntityId(user, entityType, entityId);
        if (existing.isPresent()) return existing.get();

        try {
            return chatCreationHelper.tryCreate(user, entityType, entityId);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Concurrent insert race — another transaction won; return the now-existing record
            return (entityId == null
                ? chatRepository.findByUserAndEntityTypeAndEntityIdIsNull(user, entityType)
                : chatRepository.findByUserAndEntityTypeAndEntityId(user, entityType, entityId)
            ).orElseThrow(() -> e);
        }
    }

    public Chat getChat(User user) {
        return getOrCreateChat(user, ChatEntityType.GLOBAL, null);
    }

    public List<ChatMessageDto> getMessages(User user) {
        return getMessages(user, ChatEntityType.GLOBAL, null);
    }

    public List<ChatMessageDto> getMessages(User user, ChatEntityType entityType, UUID entityId) {
        Chat chat = getOrCreateChat(user, entityType, entityId);
        return messageRepository
            .findByChatOrderByCreatedAtAsc(chat)
            .stream()
            .map(dtoMapper::toChatMessageDto)
            .toList();
    }

    public List<ChatMessageDto> getGoalActivity(User user, UUID goalPid) {
        Goal goal = goalRepository.findByPidAndOwner(goalPid, user)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Goal not found"));

        List<ChatMessage> all = new ArrayList<>();

        chatRepository.findByUserAndEntityTypeAndEntityId(user, ChatEntityType.GOAL, goalPid)
            .ifPresent(goalChat -> all.addAll(messageRepository.findByChatOrderByCreatedAtAsc(goalChat)));

        List<UUID> taskPids = taskRepository.findByGoal(goal).stream()
            .map(task -> task.getPid())
            .toList();
        if (!taskPids.isEmpty()) {
            List<Chat> taskChats = chatRepository.findByUserAndEntityTypeAndEntityIdIn(
                user, ChatEntityType.TASK, taskPids);
            all.addAll(messageRepository.findByChatIn(taskChats));
        }

        all.sort(Comparator.comparing(ChatMessage::getCreatedAt));
        return all.stream().map(dtoMapper::toChatMessageDto).toList();
    }

    @Transactional
    public void send(User user, String rawInput, UUID replyToId) {
        send(user, ChatEntityType.GLOBAL, null, rawInput, replyToId);
    }

    @Transactional
    public void send(User user, ChatEntityType entityType, UUID entityId, String rawInput, UUID replyToId) {
        Chat chat = getOrCreateChat(user, entityType, entityId);

        ChatMessage replyTo = null;
        if (replyToId != null) {
            replyTo = messageRepository
                .findByPidAndChat(replyToId, chat)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Message not found"
                    )
                );
        }

        ChatHandlerResult result = chatHandler.handle(chat, rawInput);

        ChatMessage userMessage = messageRepository.saveAndFlush(
            ChatMessage.builder()
                .chat(chat)
                .sender(MessageSender.USER)
                .blocks(result.userBlocks())
                .rawText(rawInput)
                .replyTo(replyTo)
                .variant("default")
                .build()
        );

        ChatMessage connieMessage = messageRepository.saveAndFlush(
            ChatMessage.builder()
                .chat(chat)
                .sender(MessageSender.CONNIE)
                .blocks(result.connieBlocks())
                .rawText(result.connieRawText())
                .variant(result.connieVariant())
                .build()
        );

        UUID userPid = user.getPid();
        eventPublisher.publishEvent(
            new SsePublishEvent(
                userPid,
                "message_added",
                dtoMapper.toChatMessageDto(userMessage)
            )
        );
        eventPublisher.publishEvent(
            new SsePublishEvent(
                userPid,
                "message_added",
                dtoMapper.toChatMessageDto(connieMessage)
            )
        );
    }

    @Transactional
    public void sendOpeningLlmMessage(User user, ChatEntityType entityType, UUID entityId, String trigger) {
        Chat chat = getOrCreateChat(user, entityType, entityId);
        ChatHandlerResult result = chatHandler.handle(chat, trigger);
        ChatMessage msg = messageRepository.saveAndFlush(
            ChatMessage.builder()
                .chat(chat)
                .sender(MessageSender.CONNIE)
                .blocks(result.connieBlocks())
                .rawText(result.connieRawText())
                .variant(result.connieVariant())
                .build()
        );
        eventPublisher.publishEvent(
            new SsePublishEvent(user.getPid(), "message_added", dtoMapper.toChatMessageDto(msg))
        );
        eventPublisher.publishEvent(
            new NotificationEvent(user.getPid(), entityType, entityId, result.connieRawText())
        );
    }

    @Transactional
    public void recordEvent(User user, String blocksJson, String rawText) {
        recordEvent(user, ChatEntityType.GLOBAL, null, blocksJson, rawText);
    }

    @Transactional
    public void recordEvent(User user, ChatEntityType entityType, UUID entityId, String blocksJson, String rawText) {
        Chat chat = getOrCreateChat(user, entityType, entityId);
        ChatMessage msg = messageRepository.saveAndFlush(
            ChatMessage.builder()
                .chat(chat)
                .sender(MessageSender.CONNIE)
                .blocks(blocksJson)
                .rawText(rawText)
                .variant("default")
                .build()
        );
        eventPublisher.publishEvent(
            new SsePublishEvent(
                user.getPid(),
                "message_added",
                dtoMapper.toChatMessageDto(msg)
            )
        );
        eventPublisher.publishEvent(
            new NotificationEvent(user.getPid(), entityType, entityId, rawText)
        );
    }

    @Transactional
    public void resolveActionBlock(
        User user,
        UUID messagePid,
        String tappedItemId,
        List<Map<String, Object>> resolvedItems
    ) {
        ChatMessage message = messageRepository
            .findByPid(messagePid)
            .filter(m -> user.getId().equals(m.getChat().getUser().getId()))
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Message not found"
                )
            );
        try {
            ArrayNode blocks = (ArrayNode) objectMapper.readTree(
                message.getBlocks()
            );
            for (int i = 0; i < blocks.size(); i++) {
                var block = blocks.get(i);
                if (!"actions".equals(block.path("type").asText())) continue;
                for (var item : block.path("items")) {
                    if (tappedItemId.equals(item.path("id").asText())) {
                        ObjectNode terminal = objectMapper.createObjectNode();
                        terminal.put("type", "actions");
                        terminal.set(
                            "items",
                            objectMapper.valueToTree(resolvedItems)
                        );
                        blocks.set(i, terminal);
                        message.setBlocks(blocks.toString());
                        messageRepository.saveAndFlush(message);
                        eventPublisher.publishEvent(
                            new SsePublishEvent(
                                user.getPid(),
                                "message_updated",
                                dtoMapper.toChatMessageDto(message)
                            )
                        );
                        return;
                    }
                }
            }
            log.warn(
                "[resolveActionBlock] no match for tappedItemId={} in message {}",
                tappedItemId,
                messagePid
            );
        } catch (Exception e) {
            log.warn(
                "[resolveActionBlock] failed on message {}: {}",
                messagePid,
                e.getMessage(),
                e
            );
        }
    }
}
