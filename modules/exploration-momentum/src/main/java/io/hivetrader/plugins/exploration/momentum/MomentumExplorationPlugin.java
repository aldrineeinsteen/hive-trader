package io.hivetrader.plugins.exploration.momentum;

import io.hivetrader.plugin.api.model.CandidateRanking;
import io.hivetrader.plugin.api.model.ExplorationContext;
import io.hivetrader.plugin.api.model.PluginInfo;
import io.hivetrader.plugin.api.spi.ExplorationPlugin;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class MomentumExplorationPlugin implements ExplorationPlugin {

    @Override
    public PluginInfo info() {
        return new PluginInfo("exploration-momentum", "Momentum Exploration", "0.1.0", Set.of("exploration", "momentum"));
    }

    @Override
    public java.util.List<CandidateRanking> explore(ExplorationContext context) {
        return context.marketState().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof BigDecimal)
                .map(entry -> new CandidateRanking(
                        info().id(),
                        entry.getKey(),
                        (BigDecimal) entry.getValue(),
                        bucket((BigDecimal) entry.getValue())
                ))
                .filter(candidate -> candidate.score().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(CandidateRanking::score).reversed())
                .limit(20)
                .toList();
    }

    private String bucket(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(0.08)) >= 0) {
            return "high";
        }
        if (score.compareTo(BigDecimal.valueOf(0.04)) >= 0) {
            return "medium";
        }
        return "low";
    }
}
