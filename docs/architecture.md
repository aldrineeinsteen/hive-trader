# Architecture

## Goals
- Plugin-first trading engine where all trading logic lives outside core.
- Safe by default: observe-only mode unless explicitly enabled.
- Deterministic, idempotent order execution path.
- ARM64 and k3s friendly deployment model.

## Modules
- `modules/plugin-api`: stable SPI and domain models.
- `modules/engine-core`: Spring Boot orchestration runtime and observability.
- `modules/etoro-adapter`: Broker adapter plugin for eToro API execution.
- `modules/strategy-trend`: moving-average crossover example strategy.
- `modules/exploration-momentum`: momentum candidate discovery.
- `modules/risk-basic`: baseline risk checks.

## DecisionCycle State Machine
1. Load config + control flags (kill switch/pause).
2. Fetch portfolio snapshot.
3. Run `ExplorationPlugin`s to rank instruments.
4. Run `StrategyPlugin`s to emit signals.
5. Convert signals to `TradeProposal` with deterministic `client_order_id`.
6. Run all `RiskEvaluatorPlugin`s.
7. Execute via `BrokerAdapterPlugin` only when:
   - `hive.trading.enabled=true`
   - kill switch is OFF
   - paused state is OFF
8. Emit metrics and structured logs.

## Storage
The Cassandra schema uses day-bucket partitioning (`YYYYMMDD`) for bounded partitions and efficient time-scoped reads.

## Monitoring
Actuator + Micrometer Prometheus endpoint:
- `/actuator/health`
- `/actuator/prometheus`

Custom engine metrics include order counters, signal counters, portfolio gauges, and runtime safety gates.

## Security and Safety
- Never log credentials or secure connect bundle contents.
- UTC timestamps only.
- Fail-safe behavior on uncertainty and plugin failures.
