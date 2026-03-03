# Copilot Instructions — hive-trader
This repository is an open-source, plugin-based automated trading engine.
These instructions are mandatory for any code Copilot generates or edits.

---

## 0) Golden Rules (Safety & Money)
1. **Default mode is OBSERVE-ONLY.**
   - The engine MUST NOT place trades unless:
     - `trading.enabled=true`
     - kill switch flag is OFF
     - trading is not paused due to risk rules

2. **Idempotency is mandatory.**
   - Any order placement MUST use deterministic `client_order_id`.
   - Retries MUST NOT create duplicate orders.

3. **Fail safe, not fast.**
   - If uncertain, refuse to trade and record the reason.
   - If API errors occur, stop execution and switch to observe-only or pause mode.

4. **Never log secrets.**
   - Never print API keys, tokens, Astra bundle contents, or HTTP Authorization headers.
   - Treat request/response bodies as sensitive unless explicitly safe.

5. **All times are UTC.**
   - Store and compare timestamps in UTC only.

---

## 1) Architecture Constraints (Non-Negotiable)
### Plugin-first design
- **All trading behavior must live in plugins.**
- Core engine coordinates only. Core must remain stable and minimal.
- All plugin interfaces and models live in: `modules/plugin-api`.
- Core orchestration lives in: `modules/engine-core`.
- Broker implementations live in adapters: `modules/*-adapter` (e.g., `etoro-adapter`).

### Stable SPI contract
- Treat `modules/plugin-api` as a public API.
- Keep it backward compatible:
  - Adding new methods must use default methods or new interfaces.
  - Avoid breaking changes (renaming methods/types, changing semantics).
- Any SPI change MUST include:
  - ADR update (docs/adr)
  - PLUGIN_DEVELOPMENT.md update
  - SemVer consideration (major version bump if breaking).

### DecisionCycle as a state machine
- Execution must follow the pipeline:
  1) Load config + `control_flags` (kill switch, pause)
  2) Read portfolio/positions + snapshots
  3) Run ExplorationPlugins → CandidateRankings
  4) Run StrategyPlugins → Signals
  5) Convert Signals → TradeProposals (engine-core)
  6) Run ALL RiskEvaluatorPlugins (any BLOCK blocks)
  7) Execute via BrokerAdapterPlugin (only if enabled)
  8) Persist audit and emit metrics

### Data boundaries
- Engine-core MUST NOT scrape websites.
- News ingestion must use structured feeds (RSS/official APIs) and be implemented as a `NewsProviderPlugin`.
- LLM usage is allowed only as **feature extraction** (annotations) and must be OFF by default.

---

## 2) Plugin Best Practices
### ServiceLoader requirements
- Each plugin module MUST register via `META-INF/services/<FQCN>`.
- Each plugin MUST provide `PluginInfo` with:
  - `id` (stable unique)
  - `name`
  - `version`
  - `capabilities`
- Engine must expose `/plugins` endpoint listing loaded plugins and versions.

### Plugin purity and determinism
- Plugin logic should be deterministic given inputs.
- Avoid hidden state inside plugins.
- If state is required, persist it via engine storage abstractions (not in-memory only).

### Versioning
- Plugins should declare compatible engine API version range if implemented.
- Avoid runtime reflection hacks.

### Failure handling
- Plugin failures must not crash the engine.
- Engine-core must catch plugin exceptions and:
  - mark plugin execution as failed
  - emit a metric `plugin_errors_total{plugin_id}`
  - continue in observe-only or skip plugin based on severity policy.

---

## 3) Coding Standards & Practices
### Java style
- Use Java 21 features appropriately.
- Use **records** for DTOs and immutable models.
- Avoid Lombok unless necessary; keep OSS friction low.
- Prefer composition over inheritance.

### Configuration
- Use `@ConfigurationProperties` and validation annotations.
- Config precedence:
  1) `application.yml` defaults
  2) ENV overrides
  3) mounted files (e.g., Astra secure connect bundle path)
- **Fail fast** on invalid config that could cause unsafe behavior.

### Correlation IDs
- Every DecisionCycle gets a `correlationId`.
- Propagate correlationId through:
  - logs
  - outbound eToro calls
  - persistence audit rows (where appropriate)

### Logging
- Use structured JSON logs.
- Log levels:
  - INFO for lifecycle events (cycle start/end, mode changes, order accepted)
  - WARN for recoverable issues (rate limiting, retry)
  - ERROR for failed order, plugin crash, persistence failure
- Never log full broker payloads containing sensitive details unless redacted.

### Error handling
- Use domain exceptions:
  - `BrokerApiException`, `RiskBlockedException`, `PluginExecutionException`, etc.
- Never swallow exceptions silently. Always record + metric.

---

