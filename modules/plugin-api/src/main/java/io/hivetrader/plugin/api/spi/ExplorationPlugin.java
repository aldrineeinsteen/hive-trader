package io.hivetrader.plugin.api.spi;

import io.hivetrader.plugin.api.model.CandidateRanking;
import io.hivetrader.plugin.api.model.ExplorationContext;

import java.util.List;

public interface ExplorationPlugin extends TradingPlugin {
    List<CandidateRanking> explore(ExplorationContext context);
}
