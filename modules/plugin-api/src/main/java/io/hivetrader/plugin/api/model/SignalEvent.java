package io.hivetrader.plugin.api.model;

import java.math.BigDecimal;
import java.time.Instant;

public record SignalEvent(
        String pluginId,
        String symbol,
        TradeSide side,
        BigDecimal confidence,
        String reason,
        Instant eventTime
) {
}
