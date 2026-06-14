package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "cycle_focuses",
    uniqueConstraints = {
        @UniqueConstraint(name = "uniq_cycle_goal", columnNames = {"cycle_id", "goal_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleFocus extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "cycle_id", nullable = false, updatable = false)
    private UserCycle cycle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "goal_id", nullable = false, updatable = false)
    private Goal goal;

    @Column(nullable = false)
    @Builder.Default
    private int position = 0;
}
