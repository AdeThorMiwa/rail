package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "goals")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Goal extends PublicEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "intention_id", nullable = false, updatable = false)
    private Intention intention;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private GoalType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GoalStatus status = GoalStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "blocked_by")
    private Goal blockedBy;

    @Column(columnDefinition = "TEXT")
    private String blockReason;

    @Column
    private Instant blockedSince;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EnergyLevel energyLevel;

    @Column(nullable = false, updatable = false)
    private Long estimatedTotalHours;

    @Column(nullable = false)
    @Builder.Default
    private Long actualTotalHours = 0L;

    @Column
    private LocalDate targetDate;

    @Column(columnDefinition = "TEXT")
    private String completionNotes;

    @Column
    private LocalDate earliestStartDate;

    @Column
    private Instant completedAt;
}
