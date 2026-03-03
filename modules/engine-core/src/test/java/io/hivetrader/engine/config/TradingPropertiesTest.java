package io.hivetrader.engine.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradingPropertiesTest {

    @Test
    void propertiesAreMutableAndDefaultSafe() {
        TradingProperties properties = new TradingProperties();
        assertFalse(properties.getTrading().isEnabled());

        properties.getTrading().setEnabled(true);
        properties.getTrading().setKillSwitch(true);
        properties.getTrading().setPaused(true);
        properties.getTrading().setDefaultOrderSize(BigDecimal.TEN);
        properties.getTrading().setMaxTradesPerDay(7);

        properties.getScheduling().setFixedDelayMs(2000);
        properties.getScheduling().setInitialDelayMs(100);

        properties.getCassandra().setMode("astra");
        properties.getCassandra().setContactPoints("host:9042");
        properties.getCassandra().setKeyspace("ks");
        properties.getCassandra().setLocalDatacenter("dc");
        properties.getCassandra().setUsername("u");
        properties.getCassandra().setPassword("p");
        properties.getCassandra().setSecureConnectBundlePath("/bundle.zip");

        assertTrue(properties.getTrading().isEnabled());
        assertTrue(properties.getTrading().isKillSwitch());
        assertTrue(properties.getTrading().isPaused());
        assertEquals(BigDecimal.TEN, properties.getTrading().getDefaultOrderSize());
        assertEquals(7, properties.getTrading().getMaxTradesPerDay());
        assertEquals(2000, properties.getScheduling().getFixedDelayMs());
        assertEquals(100, properties.getScheduling().getInitialDelayMs());
        assertEquals("astra", properties.getCassandra().getMode());
        assertEquals("host:9042", properties.getCassandra().getContactPoints());
        assertEquals("ks", properties.getCassandra().getKeyspace());
        assertEquals("dc", properties.getCassandra().getLocalDatacenter());
        assertEquals("u", properties.getCassandra().getUsername());
        assertEquals("p", properties.getCassandra().getPassword());
        assertEquals("/bundle.zip", properties.getCassandra().getSecureConnectBundlePath());
    }
}
