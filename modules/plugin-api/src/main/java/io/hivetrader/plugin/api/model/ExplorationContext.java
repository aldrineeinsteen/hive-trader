package io.hivetrader.plugin.api.model;

import java.time.Instant;
import java.util.Map;

public record ExplorationContext(
        String correlationId,
        PortfolioSnapshot portfolio,
        Map<String, Object> marketState,
        Instant evaluationTime
) {
}
