package io.hivetrader.adapters.etoro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.hivetrader.plugin.api.model.ExecutionRequest;
import io.hivetrader.plugin.api.model.TradeProposal;
import io.hivetrader.plugin.api.model.TradeSide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EtoroBrokerAdapterPluginTest {

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void rejectsWhenCredentialsMissing() {
        EtoroConfig config = new EtoroConfig(server.baseUrl(), "", "", true,
                "/api/v1/trading/execution/demo/market-open-orders/by-amount", 5);

        EtoroBrokerAdapterPlugin plugin = new EtoroBrokerAdapterPlugin(config, HttpClient.newHttpClient(), new ObjectMapper());
        var result = plugin.submitOrders(new ExecutionRequest("corr", List.of(sampleProposal())));

        assertFalse(result.getFirst().accepted());
    }

    @Test
    void submitsOrderWhenLookupAndOrderSucceed() {
        server.stubFor(get(urlPathEqualTo("/api/v1/market-data/search"))
                .willReturn(aResponse().withStatus(200).withBody("{\"Results\":[{\"InstrumentId\":100000}]}")));
        server.stubFor(post(urlPathMatching("/api/v1/trading/execution/demo/market-open-orders/by-amount"))
                .willReturn(aResponse().withStatus(200).withBody("{\"OrderId\":\"ord-123\"}")));

        EtoroConfig config = new EtoroConfig(server.baseUrl(), "api-key", "user-key", true,
                "/api/v1/trading/execution/demo/market-open-orders/by-amount", 5);

        EtoroBrokerAdapterPlugin plugin = new EtoroBrokerAdapterPlugin(config, HttpClient.newHttpClient(), new ObjectMapper());
        var result = plugin.submitOrders(new ExecutionRequest("corr", List.of(sampleProposal())));

        assertTrue(result.getFirst().accepted());
        assertEquals(new BigDecimal("0"), result.getFirst().externalCharge());
    }

    @Test
    void rejectsWhenInstrumentLookupFails() {
        server.stubFor(get(urlPathEqualTo("/api/v1/market-data/search"))
                .willReturn(aResponse().withStatus(404).withBody("{}")));

        EtoroConfig config = new EtoroConfig(server.baseUrl(), "api-key", "user-key", true,
                "/api/v1/trading/execution/demo/market-open-orders/by-amount", 5);

        EtoroBrokerAdapterPlugin plugin = new EtoroBrokerAdapterPlugin(config, HttpClient.newHttpClient(), new ObjectMapper());
        var result = plugin.submitOrders(new ExecutionRequest("corr", List.of(sampleProposal())));

        assertFalse(result.getFirst().accepted());
    }

    @Test
    void rejectsWhenBrokerReturnsNonSuccess() {
        server.stubFor(get(urlPathEqualTo("/api/v1/market-data/search"))
                .willReturn(aResponse().withStatus(200).withBody("{\"InstrumentId\":100000}")));
        server.stubFor(post(urlPathMatching("/api/v1/trading/execution/demo/market-open-orders/by-amount"))
                .willReturn(aResponse().withStatus(429).withBody("{}")));

        EtoroConfig config = new EtoroConfig(server.baseUrl(), "api-key", "user-key", true,
                "/api/v1/trading/execution/demo/market-open-orders/by-amount", 5);

        EtoroBrokerAdapterPlugin plugin = new EtoroBrokerAdapterPlugin(config, HttpClient.newHttpClient(), new ObjectMapper());
        var result = plugin.submitOrders(new ExecutionRequest("corr", List.of(sampleProposal())));

        assertFalse(result.getFirst().accepted());
    }

    @Test
    void supportsInstrumentIdAliasAndSellOrders() {
        server.stubFor(get(urlPathEqualTo("/api/v1/market-data/search"))
                .willReturn(aResponse().withStatus(200).withBody("{\"Results\":[{\"InstrumentID\":200001}]}")));
        server.stubFor(post(urlPathMatching("/api/v1/trading/execution/demo/market-open-orders/by-amount"))
                .willReturn(aResponse().withStatus(200).withBody("{\"OrderId\":12345}")));

        EtoroConfig config = new EtoroConfig(server.baseUrl(), "api-key", "user-key", true,
                "/api/v1/trading/execution/demo/market-open-orders/by-amount", 5);

        EtoroBrokerAdapterPlugin plugin = new EtoroBrokerAdapterPlugin(config, HttpClient.newHttpClient(), new ObjectMapper());
        var result = plugin.submitOrders(new ExecutionRequest("corr", List.of(sampleProposal(TradeSide.SELL))));

        assertTrue(result.getFirst().accepted());
    }

    @Test
    void supportsItemsLookupAndNestedOrderWithCharge() {
        server.stubFor(get(urlPathEqualTo("/api/v1/market-data/search"))
                .willReturn(aResponse().withStatus(200).withBody("{\"items\":[{\"InstrumentId\":300001}]}")));
        server.stubFor(post(urlPathMatching("/api/v1/trading/execution/demo/market-open-orders/by-amount"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"orderForOpen\":{\"orderId\":\"ord-nested\",\"TotalExternalCosts\":1.25}}")));

        EtoroConfig config = new EtoroConfig(server.baseUrl(), "api-key", "user-key", true,
                "/api/v1/trading/execution/demo/market-open-orders/by-amount", 5);

        EtoroBrokerAdapterPlugin plugin = new EtoroBrokerAdapterPlugin(config, HttpClient.newHttpClient(), new ObjectMapper());
        var result = plugin.submitOrders(new ExecutionRequest("corr", List.of(sampleProposal())));

        assertTrue(result.getFirst().accepted());
        assertEquals("ord-nested", result.getFirst().brokerOrderId());
        assertEquals(new BigDecimal("1.25"), result.getFirst().externalCharge());
    }

    @Test
    void fallsBackOrderIdWhenResponseHasNoOrderId() {
        server.stubFor(get(urlPathEqualTo("/api/v1/market-data/search"))
                .willReturn(aResponse().withStatus(200).withBody("{\"InstrumentId\":100000}")));
        server.stubFor(post(urlPathMatching("/api/v1/trading/execution/demo/market-open-orders/by-amount"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));

        EtoroConfig config = new EtoroConfig(server.baseUrl(), "api-key", "user-key", true,
                "/api/v1/trading/execution/demo/market-open-orders/by-amount", 5);

        EtoroBrokerAdapterPlugin plugin = new EtoroBrokerAdapterPlugin(config, HttpClient.newHttpClient(), new ObjectMapper());
        var proposal = sampleProposal();
        var result = plugin.submitOrders(new ExecutionRequest("corr", List.of(proposal)));

        assertTrue(result.getFirst().accepted());
        assertEquals(proposal.clientOrderId(), result.getFirst().brokerOrderId());
        assertEquals(new BigDecimal("0"), result.getFirst().externalCharge());
    }

    @Test
    void rejectsWhenLookupBodyIsMalformedJson() {
        server.stubFor(get(urlPathEqualTo("/api/v1/market-data/search"))
                .willReturn(aResponse().withStatus(200).withBody("{")));

        EtoroConfig config = new EtoroConfig(server.baseUrl(), "api-key", "user-key", true,
                "/api/v1/trading/execution/demo/market-open-orders/by-amount", 5);

        EtoroBrokerAdapterPlugin plugin = new EtoroBrokerAdapterPlugin(config, HttpClient.newHttpClient(), new ObjectMapper());
        var result = plugin.submitOrders(new ExecutionRequest("corr", List.of(sampleProposal())));

        assertFalse(result.getFirst().accepted());
    }

    private TradeProposal sampleProposal() {
        return sampleProposal(TradeSide.BUY);
    }

    private TradeProposal sampleProposal(TradeSide side) {
        return new TradeProposal(
                "corr",
                "strategy",
                "BTC",
                side,
                BigDecimal.valueOf(100),
                null,
                "coid-1",
                Instant.now()
        );
    }
}
