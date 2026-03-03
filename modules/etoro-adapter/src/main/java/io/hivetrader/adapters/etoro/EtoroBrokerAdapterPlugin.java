package io.hivetrader.adapters.etoro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hivetrader.plugin.api.model.ExecutionRequest;
import io.hivetrader.plugin.api.model.ExecutionResult;
import io.hivetrader.plugin.api.model.PluginInfo;
import io.hivetrader.plugin.api.model.TradeProposal;
import io.hivetrader.plugin.api.spi.BrokerAdapterPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EtoroBrokerAdapterPlugin implements BrokerAdapterPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(EtoroBrokerAdapterPlugin.class);

    private final EtoroConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EtoroBrokerAdapterPlugin() {
        this(EtoroConfig.fromEnvironment(), HttpClient.newHttpClient(), new ObjectMapper());
    }

    EtoroBrokerAdapterPlugin(EtoroConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PluginInfo info() {
        return new PluginInfo("broker-etoro", "eToro Broker Adapter", "0.1.0", Set.of("broker", "etoro", "market-order"));
    }

    @Override
    public List<ExecutionResult> submitOrders(ExecutionRequest request) {
        if (!config.hasCredentials()) {
            LOGGER.warn("event=broker_etoro_missing_credentials correlationId={}", request.correlationId());
            return request.proposals().stream()
                    .map(proposal -> reject(request.correlationId(), proposal.clientOrderId(), "Missing ETORO_API_KEY or ETORO_USER_KEY"))
                    .toList();
        }

        List<ExecutionResult> results = new ArrayList<>();
        for (TradeProposal proposal : request.proposals()) {
            results.add(submitSingle(request.correlationId(), proposal));
        }
        return results;
    }

    private ExecutionResult submitSingle(String correlationId, TradeProposal proposal) {
        try {
            Long instrumentId = resolveInstrumentId(correlationId, proposal.symbol());
            if (instrumentId == null) {
                return reject(correlationId, proposal.clientOrderId(), "Unable to resolve instrument ID for symbol");
            }

            String payload = objectMapper.createObjectNode()
                    .put("InstrumentId", instrumentId)
                    .put("Amount", proposal.quantity().doubleValue())
                    .put("Leverage", 1)
                    .put("IsBuy", proposal.side().name().equals("BUY"))
                    .toString();

            HttpRequest orderRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + config.openByAmountPath()))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", config.apiKey())
                    .header("x-user-key", config.userKey())
                    .header("x-request-id", proposal.clientOrderId())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(orderRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String brokerOrderId = extractTextValue(response.body(), proposal.clientOrderId(), "OrderId", "orderID", "orderId");
                BigDecimal externalCharge = extractDecimalValue(response.body(), "totalExternalCosts", "TotalExternalCosts");
                return new ExecutionResult(
                        correlationId,
                        proposal.clientOrderId(),
                        brokerOrderId,
                        true,
                        "accepted",
                        Instant.now(),
                        externalCharge,
                        "USD"
                );
            }

            return reject(correlationId, proposal.clientOrderId(), "Broker rejected order: HTTP " + response.statusCode());
        } catch (Exception exception) {
            LOGGER.error(
                    "event=broker_etoro_submit_failed correlationId={} clientOrderId={} message={}",
                    correlationId,
                    proposal.clientOrderId(),
                    exception.getMessage()
            );
            return reject(correlationId, proposal.clientOrderId(), "Adapter exception");
        }
    }

    private Long resolveInstrumentId(String correlationId, String symbol) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
        HttpRequest searchRequest = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/api/v1/market-data/search?internalSymbolFull=" + encoded))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .header("x-api-key", config.apiKey())
                .header("x-user-key", config.userKey())
                .header("x-request-id", UUID.randomUUID().toString())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOGGER.warn("event=broker_etoro_symbol_lookup_failed correlationId={} symbol={} status={}", correlationId, symbol, response.statusCode());
            return null;
        }

        return extractLongValue(response.body(), "InstrumentId", "InstrumentID");
    }

    private Long extractLongValue(String body, String... keys) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        for (String key : keys) {
            JsonNode direct = root.get(key);
            if (direct != null && direct.isNumber()) {
                return direct.longValue();
            }
        }

        JsonNode results = root.get("Results");
        if (results != null && results.isArray() && !results.isEmpty()) {
            JsonNode first = results.get(0);
            for (String key : keys) {
                JsonNode nested = first.get(key);
                if (nested != null && nested.isNumber()) {
                    return nested.longValue();
                }
            }
        }

        JsonNode items = root.get("items");
        if (items != null && items.isArray() && !items.isEmpty()) {
            JsonNode first = items.get(0);
            for (String key : keys) {
                JsonNode nested = first.get(key);
                if (nested != null && nested.isNumber()) {
                    return nested.longValue();
                }
            }
        }

        return null;
    }

    private String extractTextValue(String body, String fallback, String... keys) throws IOException {
        JsonNode root = objectMapper.readTree(body);

        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node == null && root.has("orderForOpen") && root.get("orderForOpen").isObject()) {
                node = root.get("orderForOpen").get(key);
            }
            if (node != null && node.isTextual()) {
                return node.textValue();
            }
            if (node != null && node.isNumber()) {
                return node.asText();
            }
        }
        return fallback;
    }

    private BigDecimal extractDecimalValue(String body, String... keys) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        for (String key : keys) {
            JsonNode direct = root.get(key);
            if (direct != null && direct.isNumber()) {
                return direct.decimalValue();
            }
        }

        JsonNode orderForOpen = root.get("orderForOpen");
        if (orderForOpen != null && orderForOpen.isObject()) {
            for (String key : keys) {
                JsonNode nested = orderForOpen.get(key);
                if (nested != null && nested.isNumber()) {
                    return nested.decimalValue();
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private ExecutionResult reject(String correlationId, String clientOrderId, String message) {
        return new ExecutionResult(correlationId, clientOrderId, null, false, message, Instant.now(), BigDecimal.ZERO, "USD");
    }
}
