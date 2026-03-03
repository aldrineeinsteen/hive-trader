package io.hivetrader.engine.control;

import java.time.Instant;

public record ControlFlags(
        boolean killSwitch,
        boolean paused,
        Instant pauseUntil
) {
    public boolean isPauseActive(Instant now) {
        return paused && pauseUntil != null && now.isBefore(pauseUntil);
    }
}
