package io.hivetrader.plugin.api.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StrategyContext(
        String correlationId,
        PortfolioSnapshot portfolio,
        List<CandidateRanking> candidates,
        Map<String, Object> indicators,
        Instant evaluationTime
) {
}
