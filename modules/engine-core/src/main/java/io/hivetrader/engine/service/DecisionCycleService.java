package io.hivetrader.engine.service;

import io.hivetrader.engine.config.TradingProperties;
import io.hivetrader.engine.charge.ChargeLedger;
import io.hivetrader.engine.control.ControlFlagRepository;
import io.hivetrader.engine.control.ControlFlags;
import io.hivetrader.engine.metrics.EngineMetrics;
import io.hivetrader.engine.plugin.PluginRegistry;
import io.hivetrader.plugin.api.model.CandidateRanking;
import io.hivetrader.plugin.api.model.ExecutionRequest;
import io.hivetrader.plugin.api.model.ExecutionResult;
import io.hivetrader.plugin.api.model.ExplorationContext;
import io.hivetrader.plugin.api.model.PortfolioSnapshot;
import io.hivetrader.plugin.api.model.RiskContext;
import io.hivetrader.plugin.api.model.RiskDecision;
import io.hivetrader.plugin.api.model.RiskEvaluation;
import io.hivetrader.plugin.api.model.SignalEvent;
import io.hivetrader.plugin.api.model.StrategyContext;
import io.hivetrader.plugin.api.model.TradeProposal;
import io.hivetrader.plugin.api.spi.BrokerAdapterPlugin;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DecisionCycleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionCycleService.class);

    private final PluginRegistry pluginRegistry;
    private final ControlFlagRepository controlFlagRepository;
    private final TradingProperties tradingProperties;
    private final DefaultDataProvider defaultDataProvider;
    private final IdempotencyService idempotencyService;
    private final EngineMetrics engineMetrics;
    private final ChargeLedger chargeLedger;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public DecisionCycleService(
            PluginRegistry pluginRegistry,
            ControlFlagRepository controlFlagRepository,
            TradingProperties tradingProperties,
            DefaultDataProvider defaultDataProvider,
            IdempotencyService idempotencyService,
            EngineMetrics engineMetrics,
                ChargeLedger chargeLedger,
            MeterRegistry meterRegistry
    ) {
        this.pluginRegistry = pluginRegistry;
        this.controlFlagRepository = controlFlagRepository;
        this.tradingProperties = tradingProperties;
        this.defaultDataProvider = defaultDataProvider;
        this.idempotencyService = idempotencyService;
        this.engineMetrics = engineMetrics;
        this.chargeLedger = chargeLedger;
        this.meterRegistry = meterRegistry;
        this.clock = Clock.systemUTC();
    }

    public void runCycle() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        Instant now = Instant.now(clock);

        try {
            LOGGER.info("event=decision_cycle_start correlationId={} mode=observe_only_default", correlationId);

            ControlFlags flags = controlFlagRepository.loadFlags();
            boolean killSwitchActive = flags.killSwitch() || tradingProperties.getTrading().isKillSwitch();
            boolean pausedActive = flags.isPauseActive(now) || tradingProperties.getTrading().isPaused();
            boolean tradingEnabled = tradingProperties.getTrading().isEnabled();
            engineMetrics.updateTradingGates(tradingEnabled, killSwitchActive);

            PortfolioSnapshot portfolio = defaultDataProvider.fetchPortfolio(Map.of());
            engineMetrics.updatePortfolio(
                    portfolio.equity().doubleValue(),
                    portfolio.drawdownPct().doubleValue()
            );

            List<CandidateRanking> candidates = pluginRegistry.explorationPlugins().stream()
                    .flatMap(plugin -> plugin.explore(new ExplorationContext(correlationId, portfolio, Map.of(), now)).stream())
                    .toList();

            List<SignalEvent> signals = pluginRegistry.strategyPlugins().stream()
                    .flatMap(plugin -> plugin.generateSignals(new StrategyContext(correlationId, portfolio, candidates, Map.of(), now)).stream())
                    .toList();

            signals.forEach(signal -> engineMetrics.incrementSignalGenerated(meterRegistry, signal.pluginId()));

            List<TradeProposal> proposals = signals.stream()
                    .map(signal -> {
                        String clientOrderId = idempotencyService.deterministicOrderId(
                                correlationId,
                                signal,
                                tradingProperties.getTrading().getDefaultOrderSize()
                        );
                        return new TradeProposal(
                                correlationId,
                                signal.pluginId(),
                                signal.symbol(),
                                signal.side(),
                                tradingProperties.getTrading().getDefaultOrderSize(),
                                null,
                                clientOrderId,
                                now
                        );
                    })
                    .filter(proposal -> idempotencyService.markIfNew(proposal.clientOrderId()))
                    .limit(tradingProperties.getTrading().getMaxTradesPerDay())
                    .toList();

            List<RiskEvaluation> evaluations = pluginRegistry.riskEvaluatorPlugins().stream()
                    .map(plugin -> plugin.evaluate(new RiskContext(correlationId, portfolio, proposals, Map.of(), now)))
                    .toList();

            boolean blocked = evaluations.stream().anyMatch(it -> it.decision() == RiskDecision.BLOCK);
            if (blocked) {
                signals.forEach(signal -> engineMetrics.incrementSignalBlockedByRisk(meterRegistry, signal.pluginId()));
                LOGGER.warn("event=decision_cycle_blocked_by_risk correlationId={} evaluations={}", correlationId, evaluations.size());
                return;
            }

            if (!tradingEnabled || killSwitchActive || pausedActive) {
                LOGGER.info(
                        "event=decision_cycle_observe_only correlationId={} tradingEnabled={} killSwitch={} paused={}",
                        correlationId,
                        tradingEnabled,
                        killSwitchActive,
                        pausedActive
                );
                return;
            }

            executeOrders(correlationId, proposals);
            LOGGER.info("event=decision_cycle_end correlationId={} submittedOrders={}", correlationId, proposals.size());
        } catch (Exception exception) {
            LOGGER.error("event=decision_cycle_error correlationId={} message={}", correlationId, exception.getMessage(), exception);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void executeOrders(String correlationId, List<TradeProposal> proposals) {
        if (proposals.isEmpty()) {
            return;
        }
        List<BrokerAdapterPlugin> brokers = pluginRegistry.brokerAdapterPlugins();
        if (brokers.isEmpty()) {
            LOGGER.warn("event=order_execution_skipped_no_broker correlationId={} proposalCount={}", correlationId, proposals.size());
            return;
        }

        BrokerAdapterPlugin broker = brokers.getFirst();
        List<ExecutionResult> results = new ArrayList<>(broker.submitOrders(new ExecutionRequest(correlationId, proposals)));

        for (ExecutionResult result : results) {
            if (result.accepted()) {
                engineMetrics.incrementOrdersSubmitted();
            } else {
                engineMetrics.incrementOrdersFailed();
            }

            if (result.externalCharge() != null && result.externalCharge().doubleValue() > 0.0) {
                engineMetrics.incrementExternalCharge(
                        meterRegistry,
                        broker.info().id(),
                        result.externalChargeCurrency(),
                        result.externalCharge().doubleValue()
                );
            }
        }

        chargeLedger.recordCharges(correlationId, broker.info().id(), results);
    }
}
