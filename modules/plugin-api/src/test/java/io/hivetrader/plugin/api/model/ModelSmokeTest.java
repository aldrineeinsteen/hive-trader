package io.hivetrader.plugin.api.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelSmokeTest {

    @Test
    void recordsRetainValues() {
        PositionSnapshot position = new PositionSnapshot(
                "AAPL",
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        PortfolioSnapshot portfolio = new PortfolioSnapshot(
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(0.01),
                List.of(position),
                Instant.parse("2026-01-01T00:00:01Z")
        );

        TradeProposal proposal = new TradeProposal(
                "corr-1",
                "strategy",
                "AAPL",
                TradeSide.BUY,
                BigDecimal.ONE,
                BigDecimal.valueOf(200),
                "coid-1",
                Instant.parse("2026-01-01T00:00:02Z")
        );

        RiskContext riskContext = new RiskContext(
                "corr-1",
                portfolio,
                List.of(proposal),
                Map.of("k", "v"),
                Instant.parse("2026-01-01T00:00:03Z")
        );

        PluginInfo info = new PluginInfo("id", "name", "1", Set.of("cap"));
        RiskEvaluation evaluation = RiskEvaluation.allow("risk");
        ExecutionRequest executionRequest = new ExecutionRequest("corr-1", List.of(proposal));
        ExecutionResult executionResult = new ExecutionResult(
                "corr-1",
                "coid-1",
                "broker-1",
                true,
                "accepted",
                Instant.parse("2026-01-01T00:00:04Z"),
                BigDecimal.valueOf(0.12),
                "USD"
        );
        CandidateRanking candidateRanking = new CandidateRanking(
                "explorer",
                "AAPL",
                BigDecimal.valueOf(0.8),
                "momentum"
        );
        SignalEvent signalEvent = new SignalEvent(
                "strategy",
                "AAPL",
                TradeSide.BUY,
                BigDecimal.valueOf(0.7),
                "reason",
                Instant.parse("2026-01-01T00:00:06Z")
        );

        assertEquals("AAPL", position.symbol());
        assertEquals(BigDecimal.valueOf(1000), portfolio.equity());
        assertEquals("coid-1", proposal.clientOrderId());
        assertEquals("corr-1", executionRequest.correlationId());
        assertEquals("USD", executionResult.externalChargeCurrency());
        assertEquals("AAPL", candidateRanking.symbol());
        assertEquals("strategy", signalEvent.pluginId());
        assertEquals("corr-1", riskContext.correlationId());
        assertEquals("id", info.id());
        assertTrue(evaluation.evidence().stream().anyMatch(e -> "risk".equals(e.pluginId())));
    }
}
