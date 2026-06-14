package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "goal_target")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoalTarget extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "goal_id", nullable = false, unique = true)
    private Goal goal;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal targetValue;

    @Column(nullable = false)
    private String unit;
}
