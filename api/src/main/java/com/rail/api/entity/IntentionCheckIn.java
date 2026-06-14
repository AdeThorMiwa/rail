package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "intention_check_ins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentionCheckIn extends Identifiable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "intention_id", nullable = false, updatable = false)
    private Intention intention;

    @Column(nullable = false, updatable = false)
    private LocalDate date;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String progressNote;

    @Column(nullable = false)
    private Integer rating;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
