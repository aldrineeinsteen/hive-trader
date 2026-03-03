# ADR-0005: Risk aggregation policy

## Status
Accepted

## Context
Multiple risk plugins may return conflicting outcomes.

## Decision
Aggregate using:
- any `BLOCK` => block execution
- else if any `ALLOW_WITH_LIMITS` => allow with limits
- else `ALLOW`

## Consequences
- Conservative, safety-first behavior.
- Enables composable risk controls.
