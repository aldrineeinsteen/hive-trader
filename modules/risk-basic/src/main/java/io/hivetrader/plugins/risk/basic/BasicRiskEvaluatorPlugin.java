package io.hivetrader.plugins.risk.basic;

import io.hivetrader.plugin.api.model.PluginInfo;
import io.hivetrader.plugin.api.model.RiskContext;
import io.hivetrader.plugin.api.model.RiskDecision;
import io.hivetrader.plugin.api.model.RiskEvaluation;
import io.hivetrader.plugin.api.model.RiskEvidence;
import io.hivetrader.plugin.api.model.RiskLimit;
import io.hivetrader.plugin.api.model.RiskScore;
import io.hivetrader.plugin.api.spi.RiskEvaluatorPlugin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BasicRiskEvaluatorPlugin implements RiskEvaluatorPlugin {

    private static final BigDecimal MAX_DRAWDOWN = BigDecimal.valueOf(0.12);
    private static final int MAX_PROPOSALS = 5;

    @Override
    public PluginInfo info() {
        return new PluginInfo("risk-basic", "Basic Risk", "0.1.0", Set.of("risk", "drawdown", "bucket-cap"));
    }

    @Override
    public RiskEvaluation evaluate(RiskContext context) {
        BigDecimal drawdown = context.portfolio().drawdownPct();
        if (drawdown.compareTo(MAX_DRAWDOWN) >= 0) {
            return new RiskEvaluation(
                    RiskDecision.BLOCK,
                    new RiskScore(0.95, 0.9),
                    List.of(new RiskLimit("portfolio_drawdown_pct", MAX_DRAWDOWN.doubleValue(), drawdown.doubleValue(), "ratio")),
                    List.of(new RiskEvidence(info().id(), "Drawdown exceeds max", Map.of("drawdown", drawdown.toPlainString())))
            );
        }

        if (context.proposals().size() > MAX_PROPOSALS) {
            return new RiskEvaluation(
                    RiskDecision.ALLOW_WITH_LIMITS,
                    new RiskScore(0.50, 0.8),
                    List.of(new RiskLimit("max_trade_proposals", MAX_PROPOSALS, context.proposals().size(), "count")),
                    List.of(new RiskEvidence(info().id(), "Proposal count above preferred threshold", Map.of()))
            );
        }

        return RiskEvaluation.allow(info().id());
    }
}
