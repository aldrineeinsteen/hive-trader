package io.hivetrader.plugin.api.model;

import java.util.List;

public record ExecutionRequest(
        String correlationId,
        List<TradeProposal> proposals
) {
}
