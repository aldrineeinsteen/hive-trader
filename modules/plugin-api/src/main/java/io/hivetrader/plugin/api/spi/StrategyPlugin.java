package io.hivetrader.plugin.api.spi;

import io.hivetrader.plugin.api.model.SignalEvent;
import io.hivetrader.plugin.api.model.StrategyContext;

import java.util.List;

public interface StrategyPlugin extends TradingPlugin {
    List<SignalEvent> generateSignals(StrategyContext context);
}
