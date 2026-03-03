package io.hivetrader.engine.charge;

import io.hivetrader.plugin.api.model.ExecutionResult;

import java.util.List;

public interface ChargeLedger {
    void recordCharges(String correlationId, String brokerId, List<ExecutionResult> results);
}
