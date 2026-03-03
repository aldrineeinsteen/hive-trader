package io.hivetrader.plugin.api.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionResult(
        String correlationId,
        String clientOrderId,
        String brokerOrderId,
        boolean accepted,
        String message,
        Instant executedAt,
        BigDecimal externalCharge,
        String externalChargeCurrency
) {
}
