package io.hivetrader.engine.charge;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import io.hivetrader.plugin.api.model.ExecutionResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@ConditionalOnBean(CqlSession.class)
public class CassandraChargeLedger implements ChargeLedger {

    private static final DateTimeFormatter DAY_BUCKET = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final CqlSession session;
    private final PreparedStatement insertChargeEvent;
    private final PreparedStatement insertChargeSummary;

    public CassandraChargeLedger(CqlSession session) {
        this.session = session;
        this.insertChargeEvent = session.prepare(SimpleStatement.newInstance("""
                INSERT INTO hive_trader.broker_charges_by_day
                (day_bucket, ts, correlation_id, client_order_id, broker_order_id, broker_id, charge_amount, charge_currency, accepted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """));
        this.insertChargeSummary = session.prepare(SimpleStatement.newInstance("""
                INSERT INTO hive_trader.charge_summary_by_cycle_day
                (day_bucket, correlation_id, broker_id, ts, total_charge_amount, charge_currency, order_count)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """));
    }

    @Override
    public void recordCharges(String correlationId, String brokerId, List<ExecutionResult> results) {
        Instant now = Instant.now();
        String dayBucket = DAY_BUCKET.format(now);

        BigDecimal total = BigDecimal.ZERO;
        String currency = "USD";
        for (ExecutionResult result : results) {
            BigDecimal charge = result.externalCharge() == null ? BigDecimal.ZERO : result.externalCharge();
            total = total.add(charge);
            if (result.externalChargeCurrency() != null && !result.externalChargeCurrency().isBlank()) {
                currency = result.externalChargeCurrency();
            }

            session.execute(insertChargeEvent.bind(
                    dayBucket,
                    result.executedAt() == null ? now : result.executedAt(),
                    correlationId,
                    result.clientOrderId(),
                    result.brokerOrderId(),
                    brokerId,
                    charge,
                    currency,
                    result.accepted()
            ));
        }

        session.execute(insertChargeSummary.bind(
                dayBucket,
                correlationId,
                brokerId,
                now,
                total,
                currency,
                results.size()
        ));
    }
}
