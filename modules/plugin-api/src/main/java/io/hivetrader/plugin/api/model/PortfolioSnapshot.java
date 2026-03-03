package io.hivetrader.plugin.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PortfolioSnapshot(
        BigDecimal equity,
        BigDecimal cash,
        BigDecimal drawdownPct,
        List<PositionSnapshot> positions,
        Instant capturedAt
) {
}
