package io.hivetrader.engine.service;

import io.hivetrader.plugin.api.model.SignalEvent;
import io.hivetrader.plugin.api.model.TradeSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyServiceTest {

    @Test
    void deterministicIdIsStableAndUniqueBySignal() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
        IdempotencyService service = new IdempotencyService(clock);

        SignalEvent buySignal = new SignalEvent("strategy", "AAPL", TradeSide.BUY, BigDecimal.ONE, "reason", Instant.now());
        SignalEvent sellSignal = new SignalEvent("strategy", "AAPL", TradeSide.SELL, BigDecimal.ONE, "reason", Instant.now());

        String id1 = service.deterministicOrderId("corr", buySignal, BigDecimal.ONE);
        String id2 = service.deterministicOrderId("corr", buySignal, BigDecimal.ONE);
        String id3 = service.deterministicOrderId("corr", sellSignal, BigDecimal.ONE);

        assertEquals(id1, id2);
        assertFalse(id1.equals(id3));
        assertTrue(service.markIfNew(id1));
        assertFalse(service.markIfNew(id1));
    }
}
