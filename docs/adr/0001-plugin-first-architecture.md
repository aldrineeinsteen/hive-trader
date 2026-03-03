# ADR-0001: Plugin-first architecture

## Status
Accepted

## Context
Trading rules change frequently and must not destabilize engine orchestration.

## Decision
All strategy, exploration, risk, broker, and data behavior are implemented through plugin interfaces in `plugin-api` and loaded by ServiceLoader.

## Consequences
- Core remains stable and minimal.
- New trading behavior ships as plugin modules.
- Requires strict SPI compatibility discipline.
