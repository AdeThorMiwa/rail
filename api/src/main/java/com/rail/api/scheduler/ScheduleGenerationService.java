package com.rail.api.scheduler;

import com.rail.api.entity.User;
import com.rail.api.entity.UserSchedulingProfile;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleGenerationService {

    private final UserSchedulingProfileRepository profileRepository;
    private final ScheduleInitService scheduleInitService;
    private final DailyScheduler dailyScheduler;

    /**
     * Called on-demand (e.g. from /schedule/today). No time-window guard — generates immediately.
     */
    @Async
    public void generateTodayIfMissing(User user) {
        generateForUser(user, false, 0);
    }

    /**
     * Called from the cron job. Skips if the user's local time hasn't reached wakeTime - offsetHours.
     */
    public boolean generateIfWithinWindow(User user, int wakeTriggerOffsetHours) {
        return generateForUser(user, true, wakeTriggerOffsetHours);
    }

    private boolean generateForUser(User user, boolean enforceTimeWindow, int wakeTriggerOffsetHours) {
        Optional<UserSchedulingProfile> profileOpt = profileRepository.findByUser(user);
        ZoneId zone = profileOpt.map(p -> ZoneId.of(p.getTimezone())).orElse(ZoneId.of("UTC"));
        LocalDate today = LocalDate.now(zone);

        if (enforceTimeWindow && profileOpt.isPresent()) {
            LocalTime now = LocalTime.now(zone);
            LocalTime triggerTime = profileOpt.get().getWakeTime().minusHours(wakeTriggerOffsetHours);
            if (now.isBefore(triggerTime)) return false;
        }

        if (!scheduleInitService.markGenerating(user, today)) {
            log.info("[scheduler] schedule already exists or generating for user={}", user.getPid());
            return false;
        }
        try {
            log.info("[scheduler] generating schedule for user={} date={}", user.getPid(), today);
            dailyScheduler.recompute(user, today);
            return true;
        } catch (Exception e) {
            log.error("[scheduler] schedule generation failed for user={}: {}", user.getPid(), e.getMessage(), e);
            return false;
        }
    }
}
