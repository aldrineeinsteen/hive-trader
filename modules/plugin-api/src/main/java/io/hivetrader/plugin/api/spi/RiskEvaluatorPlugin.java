package io.hivetrader.plugin.api.spi;

import io.hivetrader.plugin.api.model.RiskContext;
import io.hivetrader.plugin.api.model.RiskEvaluation;

public interface RiskEvaluatorPlugin extends TradingPlugin {
    RiskEvaluation evaluate(RiskContext context);
}
