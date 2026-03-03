package io.hivetrader.plugin.api.model;

import java.time.Instant;

public record NewsItem(
        String source,
        String headline,
        String symbol,
        Instant publishedAt
) {
}
