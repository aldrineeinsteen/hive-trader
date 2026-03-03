package io.hivetrader.engine.service;

import io.hivetrader.plugin.api.model.SignalEvent;
import io.hivetrader.plugin.api.model.TradeSide;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Component
public class IdempotencyService {

    private final Clock clock;
    private final Set<String> knownOrderIds = new HashSet<>();

    public IdempotencyService() {
        this(Clock.systemUTC());
    }

    IdempotencyService(Clock clock) {
        this.clock = clock;
    }

    public String deterministicOrderId(String correlationId, SignalEvent signal, BigDecimal quantity) {
        String day = LocalDate.now(clock).atStartOfDay().toInstant(ZoneOffset.UTC).toString().substring(0, 10).replace("-", "");
        String payload = String.join("|",
                correlationId,
                day,
                signal.pluginId(),
                signal.symbol(),
                normalizeSide(signal.side()),
                quantity.stripTrailingZeros().toPlainString()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return "ht-" + token.substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public boolean markIfNew(String clientOrderId) {
        return knownOrderIds.add(clientOrderId);
    }

    private String normalizeSide(TradeSide side) {
        return side == null ? "UNKNOWN" : side.name();
    }
}
