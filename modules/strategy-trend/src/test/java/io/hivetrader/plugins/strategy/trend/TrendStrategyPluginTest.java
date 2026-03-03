package io.hivetrader.plugins.strategy.trend;

import io.hivetrader.plugin.api.model.CandidateRanking;
import io.hivetrader.plugin.api.model.PortfolioSnapshot;
import io.hivetrader.plugin.api.model.StrategyContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrendStrategyPluginTest {

    @Test
    void generatesSignalWhenMovingAverageCrosses() {
        TrendStrategyPlugin plugin = new TrendStrategyPlugin();

        StrategyContext context = new StrategyContext(
                "corr",
                new PortfolioSnapshot(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, List.of(), Instant.now()),
                List.of(new CandidateRanking("explorer", "AAPL", BigDecimal.valueOf(0.9), "high")),
                Map.of("AAPL:prices", List.of(
                        BigDecimal.valueOf(100),
                        BigDecimal.valueOf(101),
                        BigDecimal.valueOf(102),
                        BigDecimal.valueOf(104),
                        BigDecimal.valueOf(106)
                )),
                Instant.now()
        );

        assertFalse(plugin.generateSignals(context).isEmpty());
    }

    @Test
    void generatesSellSignalWhenTrendTurnsDown() {
        TrendStrategyPlugin plugin = new TrendStrategyPlugin();

        StrategyContext context = new StrategyContext(
                "corr",
                new PortfolioSnapshot(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, List.of(), Instant.now()),
                List.of(new CandidateRanking("explorer", "AAPL", BigDecimal.valueOf(0.9), "high")),
                Map.of("AAPL:prices", List.of(
                        BigDecimal.valueOf(106),
                        BigDecimal.valueOf(104),
                        BigDecimal.valueOf(103),
                        BigDecimal.valueOf(101),
                        BigDecimal.valueOf(100)
                )),
                Instant.now()
        );

        assertTrue(plugin.generateSignals(context).stream().anyMatch(s -> s.side().name().equals("SELL")));
    }

    @Test
    void skipsSignalWhenInsufficientPriceHistory() {
        TrendStrategyPlugin plugin = new TrendStrategyPlugin();

        StrategyContext context = new StrategyContext(
                "corr",
                new PortfolioSnapshot(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, List.of(), Instant.now()),
                List.of(new CandidateRanking("explorer", "AAPL", BigDecimal.valueOf(0.9), "high")),
                Map.of("AAPL:prices", List.of(
                        BigDecimal.valueOf(106),
                        BigDecimal.valueOf(104),
                        BigDecimal.valueOf(103)
                )),
                Instant.now()
        );

        assertTrue(plugin.generateSignals(context).isEmpty());
    }
}
