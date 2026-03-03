# Phased Rollout Plan

## Phase 0 — Foundations
- Build multi-module project and SPI contracts.
- Add safe engine orchestration with observe-only default.
- Implement bounded Cassandra schema.

## Phase 1 — Internal Validation
- Run paper-trading cycles with built-in plugins.
- Validate metrics, logs, and control flags.
- Run CI `mvn -B verify` with coverage gates.

## Phase 2 — Broker Adapter Hardening
- Add concrete broker adapter module with retries, timeout, and idempotent keys.
- Add WireMock contract tests for 429/5xx/backoff scenarios.

## Phase 3 — Risk Expansion
- Add additional risk plugins (exposure bucket caps, volatility, correlation checks).
- Add pause/circuit-breaker automation and alerting.

## Phase 4 — Production k3s Rollout
- Deploy to k3s namespace with readiness/liveness probes.
- Integrate Prometheus Operator `ServiceMonitor` and Grafana dashboards.
- Enable live trading only after runbook approval.
