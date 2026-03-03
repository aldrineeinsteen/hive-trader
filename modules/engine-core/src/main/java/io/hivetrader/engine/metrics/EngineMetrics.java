package io.hivetrader.engine.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class EngineMetrics {

    private final Counter ordersSubmitted;
    private final Counter ordersFailed;
    private final AtomicReference<Double> portfolioEquity = new AtomicReference<>(0.0);
    private final AtomicReference<Double> portfolioDrawdownPct = new AtomicReference<>(0.0);
    private final AtomicBoolean tradingEnabled = new AtomicBoolean(false);
    private final AtomicBoolean killSwitch = new AtomicBoolean(false);

    public EngineMetrics(MeterRegistry meterRegistry) {
        this.ordersSubmitted = Counter.builder("orders_submitted_total").register(meterRegistry);
        this.ordersFailed = Counter.builder("orders_failed_total").register(meterRegistry);
        Gauge.builder("portfolio_equity", portfolioEquity, AtomicReference::get).register(meterRegistry);
        Gauge.builder("portfolio_drawdown_pct", portfolioDrawdownPct, AtomicReference::get).register(meterRegistry);
        Gauge.builder("trading_enabled", tradingEnabled, b -> b.get() ? 1.0 : 0.0).register(meterRegistry);
        Gauge.builder("kill_switch", killSwitch, b -> b.get() ? 1.0 : 0.0).register(meterRegistry);
    }

    public void incrementOrdersSubmitted() {
        ordersSubmitted.increment();
    }

    public void incrementOrdersFailed() {
        ordersFailed.increment();
    }

    public void updatePortfolio(double equity, double drawdownPct) {
        portfolioEquity.set(equity);
        portfolioDrawdownPct.set(drawdownPct);
    }

    public void updateTradingGates(boolean isTradingEnabled, boolean isKillSwitchOn) {
        tradingEnabled.set(isTradingEnabled);
        killSwitch.set(isKillSwitchOn);
    }

    public void incrementSignalGenerated(MeterRegistry meterRegistry, String pluginId) {
        Counter.builder("signals_generated_total")
                .tag("plugin", pluginId)
                .register(meterRegistry)
                .increment();
    }

    public void incrementSignalBlockedByRisk(MeterRegistry meterRegistry, String pluginId) {
        Counter.builder("signals_blocked_risk_total")
                .tag("plugin", pluginId)
                .register(meterRegistry)
                .increment();
    }

    public void incrementExternalCharge(MeterRegistry meterRegistry, String brokerId, String currency, double amount) {
        Counter.builder("broker_external_charges_total")
                .tag("broker", brokerId)
                .tag("currency", currency == null || currency.isBlank() ? "USD" : currency)
                .register(meterRegistry)
                .increment(amount);
    }
}
