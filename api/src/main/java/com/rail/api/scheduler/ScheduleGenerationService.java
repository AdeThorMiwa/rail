package com.rail.api.scheduler;

import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleStatus;
import com.rail.api.entity.User;
import com.rail.api.repository.DailyScheduleRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleGenerationService {

    private final UserSchedulingProfileRepository profileRepository;
    private final DailyScheduleRepository scheduleRepository;
    private final ScheduleGenerationLock lock;
    private final DailyScheduler dailyScheduler;

    public LocalDate todayFor(User user) {
        return profileRepository
            .findByUser(user)
            .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
            .orElse(LocalDate.now());
    }

    public DailySchedule createNew(User user, LocalDate date) {
        return scheduleRepository.saveAndFlush(
            DailySchedule.builder()
                .user(user)
                .scheduledDate(date)
                .generatedAt(Instant.now())
                .status(DailyScheduleStatus.GENERATING)
                .build()
        );
    }

    public DailySchedule resetToGenerating(DailySchedule schedule) {
        schedule.setStatus(DailyScheduleStatus.GENERATING);
        schedule.setGeneratedAt(Instant.now());
        return scheduleRepository.saveAndFlush(schedule);
    }

    public void markFailed(DailySchedule schedule) {
        schedule.setStatus(DailyScheduleStatus.FAILED);
        scheduleRepository.save(schedule);
    }

    @Async
    public void generateAsync(User user, LocalDate date) {
        if (!lock.tryAcquire(user.getPid(), date)) {
            log.info(
                "[scheduler] generation already in progress user={} date={}",
                user.getPid(),
                date
            );
            return;
        }
        try {
            log.info(
                "[scheduler] generation started user={} date={}",
                user.getPid(),
                date
            );
            dailyScheduler.recompute(user, date);
        } catch (Exception e) {
            log.error(
                "[scheduler] generation failed user={} date={}: {}",
                user.getPid(),
                date,
                e.getMessage(),
                e
            );
            scheduleRepository
                .findByUserAndScheduledDate(user, date)
                .ifPresent(s -> {
                    markFailed(s);
                });
        } finally {
            lock.release(user.getPid(), date);
        }
    }
}
