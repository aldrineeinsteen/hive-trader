package io.hivetrader.plugin.api.model;

import java.math.BigDecimal;

public record CandidateRanking(
        String pluginId,
        String symbol,
        BigDecimal score,
        String bucket
) {
}
