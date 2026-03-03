package io.hivetrader.engine.service;

import io.hivetrader.engine.config.TradingProperties;
import io.hivetrader.engine.control.ControlFlags;
import io.hivetrader.engine.charge.ChargeLedger;
import io.hivetrader.engine.metrics.EngineMetrics;
import io.hivetrader.engine.plugin.PluginRegistry;
import io.hivetrader.plugin.api.model.CandidateRanking;
import io.hivetrader.plugin.api.model.ExecutionResult;
import io.hivetrader.plugin.api.model.PluginInfo;
import io.hivetrader.plugin.api.model.RiskContext;
import io.hivetrader.plugin.api.model.RiskDecision;
import io.hivetrader.plugin.api.model.RiskEvaluation;
import io.hivetrader.plugin.api.model.RiskScore;
import io.hivetrader.plugin.api.model.SignalEvent;
import io.hivetrader.plugin.api.model.TradeSide;
import io.hivetrader.plugin.api.spi.BrokerAdapterPlugin;
import io.hivetrader.plugin.api.spi.ExplorationPlugin;
import io.hivetrader.plugin.api.spi.RiskEvaluatorPlugin;
import io.hivetrader.plugin.api.spi.StrategyPlugin;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DecisionCycleServiceTest {

    private PluginRegistry pluginRegistry;
    private TradingProperties properties;
    private BrokerAdapterPlugin broker;
    private RiskEvaluatorPlugin risk;
        private ChargeLedger chargeLedger;

    @BeforeEach
    void setUp() {
        pluginRegistry = mock(PluginRegistry.class);
        properties = new TradingProperties();
        broker = mock(BrokerAdapterPlugin.class);
        risk = mock(RiskEvaluatorPlugin.class);
        chargeLedger = mock(ChargeLedger.class);

        ExplorationPlugin exploration = mock(ExplorationPlugin.class);
        when(exploration.explore(any())).thenReturn(List.of(new CandidateRanking("explorer", "AAPL", BigDecimal.valueOf(0.4), "high")));

        StrategyPlugin strategy = mock(StrategyPlugin.class);
        when(strategy.generateSignals(any())).thenReturn(List.of(
                new SignalEvent("strategy-1", "AAPL", TradeSide.BUY, BigDecimal.valueOf(0.8), "trend", Instant.now())
        ));

        when(pluginRegistry.explorationPlugins()).thenReturn(List.of(exploration));
        when(pluginRegistry.strategyPlugins()).thenReturn(List.of(strategy));
        when(pluginRegistry.riskEvaluatorPlugins()).thenReturn(List.of(risk));
        when(pluginRegistry.brokerAdapterPlugins()).thenReturn(List.of(broker));
        when(pluginRegistry.dataProviderPlugins()).thenReturn(List.of());
        when(pluginRegistry.newsProviderPlugins()).thenReturn(List.of());
        when(pluginRegistry.featureExtractorPlugins()).thenReturn(List.of());

        when(risk.info()).thenReturn(new PluginInfo("risk", "risk", "1", Set.of("risk")));
        when(broker.submitOrders(any())).thenReturn(List.of(
                new ExecutionResult("corr", "id", "broker-id", true, "ok", Instant.now(), BigDecimal.ZERO, "USD")
        ));
    }

    @Test
    void executesOrderWhenTradingEnabledAndRiskAllows() {
        properties.getTrading().setEnabled(true);
        when(risk.evaluate(any(RiskContext.class))).thenReturn(new RiskEvaluation(
                RiskDecision.ALLOW,
                new RiskScore(0.1, 0.8),
                List.of(),
                List.of()
        ));

        DecisionCycleService service = new DecisionCycleService(
                pluginRegistry,
                () -> new ControlFlags(false, false, Instant.EPOCH),
                properties,
                new DefaultDataProvider(),
                new IdempotencyService(),
                new EngineMetrics(new SimpleMeterRegistry()),
                chargeLedger,
                new SimpleMeterRegistry()
        );

        service.runCycle();

        verify(broker).submitOrders(any());
    }

    @Test
    void doesNotExecuteOrderWhenRiskBlocks() {
        properties.getTrading().setEnabled(true);
        when(risk.evaluate(any(RiskContext.class))).thenReturn(new RiskEvaluation(
                RiskDecision.BLOCK,
                new RiskScore(0.9, 0.9),
                List.of(),
                List.of()
        ));

        DecisionCycleService service = new DecisionCycleService(
                pluginRegistry,
                () -> new ControlFlags(false, false, Instant.EPOCH),
                properties,
                new DefaultDataProvider(),
                new IdempotencyService(),
                new EngineMetrics(new SimpleMeterRegistry()),
                chargeLedger,
                new SimpleMeterRegistry()
        );

        service.runCycle();

        verify(broker, never()).submitOrders(any());
    }

        @Test
        void remainsObserveOnlyWhenTradingDisabled() {
                properties.getTrading().setEnabled(false);
                when(risk.evaluate(any(RiskContext.class))).thenReturn(new RiskEvaluation(
                                RiskDecision.ALLOW,
                                new RiskScore(0.1, 0.8),
                                List.of(),
                                List.of()
                ));

                DecisionCycleService service = new DecisionCycleService(
                                pluginRegistry,
                                () -> new ControlFlags(false, false, Instant.EPOCH),
                                properties,
                                new DefaultDataProvider(),
                                new IdempotencyService(),
                                new EngineMetrics(new SimpleMeterRegistry()),
                                chargeLedger,
                                new SimpleMeterRegistry()
                );

                service.runCycle();
                verify(broker, never()).submitOrders(any());
        }

        @Test
        void skipsExecutionWhenNoBrokerPluginPresent() {
                properties.getTrading().setEnabled(true);
                when(risk.evaluate(any(RiskContext.class))).thenReturn(new RiskEvaluation(
                                RiskDecision.ALLOW,
                                new RiskScore(0.1, 0.8),
                                List.of(),
                                List.of()
                ));
                when(pluginRegistry.brokerAdapterPlugins()).thenReturn(List.of());

                DecisionCycleService service = new DecisionCycleService(
                                pluginRegistry,
                                () -> new ControlFlags(false, false, Instant.EPOCH),
                                properties,
                                new DefaultDataProvider(),
                                new IdempotencyService(),
                                new EngineMetrics(new SimpleMeterRegistry()),
                                chargeLedger,
                                new SimpleMeterRegistry()
                );

                service.runCycle();
                verify(broker, never()).submitOrders(any());
        }
}
