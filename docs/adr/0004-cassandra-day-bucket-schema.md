# ADR-0004: Cassandra day-bucket schema

## Status
Accepted

## Context
Unbounded partitions degrade Cassandra performance.

## Decision
Use bounded partitions with `day_bucket = YYYYMMDD` and clustering by timestamp.

## Consequences
- Predictable partition growth.
- Efficient reads for today's events/snapshots.
