package io.hivetrader.engine.control;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlFlagsTest {

    @Test
    void pauseIsActiveOnlyBeforePauseUntil() {
        Instant now = Instant.parse("2026-03-03T00:00:00Z");
        ControlFlags active = new ControlFlags(false, true, now.plusSeconds(60));
        ControlFlags inactive = new ControlFlags(false, true, now.minusSeconds(60));

        assertTrue(active.isPauseActive(now));
        assertFalse(inactive.isPauseActive(now));
    }
}
