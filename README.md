# hive-trader

Production-ready, open-source, plugin-based automated trading engine.

## Stack
- OpenJDK 21
- Spring Boot 3.3.x
- Maven multi-module
- Cassandra (local and Astra-ready config)
- Micrometer + Prometheus
- k3s-friendly manifests
- JaCoCo quality gates (line >= 70%, branch >= 60%)

## Project Layout
- `modules/plugin-api` — stable extension contracts and domain models
- `modules/engine-core` — orchestration runtime and observability
- `modules/etoro-adapter` — eToro broker adapter plugin
- `modules/strategy-trend` — moving-average crossover strategy plugin
- `modules/exploration-momentum` — momentum exploration plugin
- `modules/risk-basic` — baseline risk evaluation plugin
- `deploy/k8s` — namespace, deployment, service, servicemonitor
- `deploy/monitoring` — Prometheus scrape config
- `docs` — architecture, plugin guide, ADRs, rollout plan

## Plugin Architecture
All trading behavior is implemented through plugins discovered via Java `ServiceLoader`.
Core orchestration does not hard-code strategy, risk model, broker API, or news provider logic.

### Extension Points
- `StrategyPlugin`
- `ExplorationPlugin`
- `RiskEvaluatorPlugin`
- `BrokerAdapterPlugin`
- `DataProviderPlugin`
- `NewsProviderPlugin` (optional)
- `FeatureExtractorPlugin` (optional)

## DecisionCycle
1. Load config + control flags
2. Fetch portfolio state
3. Run exploration plugins
4. Run strategy plugins
5. Convert signals to idempotent trade proposals
6. Run all risk evaluators
7. Execute through broker adapter only when trading gates allow

## Safety Defaults
- Observe-only mode by default (`TRADING_ENABLED=false`)
- Kill switch and pause gates enforced
- Deterministic `client_order_id`
- UTC timestamps only

## Configuration
Defaults are in `modules/engine-core/src/main/resources/application.yml` and can be overridden via environment variables.

Examples:
- `TRADING_ENABLED=true`
- `TRADING_KILL_SWITCH=false`
- `STORAGE_MODE=astra`
- `ASTRA_SECURE_BUNDLE_PATH=/mnt/secrets/astra/secure-connect.zip`

eToro adapter environment variables (consumed by `etoro-adapter`):
- `ETORO_BASE_URL` (default `https://public-api.etoro.com`)
- `ETORO_API_KEY` (required)
- `ETORO_USER_KEY` (required)
- `ETORO_DEMO_MODE` (default `true`)
- `ETORO_OPEN_BY_AMOUNT_PATH` (optional, defaults to demo path)
- `ETORO_TIMEOUT_SECONDS` (default `15`)

For local testing, copy `.env.example` to `.env` and populate keys.

## Cassandra
Schema file:
- `modules/engine-core/src/main/resources/cassandra/schema.cql`

Bounded partitions by day bucket (`YYYYMMDD`) are used for event and signal tables.

Charge tracking storage:
- `broker_charges_by_day` stores per-order external costs from broker execution responses.
- `charge_summary_by_cycle_day` stores consolidated cycle-level charge totals.

To persist charge ledger events in Cassandra at runtime, set:
- `CASSANDRA_SESSION_ENABLED=true`

When disabled, the engine still consolidates charges in memory and emits structured logs/metrics.

## Monitoring
- `GET /actuator/health`
- `GET /actuator/prometheus`
- `GET /plugins`

Custom metrics:
- `orders_submitted_total`
- `orders_failed_total`
- `signals_generated_total{plugin}`
- `signals_blocked_risk_total{plugin}`
- `broker_external_charges_total{broker,currency}`
- `portfolio_equity`
- `portfolio_drawdown_pct`
- `trading_enabled`
- `kill_switch`

## Local Run
Start dependencies:

```bash
docker compose up -d cassandra prometheus grafana
```

Build and verify:

```bash
mvn -B verify
```

Run engine:

```bash
SPRING_PROFILES_ACTIVE=local mvn -pl modules/engine-core spring-boot:run
```

## Maven Release Versioning
This project is configured with:
- `maven-release-plugin` for tagged releases (`v<version>`)
- `versions-maven-plugin` for controlled version bumps

Prepare and perform a local release (no push by default):

```bash
mvn -B release:clean release:prepare -Darguments="-DskipTests"
mvn -B release:perform -Darguments="-DskipTests"
```

Set next snapshot explicitly:

```bash
mvn -B versions:set -DnewVersion=0.2.0-SNAPSHOT
mvn -B versions:commit
```

Update `scm` coordinates in [pom.xml](pom.xml) to your real repository before first release.

## Demo Trade Verification
For demo API trading, keep:
- `ETORO_DEMO_MODE=true`
- `ETORO_OPEN_BY_AMOUNT_PATH=/api/v1/trading/execution/demo/market-open-orders/by-amount`

Where to see demo trade results:
- eToro app/web in **Virtual Portfolio / Demo mode** under **Portfolio** and **History**.
- API response from demo order includes `orderForOpen.orderID` and `openDateTime` for traceability.

Helper script:

```bash
scripts/demo-trade.sh --dry-run
scripts/demo-trade.sh --symbol BTC --amount 100 --side BUY
```

The script prints a sanitized summary with `orderId`, `statusId`, and `openDateTime`.

## k3s
Apply manifests:

```bash
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/deployment.yaml
kubectl apply -f deploy/k8s/service.yaml
kubectl apply -f deploy/k8s/servicemonitor.yaml
```

## Test Stack
- JUnit 5 + Mockito
- WireMock
- Testcontainers (Cassandra)

## Notes
This repository intentionally keeps core behavior minimal and delegates all trading intelligence to plugin modules.