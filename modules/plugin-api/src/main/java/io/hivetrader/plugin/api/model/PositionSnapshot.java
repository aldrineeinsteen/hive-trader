package io.hivetrader.plugin.api.model;

import java.math.BigDecimal;
import java.time.Instant;

public record PositionSnapshot(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        Instant updatedAt
) {
}