## 4) Risk & Execution Discipline (Best Practices)
### Minimum risk rules (must exist in engine)
Even if risk plugins exist, engine-core must enforce baseline:
- trading.enabled gate
- kill switch gate
- pause gate
- idempotency gate
- max trades/day gate

### Risk plugin composition
- All RiskEvaluatorPlugins are run.
- Decision aggregation:
  - any `BLOCK` ⇒ block
  - else if any `ALLOW_WITH_LIMITS` ⇒ apply strictest limits
  - else `ALLOW`

### Circuit breakers / pause logic
- Track consecutive failures and/or daily drawdown.
- If breached:
  - set `control_flags` pause for 24h
  - emit metrics and logs
  - do not trade until pause expires

### Execution best practices
- Always write an audit event BEFORE and AFTER placing an order.
- Use backoff and retry for 429/5xx.
- Keep HTTP timeouts explicit.
- Use deterministic `client_order_id` and store it in `orders_by_day`.

---

## 5) Cassandra Best Practices (Pi-friendly, bounded partitions)
- Use bounded partitions with day_bucket = YYYYMMDD.
- Avoid “infinite partitions” (never append forever to one partition).
- Use UTC timestamps, clustering by ts.
- Prefer “append-only event tables” for audit trails.
- Provide efficient read paths for:
  - latest position snapshot
  - latest portfolio snapshot
  - today’s orders/signals

### Local vs Astra
- Local Cassandra is for dev and high-volume backtests.
- Astra is for low-volume always-on event storage.
- Ensure `storage.mode` switches cleanly.

---

## 6) k3s/Kubernetes Friendliness (Best Practices)
- Provide readiness + liveness probes:
  - readiness checks Cassandra connectivity and plugin load success
  - liveness avoids flapping (do not fail liveness due to a temporary broker error)
- Support graceful shutdown:
  - stop scheduling new cycles
  - complete in-flight cycle
  - flush audits
- Keep resource usage conservative (ARM64):
  - set memory limits, avoid excessive threads
- All secrets must be injected via Kubernetes Secret (env or mounted files).
- ConfigMaps must contain non-sensitive config only.

---

## 7) Monitoring Requirements (Prometheus-first)
### Actuator
- Must expose:
  - `/actuator/health`
  - `/actuator/prometheus`

### Required metrics (minimum set)
- Engine:
  - `decision_cycles_total{result}`
  - `decision_cycle_duration_seconds`
  - `observe_only` gauge
  - `trading_enabled` gauge
  - `kill_switch` gauge
  - `paused` gauge
- Plugins:
  - `plugin_invocations_total{plugin_id,type}`
  - `plugin_errors_total{plugin_id,type}`
- Broker:
  - `broker_requests_total{status}`
  - `broker_latency_seconds`
- Trading:
  - `orders_submitted_total`
  - `orders_failed_total`
  - `orders_filled_total`
  - `signals_generated_total{plugin_id}`
  - `signals_blocked_risk_total{plugin_id}`
- Portfolio:
  - `portfolio_equity`
  - `portfolio_drawdown_pct`
  - `bucket_exposure_pct{bucket}`

### Alerting hooks
- Provide example Prometheus alert rules or at least recommended alerts in docs:
  - kill switch ON unexpectedly
  - paused state active
  - broker error rate spike
  - no decision cycles for X minutes
  - Cassandra connectivity failures

---

## 8) Testing & Coverage (Strict)
### Coverage gates
- The build MUST fail if:
  - line coverage < 70%
  - branch coverage < 60%
- Use JaCoCo at parent + module aggregation.

### Test pyramid
- Unit tests:
  - risk aggregation logic
  - proposal sizing logic
  - idempotency key generation
  - config validation
- Contract tests:
  - Broker adapter via WireMock (timeouts, retries, 429 handling)
- Integration tests:
  - Cassandra DAO via Testcontainers

### Test quality rules
- No “assert true” tests.
- Every non-trivial branch must be exercised.
- Use deterministic clocks (inject Clock) to avoid flaky tests.

---

## 9) Security & OSS Hygiene
- No secrets in repo. Ever.
- Provide secret templates and `.env.example` only.
- Add dependency scanning compatibility (standard Maven).
- Pin critical dependency versions in parent pom when needed.
- Document threat model briefly in `docs/architecture.md`.

---

## 10) Documentation Discipline
Every feature PR should include:
- README updates if it changes how to run/configure
- architecture.md updates if it changes the system flow
- PLUGIN_DEVELOPMENT.md updates if it affects plugin authors
- ADR if it changes core architectural choices

---

## 11) How Copilot should behave
When generating code:
- Prefer minimal, safe defaults.
- Prefer clarity over cleverness.
- Provide tests alongside implementations.
- Never implement speculative endpoints or assumptions about the broker API without a clear contract in the adapter layer.
- When unsure, create a stub with TODO and a failing test demonstrating expected behavior (but do not break the build).

End of instructions.