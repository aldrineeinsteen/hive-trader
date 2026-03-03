package io.hivetrader.plugin.api.model;

public record RiskScore(
        double score,
        double confidence
) {
}
