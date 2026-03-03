package io.hivetrader.plugin.api.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RiskContext(
        String correlationId,
        PortfolioSnapshot portfolio,
        List<TradeProposal> proposals,
        Map<String, Object> attributes,
        Instant evaluationTime
) {
}
