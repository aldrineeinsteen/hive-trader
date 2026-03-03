package io.hivetrader.plugins.exploration.momentum;

import io.hivetrader.plugin.api.model.ExplorationContext;
import io.hivetrader.plugin.api.model.PortfolioSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MomentumExplorationPluginTest {

    @Test
    void ranksPositiveMomentumSymbols() {
        MomentumExplorationPlugin plugin = new MomentumExplorationPlugin();

        ExplorationContext context = new ExplorationContext(
                "corr",
                new PortfolioSnapshot(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, List.of(), Instant.now()),
                Map.of(
                        "AAPL", BigDecimal.valueOf(0.09),
                        "MSFT", BigDecimal.valueOf(0.03),
                        "TSLA", BigDecimal.valueOf(-0.01)
                ),
                Instant.now()
        );

        var rankings = plugin.explore(context);
        assertEquals(2, rankings.size());
        assertEquals("AAPL", rankings.getFirst().symbol());
    }
}
