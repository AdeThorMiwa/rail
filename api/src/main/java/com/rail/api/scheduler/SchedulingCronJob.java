package com.rail.api.scheduler;

import com.rail.api.repository.UserSchedulingProfileRepository;
import com.rail.api.service.ScheduleEntryService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class SchedulingCronJob {

    private final UserSchedulingProfileRepository profileRepository;
    private final DailyScheduler dailyScheduler;
    private final ScheduleEntryService scheduleEntryService;
    private final ScheduleInitService scheduleInitService;

    @Scheduled(cron = "0 */20 * * * *")
    public void buildDailySchedules() {
        for (var profile : profileRepository.findAll()) {
            var user = profile.getUser();
            try {
                ZoneId zone = ZoneId.of(profile.getTimezone());
                LocalDate today = LocalDate.now(zone);
                LocalTime now = LocalTime.now(zone);
                LocalTime triggerTime = profile.getWakeTime().minusHours(1);
                if (now.isBefore(triggerTime)) {
                    continue;
                }
                if (!scheduleInitService.markGenerating(user, today)) {
                    continue;
                }
                dailyScheduler.recompute(user, today);
                log.info("Built schedule for user {}", user.getPid());
            } catch (Exception e) {
                log.error(
                    "Failed to build schedule for user {}: {}",
                    user.getPid(),
                    e.getMessage(),
                    e
                );
            }
        }
    }

    @Scheduled(cron = "0 * * * * *")
    public void autoMissOverdueEntries() {
        try {
            scheduleEntryService.autoMissOverdueEntries();
        } catch (Exception e) {
            log.error("Auto-miss cron failed: {}", e.getMessage(), e);
        }
    }
}
