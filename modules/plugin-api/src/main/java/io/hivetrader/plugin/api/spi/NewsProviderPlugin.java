package io.hivetrader.plugin.api.spi;

import io.hivetrader.plugin.api.model.NewsItem;

import java.util.List;

public interface NewsProviderPlugin extends TradingPlugin {
    List<NewsItem> latestNews(List<String> symbols);
}
