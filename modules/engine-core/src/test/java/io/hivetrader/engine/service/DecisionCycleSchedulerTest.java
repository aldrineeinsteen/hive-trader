package io.hivetrader.engine.service;

import io.hivetrader.engine.config.TradingProperties;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DecisionCycleSchedulerTest {

    @Test
    void invokesDecisionCycleService() {
        DecisionCycleService cycleService = mock(DecisionCycleService.class);
        DecisionCycleScheduler scheduler = new DecisionCycleScheduler(cycleService, new TradingProperties());

        scheduler.schedule();

        verify(cycleService).runCycle();
    }
}
