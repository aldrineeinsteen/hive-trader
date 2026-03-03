package io.hivetrader.plugin.api.model;

import java.util.Map;

public record RiskEvidence(
        String pluginId,
        String message,
        Map<String, String> attributes
) {
}
