package io.hivetrader.plugin.api.spi;

import java.util.Map;

public interface FeatureExtractorPlugin extends TradingPlugin {
    Map<String, Double> extractFeatures(String text, String symbol);
}
