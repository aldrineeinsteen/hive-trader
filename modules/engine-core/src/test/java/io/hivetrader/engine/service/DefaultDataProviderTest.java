package io.hivetrader.engine.service;

import io.hivetrader.plugin.api.model.PluginInfo;
import io.hivetrader.plugin.api.model.PortfolioSnapshot;
import io.hivetrader.plugin.api.spi.DataProviderPlugin;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultDataProviderTest {

    @Test
    void usesPluginPortfolioWhenAvailable() {
        DefaultDataProvider provider = new DefaultDataProvider();
        PortfolioSnapshot expected = new PortfolioSnapshot(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, List.of(), Instant.now());
        DataProviderPlugin plugin = new DataProviderPlugin() {
            @Override
            public PluginInfo info() {
                return new PluginInfo("data", "data", "1", Set.of());
            }

            @Override
            public PortfolioSnapshot fetchPortfolio() {
                return expected;
            }

            @Override
            public Map<String, BigDecimal> fetchLastPrices() {
                return Map.of();
            }
        };

        PortfolioSnapshot actual = provider.fetchPortfolio(Map.of("data", plugin));
        assertEquals(expected, actual);
    }

    @Test
    void fallsBackWhenNoDataProvider() {
        DefaultDataProvider provider = new DefaultDataProvider();
        PortfolioSnapshot actual = provider.fetchPortfolio(Map.of());
        assertEquals(BigDecimal.valueOf(100_000), actual.equity());
    }
}
