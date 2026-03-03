package io.hivetrader.plugin.api.model;

import java.util.List;

public record RiskEvaluation(
        RiskDecision decision,
        RiskScore score,
        List<RiskLimit> limits,
        List<RiskEvidence> evidence
) {
    public static RiskEvaluation allow(String pluginId) {
        return new RiskEvaluation(
                RiskDecision.ALLOW,
                new RiskScore(0.0, 1.0),
                List.of(),
                List.of(new RiskEvidence(pluginId, "Allowed", java.util.Map.of()))
        );
    }
}
