package io.hivetrader.engine.service;

import io.hivetrader.engine.config.TradingProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DecisionCycleScheduler {

    private final DecisionCycleService decisionCycleService;

    public DecisionCycleScheduler(DecisionCycleService decisionCycleService, TradingProperties properties) {
        this.decisionCycleService = decisionCycleService;
    }

    @Scheduled(
            fixedDelayString = "${hive.scheduling.fixed-delay-ms}",
            initialDelayString = "${hive.scheduling.initial-delay-ms}"
    )
    public void schedule() {
        decisionCycleService.runCycle();
    }
}
