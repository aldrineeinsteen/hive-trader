package io.hivetrader.engine.web;

import io.hivetrader.engine.plugin.PluginRegistry;
import io.hivetrader.plugin.api.model.PluginInfo;
import io.hivetrader.plugin.api.model.SignalEvent;
import io.hivetrader.plugin.api.model.StrategyContext;
import io.hivetrader.plugin.api.spi.StrategyPlugin;
import io.hivetrader.plugin.api.spi.TradingPlugin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginControllerTest {

    @Test
    void collectsPluginsAcrossAllTypes() {
        PluginRegistry registry = mock(PluginRegistry.class);
        TradingPlugin plugin = new StrategyPlugin() {
            @Override
            public List<SignalEvent> generateSignals(StrategyContext context) {
                return List.of();
            }

            @Override
            public PluginInfo info() {
                return new PluginInfo("p1", "Plugin 1", "1", Set.of("strategy"));
            }
        };

        when(registry.explorationPlugins()).thenReturn(List.of());
        when(registry.strategyPlugins()).thenReturn(List.of((StrategyPlugin) plugin));
        when(registry.riskEvaluatorPlugins()).thenReturn(List.of());
        when(registry.brokerAdapterPlugins()).thenReturn(List.of());
        when(registry.dataProviderPlugins()).thenReturn(List.of());
        when(registry.newsProviderPlugins()).thenReturn(List.of());
        when(registry.featureExtractorPlugins()).thenReturn(List.of());

        PluginController controller = new PluginController(registry);
        assertEquals(1, controller.plugins().size());
    }
}
