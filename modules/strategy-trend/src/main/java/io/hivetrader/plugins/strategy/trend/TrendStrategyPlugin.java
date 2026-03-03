package io.hivetrader.plugins.strategy.trend;

import io.hivetrader.plugin.api.model.PluginInfo;
import io.hivetrader.plugin.api.model.SignalEvent;
import io.hivetrader.plugin.api.model.StrategyContext;
import io.hivetrader.plugin.api.model.TradeSide;
import io.hivetrader.plugin.api.spi.StrategyPlugin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrendStrategyPlugin implements StrategyPlugin {

    @Override
    public PluginInfo info() {
        return new PluginInfo("strategy-trend", "Trend Strategy", "0.1.0", Set.of("strategy", "moving-average"));
    }

    @Override
    public List<SignalEvent> generateSignals(StrategyContext context) {
        List<SignalEvent> signals = new ArrayList<>();
        for (var candidate : context.candidates()) {
            List<BigDecimal> prices = priceSeries(context.indicators(), candidate.symbol());
            if (prices.size() < 5) {
                continue;
            }

            BigDecimal shortMa = average(prices.subList(prices.size() - 3, prices.size()));
            BigDecimal longMa = average(prices.subList(prices.size() - 5, prices.size()));
            if (shortMa.compareTo(longMa) > 0) {
                signals.add(new SignalEvent(
                        info().id(),
                        candidate.symbol(),
                        TradeSide.BUY,
                        candidate.score().min(BigDecimal.ONE),
                        "short_ma_cross_above_long_ma",
                        Instant.now()
                ));
            } else if (shortMa.compareTo(longMa) < 0) {
                signals.add(new SignalEvent(
                        info().id(),
                        candidate.symbol(),
                        TradeSide.SELL,
                        candidate.score().min(BigDecimal.ONE),
                        "short_ma_cross_below_long_ma",
                        Instant.now()
                ));
            }
        }
        return signals;
    }

    @SuppressWarnings("unchecked")
    private List<BigDecimal> priceSeries(Map<String, Object> indicators, String symbol) {
        Object data = indicators.get(symbol + ":prices");
        if (data instanceof List<?> list) {
            return list.stream()
                    .filter(BigDecimal.class::isInstance)
                    .map(BigDecimal.class::cast)
                    .toList();
        }
        return List.of();
    }

    private BigDecimal average(List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), java.math.RoundingMode.HALF_UP);
    }
}
