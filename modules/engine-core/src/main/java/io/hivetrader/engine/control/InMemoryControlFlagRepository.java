package io.hivetrader.engine.control;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InMemoryControlFlagRepository implements ControlFlagRepository {

    @Override
    public ControlFlags loadFlags() {
        return new ControlFlags(false, false, Instant.EPOCH);
    }
}
