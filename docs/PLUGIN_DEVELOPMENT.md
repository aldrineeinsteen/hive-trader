# Plugin Development Guide

## Overview
All trading behavior must be implemented as plugins in external modules that depend on `plugin-api`.

## Mandatory Contract
Every plugin implements one of:
- `StrategyPlugin`
- `ExplorationPlugin`
- `RiskEvaluatorPlugin`
- `BrokerAdapterPlugin`
- `DataProviderPlugin`
- Optional: `NewsProviderPlugin`, `FeatureExtractorPlugin`

And must provide:
- `PluginInfo` via `info()` with stable `id`, `name`, `version`, and `capabilities`.

## ServiceLoader Registration
Create resource file in plugin module:
- `src/main/resources/META-INF/services/<interface-fqcn>`

Example for strategy:
- `META-INF/services/io.hivetrader.plugin.api.spi.StrategyPlugin`

File content:
- `com.example.plugins.MyStrategyPlugin`

## Safety Rules
- Plugin logic should be deterministic for same inputs.
- Avoid hidden mutable state.
- Fail with clear exceptions; do not swallow errors.
- Never log credentials or sensitive broker payloads.

## Compatibility
`plugin-api` is treated as stable SPI. Avoid breaking changes to existing interfaces; prefer additive contracts.
