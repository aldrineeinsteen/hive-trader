package io.hivetrader.engine.plugin;

import io.hivetrader.plugin.api.spi.BrokerAdapterPlugin;
import io.hivetrader.plugin.api.spi.DataProviderPlugin;
import io.hivetrader.plugin.api.spi.ExplorationPlugin;
import io.hivetrader.plugin.api.spi.FeatureExtractorPlugin;
import io.hivetrader.plugin.api.spi.NewsProviderPlugin;
import io.hivetrader.plugin.api.spi.RiskEvaluatorPlugin;
import io.hivetrader.plugin.api.spi.StrategyPlugin;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ServiceLoader;

@Component
public class PluginRegistry {

    private List<StrategyPlugin> strategyPlugins;
    private List<ExplorationPlugin> explorationPlugins;
    private List<RiskEvaluatorPlugin> riskEvaluatorPlugins;
    private List<BrokerAdapterPlugin> brokerAdapterPlugins;
    private List<DataProviderPlugin> dataProviderPlugins;
    private List<NewsProviderPlugin> newsProviderPlugins;
    private List<FeatureExtractorPlugin> featureExtractorPlugins;

    @PostConstruct
    void init() {
        strategyPlugins = loadPlugins(StrategyPlugin.class);
        explorationPlugins = loadPlugins(ExplorationPlugin.class);
        riskEvaluatorPlugins = loadPlugins(RiskEvaluatorPlugin.class);
        brokerAdapterPlugins = loadPlugins(BrokerAdapterPlugin.class);
        dataProviderPlugins = loadPlugins(DataProviderPlugin.class);
        newsProviderPlugins = loadPlugins(NewsProviderPlugin.class);
        featureExtractorPlugins = loadPlugins(FeatureExtractorPlugin.class);
    }

    private <T> List<T> loadPlugins(Class<T> type) {
        return ServiceLoader.load(type)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    public List<StrategyPlugin> strategyPlugins() {
        return strategyPlugins;
    }

    public List<ExplorationPlugin> explorationPlugins() {
        return explorationPlugins;
    }

    public List<RiskEvaluatorPlugin> riskEvaluatorPlugins() {
        return riskEvaluatorPlugins;
    }

    public List<BrokerAdapterPlugin> brokerAdapterPlugins() {
        return brokerAdapterPlugins;
    }

    public List<DataProviderPlugin> dataProviderPlugins() {
        return dataProviderPlugins;
    }

    public List<NewsProviderPlugin> newsProviderPlugins() {
        return newsProviderPlugins;
    }

    public List<FeatureExtractorPlugin> featureExtractorPlugins() {
        return featureExtractorPlugins;
    }
}
