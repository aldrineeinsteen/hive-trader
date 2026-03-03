package io.hivetrader.plugin.api.spi;

import io.hivetrader.plugin.api.model.ExecutionRequest;
import io.hivetrader.plugin.api.model.ExecutionResult;

import java.util.List;

public interface BrokerAdapterPlugin extends TradingPlugin {
    List<ExecutionResult> submitOrders(ExecutionRequest request);
}
