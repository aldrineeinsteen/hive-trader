package io.hivetrader.engine.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EngineMetricsTest {

    @Test
    void updatesCountersAndGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EngineMetrics metrics = new EngineMetrics(registry);

        metrics.incrementOrdersSubmitted();
        metrics.incrementOrdersFailed();
        metrics.updatePortfolio(12345.0, 0.03);
        metrics.updateTradingGates(true, false);
        metrics.incrementSignalGenerated(registry, "strategy-test");
        metrics.incrementSignalBlockedByRisk(registry, "strategy-test");

        assertEquals(1.0, registry.get("orders_submitted_total").counter().count());
        assertEquals(1.0, registry.get("orders_failed_total").counter().count());
        assertEquals(12345.0, registry.get("portfolio_equity").gauge().value());
        assertEquals(0.03, registry.get("portfolio_drawdown_pct").gauge().value());
        assertEquals(1.0, registry.get("trading_enabled").gauge().value());
        assertEquals(0.0, registry.get("kill_switch").gauge().value());
        assertEquals(1.0, registry.get("signals_generated_total").tag("plugin", "strategy-test").counter().count());
        assertEquals(1.0, registry.get("signals_blocked_risk_total").tag("plugin", "strategy-test").counter().count());
    }
}
