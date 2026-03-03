# ADR-0003: ServiceLoader-based plugin discovery

## Status
Accepted

## Context
Need zero-touch runtime plugin discovery with standard Java tooling.

## Decision
Use `java.util.ServiceLoader` and `META-INF/services` registration files.

## Consequences
- No reflection hacks.
- Plugins remain loosely coupled to engine runtime.
