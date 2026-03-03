package io.hivetrader.engine.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class CassandraContainerSmokeTest {

    @Container
    static CassandraContainer<?> cassandra = new CassandraContainer<>("cassandra:5.0");

    @Test
    void startsCassandraContainer() {
        assertTrue(cassandra.isRunning());
        assertTrue(cassandra.getMappedPort(9042) > 0);
    }
}
