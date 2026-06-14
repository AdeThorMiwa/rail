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
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tasks",
    indexes = @Index(name = "idx_tasks_goal_status", columnList = "goal_id, status")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task extends PublicEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(optional = true)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @ManyToOne(optional = true)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(optional = true)
    @JoinColumn(name = "rescheduled_from_id")
    private Task rescheduledFrom;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private TaskCompletionType completionType;

    @Column(columnDefinition = "TEXT")
    private String completionNote;

    private Integer durationMinutes;

    private LocalTime fixedTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskFlexibility flexibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskFlexibilitySetBy flexibilitySetBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    private LocalDate deadline;

    @Column(columnDefinition = "TEXT")
    private String missReason;

    private Instant startedAt;
    private Instant endedAt;
    private Instant completedAt;
}
