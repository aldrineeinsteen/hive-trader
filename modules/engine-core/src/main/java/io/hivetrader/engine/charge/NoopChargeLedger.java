package io.hivetrader.engine.charge;

import io.hivetrader.plugin.api.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnMissingBean(ChargeLedger.class)
public class NoopChargeLedger implements ChargeLedger {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoopChargeLedger.class);

    @Override
    public void recordCharges(String correlationId, String brokerId, List<ExecutionResult> results) {
        BigDecimal total = results.stream()
                .map(ExecutionResult::externalCharge)
                .filter(charge -> charge != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LOGGER.info(
                "event=charge_consolidation_in_memory correlationId={} brokerId={} totalCharge={} currency={} orders={}",
                correlationId,
                brokerId,
                total,
                "USD",
                results.size()
        );
    }
}
