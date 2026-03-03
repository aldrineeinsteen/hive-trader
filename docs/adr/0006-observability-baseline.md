# ADR-0006: Prometheus-first observability

## Status
Accepted

## Context
Trading systems need transparent health and performance telemetry.

## Decision
Expose Actuator health + Prometheus, and emit custom counters/gauges for order flow, risk blocks, and safety gates.

## Consequences
- Better operational visibility.
- Simple integration with Grafana and alerting.
