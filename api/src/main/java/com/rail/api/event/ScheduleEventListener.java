package com.rail.api.event;

import com.rail.api.component.RateLimitService;
import com.rail.api.dto.DailyScheduleDto;
import com.rail.api.repository.UserSchedulingProfileRepository;
import com.rail.api.scheduler.DailyScheduler;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleEventListener {

    private final DailyScheduler dailyScheduler;
    private final UserSchedulingProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimitService rateLimitService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onIntentionConfirmed(IntentionConfirmedEvent event) {
        if (!rateLimitService.tryRecompute(event.user().getPid())) {
            log.warn("[rate-limit] recompute skipped for user={} — daily cap reached", event.user().getPid());
            return;
        }
        log.info("processing intention confirmation. Rescheduling...");
        LocalDate today = profileRepository.findByUser(event.user())
            .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
            .orElseGet(LocalDate::now);

        try {
            DailyScheduleDto updated = dailyScheduler.recompute(
                event.user(),
                today
            );
            eventPublisher.publishEvent(
                new SsePublishEvent(
                    event.user().getPid(),
                    "schedule_updated",
                    updated
                )
            );
            log.info(
                "Schedule built/recomputed and pushed for user {}",
                event.user().getPid()
            );
        } catch (Exception e) {
            log.error(
                "Schedule build/recompute failed for user {}: {}",
                event.user().getPid(),
                e.getMessage(),
                e
            );
        }
    }
}
