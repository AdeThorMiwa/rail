package com.rail.api.scheduler;

import com.rail.api.connie.ConnieProfileService;
import com.rail.api.entity.UserConnieLogType;
import com.rail.api.repository.UserConnieLogRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import com.rail.api.service.ScheduleEntryService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class SchedulingCronJob {

    @Value("${rail.connie.pattern-analysis.analysis-hour:1}")
    private int patternAnalysisHour;

    private final UserSchedulingProfileRepository profileRepository;
    private final ScheduleEntryService scheduleEntryService;
    private final ConnieProfileService connieProfileService;
    private final UserConnieLogRepository connieLogRepository;

    @Scheduled(cron = "${rail.scheduling.cron.auto-miss:0 0 * * * *}")
    public void autoMissOverdueEntries() {
        try {
            scheduleEntryService.autoMissOverdueEntries();
        } catch (Exception e) {
            log.error("Auto-miss cron failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${rail.connie.pattern-analysis.cron:0 */30 * * * *}")
    public void runConniePatternAnalysis() {
        for (var profile : profileRepository.findAll()) {
            var user = profile.getUser();
            try {
                ZoneId zone = ZoneId.of(profile.getTimezone());
                LocalTime nowInZone = LocalTime.now(zone);
                LocalDate todayInZone = LocalDate.now(zone);

                // Only run within the configured analysis hour window in the user's timezone
                LocalTime windowStart = LocalTime.of(patternAnalysisHour, 0);
                LocalTime windowEnd = windowStart.plusHours(1);
                if (nowInZone.isBefore(windowStart) || nowInZone.isAfter(windowEnd)) {
                    continue;
                }

                // Skip if analysis already ran today for this user
                boolean ranToday = connieLogRepository
                    .findTopByUserAndTypeOrderByCreatedAtDesc(user, UserConnieLogType.ANALYSIS)
                    .map(log -> log.getCreatedAt().atZone(zone).toLocalDate().equals(todayInZone))
                    .orElse(false);
                if (ranToday) continue;

                connieProfileService.runDailyAnalysis(user);
                log.info("Connie pattern analysis complete for user {}", user.getPid());
            } catch (Exception e) {
                log.error(
                    "Connie pattern analysis failed for user {}: {}",
                    user.getPid(),
                    e.getMessage(),
                    e
                );
            }
        }
    }
}
