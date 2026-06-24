package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tool_call_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolCallLog extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id", nullable = false, updatable = false)
    private Chat chat;

    @Column(nullable = false, updatable = false)
    private String callId;

    @Column(nullable = false, updatable = false)
    private String toolName;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String arguments;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String result;
}
