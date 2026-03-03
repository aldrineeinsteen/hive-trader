package io.hivetrader.plugins.risk.basic;

import io.hivetrader.plugin.api.model.PortfolioSnapshot;
import io.hivetrader.plugin.api.model.RiskContext;
import io.hivetrader.plugin.api.model.RiskDecision;
import io.hivetrader.plugin.api.model.TradeProposal;
import io.hivetrader.plugin.api.model.TradeSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BasicRiskEvaluatorPluginTest {

    @Test
    void blocksWhenDrawdownTooHigh() {
        BasicRiskEvaluatorPlugin plugin = new BasicRiskEvaluatorPlugin();
        RiskContext context = new RiskContext(
                "corr",
                new PortfolioSnapshot(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(0.20), List.of(), Instant.now()),
                List.of(),
                Map.of(),
                Instant.now()
        );

        assertEquals(RiskDecision.BLOCK, plugin.evaluate(context).decision());
    }

    @Test
    void allowsWithLimitsWhenTooManyProposals() {
        BasicRiskEvaluatorPlugin plugin = new BasicRiskEvaluatorPlugin();
        List<TradeProposal> proposals = List.of(
                proposal("1"), proposal("2"), proposal("3"), proposal("4"), proposal("5"), proposal("6")
        );

        RiskContext context = new RiskContext(
                "corr",
                new PortfolioSnapshot(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(0.01), List.of(), Instant.now()),
                proposals,
                Map.of(),
                Instant.now()
        );

        assertEquals(RiskDecision.ALLOW_WITH_LIMITS, plugin.evaluate(context).decision());
    }

    private TradeProposal proposal(String id) {
        return new TradeProposal("corr", "strategy", "AAPL", TradeSide.BUY, BigDecimal.ONE, null, id, Instant.now());
    }
}
