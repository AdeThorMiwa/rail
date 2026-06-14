package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "goal_recurrence_days",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uniq_key_goal_recurrence_day",
            columnNames = { "goal_recurrence_id", "day_of_week" }
        ),
    }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoalRecurrenceDay extends Identifiable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "goal_recurrence_id", nullable = false)
    private GoalRecurrence goalRecurrence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column
    private LocalTime preferredTime;
}
