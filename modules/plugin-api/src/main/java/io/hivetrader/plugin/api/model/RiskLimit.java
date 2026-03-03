package io.hivetrader.plugin.api.model;

public record RiskLimit(
        String metric,
        double maxValue,
        double actualValue,
        String unit
) {
}
