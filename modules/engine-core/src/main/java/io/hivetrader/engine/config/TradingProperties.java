package io.hivetrader.engine.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "hive")
public class TradingProperties {

    @Valid
    private final Trading trading = new Trading();

    @Valid
    private final Scheduling scheduling = new Scheduling();

    @Valid
    private final Cassandra cassandra = new Cassandra();

    public Trading getTrading() {
        return trading;
    }

    public Scheduling getScheduling() {
        return scheduling;
    }

    public Cassandra getCassandra() {
        return cassandra;
    }

    public static class Trading {
        private boolean enabled = false;
        private boolean killSwitch = false;
        private boolean paused = false;

        @NotNull
        @DecimalMin("0.0001")
        private BigDecimal defaultOrderSize = BigDecimal.ONE;

        @Min(1)
        private int maxTradesPerDay = 20;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isKillSwitch() {
            return killSwitch;
        }

        public void setKillSwitch(boolean killSwitch) {
            this.killSwitch = killSwitch;
        }

        public boolean isPaused() {
            return paused;
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
        }

        public BigDecimal getDefaultOrderSize() {
            return defaultOrderSize;
        }

        public void setDefaultOrderSize(BigDecimal defaultOrderSize) {
            this.defaultOrderSize = defaultOrderSize;
        }

        public int getMaxTradesPerDay() {
            return maxTradesPerDay;
        }

        public void setMaxTradesPerDay(int maxTradesPerDay) {
            this.maxTradesPerDay = maxTradesPerDay;
        }
    }

    public static class Scheduling {
        @Min(500)
        private long fixedDelayMs = 15000;

        @Min(0)
        private long initialDelayMs = 1000;

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }
    }

    public static class Cassandra {
        private boolean sessionEnabled = false;
        private String mode = "local";
        private String contactPoints = "127.0.0.1:9042";
        private String keyspace = "hive_trader";
        private String localDatacenter = "datacenter1";
        private String username = "";
        private String password = "";
        private String secureConnectBundlePath = "";

        public boolean isSessionEnabled() {
            return sessionEnabled;
        }

        public void setSessionEnabled(boolean sessionEnabled) {
            this.sessionEnabled = sessionEnabled;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getContactPoints() {
            return contactPoints;
        }

        public void setContactPoints(String contactPoints) {
            this.contactPoints = contactPoints;
        }

        public String getKeyspace() {
            return keyspace;
        }

        public void setKeyspace(String keyspace) {
            this.keyspace = keyspace;
        }

        public String getLocalDatacenter() {
            return localDatacenter;
        }

        public void setLocalDatacenter(String localDatacenter) {
            this.localDatacenter = localDatacenter;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getSecureConnectBundlePath() {
            return secureConnectBundlePath;
        }

        public void setSecureConnectBundlePath(String secureConnectBundlePath) {
            this.secureConnectBundlePath = secureConnectBundlePath;
        }
    }
}
