package io.hivetrader.engine.web;

import io.hivetrader.engine.plugin.PluginRegistry;
import io.hivetrader.plugin.api.model.PluginInfo;
import io.hivetrader.plugin.api.spi.TradingPlugin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class PluginController {

    private final PluginRegistry registry;

    public PluginController(PluginRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/plugins")
    public List<PluginInfo> plugins() {
        List<PluginInfo> infos = new ArrayList<>();
        addInfo(infos, registry.explorationPlugins());
        addInfo(infos, registry.strategyPlugins());
        addInfo(infos, registry.riskEvaluatorPlugins());
        addInfo(infos, registry.brokerAdapterPlugins());
        addInfo(infos, registry.dataProviderPlugins());
        addInfo(infos, registry.newsProviderPlugins());
        addInfo(infos, registry.featureExtractorPlugins());
        return infos;
    }

    private void addInfo(List<PluginInfo> infos, List<? extends TradingPlugin> plugins) {
        plugins.stream().map(TradingPlugin::info).forEach(infos::add);
    }
}
