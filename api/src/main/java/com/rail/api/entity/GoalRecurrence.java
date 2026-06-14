package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "goal_recurrences")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoalRecurrence extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "goal_id", nullable = false, updatable = false)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private GoalRecurrenceFrequency frequency;

    @Column(nullable = false)
    private Integer timesPerPeriod;
}
