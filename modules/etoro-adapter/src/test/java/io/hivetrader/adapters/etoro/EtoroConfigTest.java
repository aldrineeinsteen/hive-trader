package io.hivetrader.adapters.etoro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EtoroConfigTest {

    @Test
    void explicitBlankCredentialsAreSafe() {
        EtoroConfig config = new EtoroConfig(
                "https://public-api.etoro.com",
                "",
                "",
                true,
                "/api/v1/trading/execution/demo/market-open-orders/by-amount",
                15
        );
        assertFalse(config.hasCredentials());
    }
}
