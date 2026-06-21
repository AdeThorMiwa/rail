package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "intentions",
    indexes = @Index(name = "idx_intentions_owner_status", columnList = "owner_id, status")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Intention extends PublicEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @Column(nullable = false)
    private String rawInput;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String completionCriteria;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntentionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IntentionStatus status = IntentionStatus.ACTIVE;

    @Column
    private Instant completedAt;
}
