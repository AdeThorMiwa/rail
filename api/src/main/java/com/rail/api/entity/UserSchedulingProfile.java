package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_scheduling_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSchedulingProfile extends PublicEntity {

    @OneToOne(optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        unique = true,
        updatable = false
    )
    private User user;

    @Column(nullable = false)
    private LocalTime deepWorkStart;

    @Column(nullable = false)
    private LocalTime deepWorkEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnergyPattern energyPattern;

    @Column(nullable = false)
    private LocalTime wakeTime;

    @Column(nullable = false)
    private LocalTime sleepTime;

    @Column(nullable = false)
    private String timezone;
}
