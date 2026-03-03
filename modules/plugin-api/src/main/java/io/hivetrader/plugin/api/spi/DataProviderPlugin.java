package io.hivetrader.plugin.api.spi;

import io.hivetrader.plugin.api.model.PortfolioSnapshot;

import java.math.BigDecimal;
import java.util.Map;

public interface DataProviderPlugin extends TradingPlugin {
    PortfolioSnapshot fetchPortfolio();

    Map<String, BigDecimal> fetchLastPrices();
}
