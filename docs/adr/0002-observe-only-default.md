# ADR-0002: Observe-only default

## Status
Accepted

## Context
Unsafe defaults can cause unintended live trading.

## Decision
Engine starts in observe-only mode and only executes orders when explicit gates are enabled.

## Consequences
- Safer boot behavior.
- Requires explicit operations control for live trading.
