package com.rail.api.scheduler;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleStatus;
import com.rail.api.entity.User;
import com.rail.api.repository.DailyScheduleRepository;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleInitService {

    private final DailyScheduleRepository scheduleRepository;

    /**
     * Creates a GENERATING placeholder for the given user+date in its own committed transaction.
     * Returns false if a record already exists (any status), so the caller can skip.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markGenerating(User user, LocalDate date) {
        if (
            scheduleRepository
                .findByUserAndScheduledDate(user, date)
                .isPresent()
        ) {
            return false;
        }

        scheduleRepository.saveAndFlush(
            DailySchedule.builder()
                .user(user)
                .scheduledDate(date)
                .generatedAt(Instant.now())
                .status(DailyScheduleStatus.GENERATING)
                .build()
        );
        return true;
    }
}
