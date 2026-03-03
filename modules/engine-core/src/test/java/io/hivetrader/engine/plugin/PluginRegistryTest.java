package io.hivetrader.engine.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PluginRegistryTest {

    @Test
    void initializesPluginCollections() {
        PluginRegistry registry = new PluginRegistry();
        registry.init();

        assertNotNull(registry.strategyPlugins());
        assertNotNull(registry.explorationPlugins());
        assertNotNull(registry.riskEvaluatorPlugins());
        assertNotNull(registry.brokerAdapterPlugins());
        assertNotNull(registry.dataProviderPlugins());
        assertNotNull(registry.newsProviderPlugins());
        assertNotNull(registry.featureExtractorPlugins());
    }
}
