package com.rail.api.event;

import com.rail.api.repository.UserSchedulingProfileRepository;
import com.rail.api.scheduler.DailyScheduler;
import com.rail.api.service.NextGoalGenerationService;
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
public class GoalEventListener {

    private final NextGoalGenerationService nextGoalGenerationService;
    private final DailyScheduler dailyScheduler;
    private final UserSchedulingProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onGoalCompleted(GoalCompletedEvent event) {
        try {
            nextGoalGenerationService.generateNextGoal(event.user(), event.goalPid());
        } catch (Exception e) {
            log.error("[goal-listener] next goal generation failed for goal {}: {}",
                event.goalPid(), e.getMessage(), e);
        }

        try {
            LocalDate today = profileRepository.findByUser(event.user())
                .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
                .orElseGet(LocalDate::now);
            var updated = dailyScheduler.recompute(event.user(), today);
            eventPublisher.publishEvent(new SsePublishEvent(
                event.user().getPid(),
                "schedule_updated",
                updated
            ));
        } catch (Exception e) {
            log.error("[goal-listener] schedule recompute failed for user {}: {}",
                event.user().getPid(), e.getMessage(), e);
        }
    }
}
