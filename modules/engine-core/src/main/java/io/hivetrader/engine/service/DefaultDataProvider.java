package io.hivetrader.engine.service;

import io.hivetrader.plugin.api.model.PortfolioSnapshot;
import io.hivetrader.plugin.api.spi.DataProviderPlugin;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class DefaultDataProvider {

    public PortfolioSnapshot fallbackPortfolio() {
        return new PortfolioSnapshot(
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(100_000),
                BigDecimal.ZERO,
                List.of(),
                Instant.now()
        );
    }

    public PortfolioSnapshot fetchPortfolio(Map<String, DataProviderPlugin> providers) {
        return providers.values().stream().findFirst()
                .map(DataProviderPlugin::fetchPortfolio)
                .orElseGet(this::fallbackPortfolio);
    }
}
