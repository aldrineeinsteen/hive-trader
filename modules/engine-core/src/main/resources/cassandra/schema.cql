CREATE KEYSPACE IF NOT EXISTS hive_trader
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE hive_trader;

CREATE TABLE IF NOT EXISTS universe_instruments (
    day_bucket text,
    symbol text,
    bucket text,
    metadata map<text, text>,
    ts timestamp,
    PRIMARY KEY ((day_bucket), symbol)
);

CREATE TABLE IF NOT EXISTS candles_by_instrument_day (
    symbol text,
    day_bucket text,
    ts timestamp,
    open decimal,
    high decimal,
    low decimal,
    close decimal,
    volume decimal,
    PRIMARY KEY ((symbol, day_bucket), ts)
) WITH CLUSTERING ORDER BY (ts DESC);

CREATE TABLE IF NOT EXISTS candidate_rankings_by_bucket_day (
    bucket text,
    day_bucket text,
    ts timestamp,
    symbol text,
    plugin_id text,
    score decimal,
    PRIMARY KEY ((bucket, day_bucket), ts, symbol)
) WITH CLUSTERING ORDER BY (ts DESC, symbol ASC);

CREATE TABLE IF NOT EXISTS signals_by_strategy_day (
    strategy_id text,
    day_bucket text,
    ts timestamp,
    symbol text,
    side text,
    confidence decimal,
    reason text,
    correlation_id text,
    PRIMARY KEY ((strategy_id, day_bucket), ts, symbol)
) WITH CLUSTERING ORDER BY (ts DESC, symbol ASC);

CREATE TABLE IF NOT EXISTS orders_by_day (
    day_bucket text,
    client_order_id text,
    ts timestamp,
    correlation_id text,
    symbol text,
    side text,
    qty decimal,
    status text,
    broker_order_id text,
    PRIMARY KEY ((day_bucket), client_order_id)
);

CREATE TABLE IF NOT EXISTS positions_by_instrument (
    symbol text PRIMARY KEY,
    quantity decimal,
    avg_price decimal,
    market_value decimal,
    unrealized_pnl decimal,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS portfolio_snapshots_by_day (
    day_bucket text,
    ts timestamp,
    equity decimal,
    cash decimal,
    drawdown_pct decimal,
    correlation_id text,
    PRIMARY KEY ((day_bucket), ts)
) WITH CLUSTERING ORDER BY (ts DESC);

CREATE TABLE IF NOT EXISTS control_flags (
    id text PRIMARY KEY,
    kill_switch boolean,
    paused boolean,
    pause_until timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS broker_charges_by_day (
    day_bucket text,
    ts timestamp,
    correlation_id text,
    client_order_id text,
    broker_order_id text,
    broker_id text,
    charge_amount decimal,
    charge_currency text,
    accepted boolean,
    PRIMARY KEY ((day_bucket), ts, client_order_id)
) WITH CLUSTERING ORDER BY (ts DESC, client_order_id ASC);

CREATE TABLE IF NOT EXISTS charge_summary_by_cycle_day (
    day_bucket text,
    correlation_id text,
    broker_id text,
    ts timestamp,
    total_charge_amount decimal,
    charge_currency text,
    order_count int,
    PRIMARY KEY ((day_bucket), correlation_id)
);
