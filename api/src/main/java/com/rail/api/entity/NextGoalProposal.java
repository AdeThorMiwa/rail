package com.rail.api.entity;

import com.rail.api.intelligence.GoalBlueprint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "next_goal_proposals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextGoalProposal extends PublicEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "intention_id", nullable = false, updatable = false)
    private Intention intention;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id", nullable = false, updatable = false)
    private Chat chat;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private GoalBlueprint goalBlueprint;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NextGoalProposalStatus status = NextGoalProposalStatus.REFINING;
}
