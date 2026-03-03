package io.hivetrader.adapters.etoro;

public record EtoroConfig(
        String baseUrl,
        String apiKey,
        String userKey,
        boolean demoMode,
        String openByAmountPath,
        int timeoutSeconds
) {
    public static EtoroConfig fromEnvironment() {
        String baseUrl = getenvOrDefault("ETORO_BASE_URL", "https://public-api.etoro.com");
        String apiKey = getenvOrDefault("ETORO_API_KEY", "");
        String userKey = getenvOrDefault("ETORO_USER_KEY", "");
        boolean demoMode = Boolean.parseBoolean(getenvOrDefault("ETORO_DEMO_MODE", "true"));
        String openByAmountPath = getenvOrDefault(
                "ETORO_OPEN_BY_AMOUNT_PATH",
                demoMode
                        ? "/api/v1/trading/execution/demo/market-open-orders/by-amount"
                        : "/api/v1/trading/execution/market-open-orders/by-amount"
        );
        int timeoutSeconds = Integer.parseInt(getenvOrDefault("ETORO_TIMEOUT_SECONDS", "15"));
        return new EtoroConfig(baseUrl, apiKey, userKey, demoMode, openByAmountPath, timeoutSeconds);
    }

    private static String getenvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public boolean hasCredentials() {
        return apiKey != null && !apiKey.isBlank() && userKey != null && !userKey.isBlank();
    }
}
