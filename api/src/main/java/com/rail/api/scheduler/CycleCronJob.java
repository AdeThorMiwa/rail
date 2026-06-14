package com.rail.api.scheduler;

import com.rail.api.service.CycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class CycleCronJob {

    private final CycleService cycleService;

    @Scheduled(cron = "${rail.cycle.activation-cron:0 1 0 * * *}")
    public void activatePlannedCycles() {
        log.info("Cycle cron: activating planned cycles for today");
        cycleService.activatePlannedCycles();
    }

    @Scheduled(cron = "${rail.cycle.review-cron:0 * * * * *}")
    public void transitionCyclesToReview() {
        cycleService.transitionCyclesToReview();
    }
}
