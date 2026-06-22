package com.rail.api.scheduler;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleStatus;
import com.rail.api.entity.User;
import com.rail.api.repository.DailyScheduleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleInitService {

    @Value("${rail.scheduling.stale-generating-minutes:10}")
    private int staleGeneratingMinutes;

    private final DailyScheduleRepository scheduleRepository;

    /**
     * Claims the generation slot for the given user+date in its own committed transaction.
     * Returns false if a non-stale record already exists, so the caller can skip.
     * A GENERATING record older than STALE_GENERATING_MINUTES is treated as abandoned and reset.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markGenerating(User user, LocalDate date) {
        return scheduleRepository
            .findByUserAndScheduledDate(user, date)
            .map(existing -> {
                if (existing.getStatus() == DailyScheduleStatus.GENERATING) {
                    Instant staleThreshold = Instant.now().minus(staleGeneratingMinutes, ChronoUnit.MINUTES);
                    if (existing.getGeneratedAt().isBefore(staleThreshold)) {
                        log.warn("[scheduler] resetting stale GENERATING schedule for user={} date={}", user.getPid(), date);
                        existing.setGeneratedAt(Instant.now());
                        scheduleRepository.saveAndFlush(existing);
                        return true;
                    }
                }
                return false;
            })
            .orElseGet(() -> {
                scheduleRepository.saveAndFlush(
                    DailySchedule.builder()
                        .user(user)
                        .scheduledDate(date)
                        .generatedAt(Instant.now())
                        .status(DailyScheduleStatus.GENERATING)
                        .build()
                );
                return true;
            });
    }
}
