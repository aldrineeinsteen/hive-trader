package io.hivetrader.plugin.api.model;

import java.util.Set;

public record PluginInfo(
        String id,
        String name,
        String version,
        Set<String> capabilities
) {
}
