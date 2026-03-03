package io.hivetrader.engine.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
@ConditionalOnProperty(prefix = "hive.cassandra", name = "session-enabled", havingValue = "true")
public class CassandraSessionConfig {

    @Bean(destroyMethod = "close")
    public CqlSession cqlSession(TradingProperties properties) {
        TradingProperties.Cassandra cassandra = properties.getCassandra();

        if ("astra".equalsIgnoreCase(cassandra.getMode()) && cassandra.getSecureConnectBundlePath() != null && !cassandra.getSecureConnectBundlePath().isBlank()) {
            CqlSessionBuilder builder = CqlSession.builder()
                    .withCloudSecureConnectBundle(java.nio.file.Path.of(cassandra.getSecureConnectBundlePath()));

            if (cassandra.getUsername() != null && !cassandra.getUsername().isBlank()) {
                builder.withAuthCredentials(cassandra.getUsername(), cassandra.getPassword());
            }
            return builder.withKeyspace(cassandra.getKeyspace()).build();
        }

        String[] hostPort = cassandra.getContactPoints().split(":");
        String host = hostPort[0];
        int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 9042;

        CqlSessionBuilder builder = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, port))
                .withLocalDatacenter(cassandra.getLocalDatacenter())
                .withKeyspace(cassandra.getKeyspace());

        if (cassandra.getUsername() != null && !cassandra.getUsername().isBlank()) {
            builder.withAuthCredentials(cassandra.getUsername(), cassandra.getPassword());
        }

        return builder.build();
    }
}
