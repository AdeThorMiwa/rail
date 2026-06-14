package com.rail.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "chat_messages",
    indexes = @Index(name = "idx_chat_messages_chat_created", columnList = "chat_id, created_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends PublicEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id", nullable = false, updatable = false)
    private Chat chat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MessageSender sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String blocks;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    @ManyToOne
    @JoinColumn(name = "reply_to_id")
    private ChatMessage replyTo;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private String variant = "default";
}
