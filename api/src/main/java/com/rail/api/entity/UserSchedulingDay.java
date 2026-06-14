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
    name = "user_scheduling_days",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uniq_user_scheduling_day",
            columnNames = { "user_scheduling_profile_id", "day_of_week" }
        ),
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSchedulingDay extends PublicEntity {

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "user_scheduling_profile_id",
        nullable = false,
        updatable = false
    )
    private UserSchedulingProfile userSchedulingProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column
    private LocalTime preferredWorkStart;

    @Column
    private LocalTime preferredWorkEnd;
}
