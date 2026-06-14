package com.rail.api.repository;

import com.rail.api.entity.CycleStatus;
import com.rail.api.entity.User;
import com.rail.api.entity.UserCycle;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCycleRepository extends JpaRepository<UserCycle, Long> {

    List<UserCycle> findByOwnerAndStatusIn(User owner, Collection<CycleStatus> statuses);

    List<UserCycle> findByStatusAndStartDate(CycleStatus status, LocalDate startDate);

    Optional<UserCycle> findByPidAndOwner(UUID pid, User owner);

    Optional<UserCycle> findTopByOwnerAndStatusInOrderByEndDateDesc(
        User owner, Collection<CycleStatus> statuses
    );

    @Query("""
        SELECT c FROM UserCycle c
        WHERE c.status = 'ACTIVE'
        AND c.endDate <= :today
        AND c.reviewTime <= :now
    """)
    List<UserCycle> findCyclesDueForReview(
        @Param("today") LocalDate today,
        @Param("now") LocalTime now
    );
}
