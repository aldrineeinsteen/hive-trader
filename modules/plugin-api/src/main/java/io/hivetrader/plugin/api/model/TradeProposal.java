package io.hivetrader.plugin.api.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeProposal(
        String correlationId,
        String pluginId,
        String symbol,
        TradeSide side,
        BigDecimal quantity,
        BigDecimal limitPrice,
        String clientOrderId,
        Instant createdAt
) {
}
