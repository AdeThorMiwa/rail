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
@Table(name = "task_targets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTarget extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(
        name = "task_id",
        nullable = false,
        unique = true,
        updatable = false
    )
    private Task task;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal estimatedValue;

    @Column(precision = 19, scale = 4)
    private BigDecimal actualValue;
}
