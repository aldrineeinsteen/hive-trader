package io.hivetrader.engine;

import io.hivetrader.engine.config.TradingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TradingProperties.class)
public class HiveTraderApplication {

    public static void main(String[] args) {
        SpringApplication.run(HiveTraderApplication.class, args);
    }
}
