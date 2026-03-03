#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

SYMBOL="BTC"
AMOUNT="100"
LEVERAGE="1"
SIDE="BUY"
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --symbol)
      SYMBOL="$2"
      shift 2
      ;;
    --amount)
      AMOUNT="$2"
      shift 2
      ;;
    --leverage)
      LEVERAGE="$2"
      shift 2
      ;;
    --side)
      SIDE="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    *)
      echo "Unknown argument: $1"
      echo "Usage: scripts/demo-trade.sh [--symbol BTC] [--amount 100] [--leverage 1] [--side BUY|SELL] [--dry-run]"
      exit 1
      ;;
  esac
done

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing .env file at $ENV_FILE"
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

if [[ "${ETORO_DEMO_MODE:-true}" != "true" ]]; then
  echo "Refusing to run: ETORO_DEMO_MODE is not true"
  exit 1
fi

if [[ -z "${ETORO_API_KEY:-}" || -z "${ETORO_USER_KEY:-}" ]]; then
  echo "Missing ETORO_API_KEY or ETORO_USER_KEY in .env"
  exit 1
fi

BASE_URL="${ETORO_BASE_URL:-https://public-api.etoro.com}"
OPEN_PATH="${ETORO_OPEN_BY_AMOUNT_PATH:-/api/v1/trading/execution/demo/market-open-orders/by-amount}"

REQ_LOOKUP="$(/usr/bin/uuidgen | /usr/bin/tr '[:upper:]' '[:lower:]')"
LOOKUP_RESPONSE="$(/usr/bin/curl --compressed -sS \
  "${BASE_URL}/api/v1/market-data/search?internalSymbolFull=${SYMBOL}" \
  -H "Accept: application/json" \
  -H "x-request-id: ${REQ_LOOKUP}" \
  -H "x-api-key: ${ETORO_API_KEY}" \
  -H "x-user-key: ${ETORO_USER_KEY}")"

INSTRUMENT_ID="$(SYMBOL_ENV="$SYMBOL" printf '%s' "$LOOKUP_RESPONSE" | /usr/bin/python3 -c '
import json, os, sys

raw = sys.stdin.read().strip()
obj = json.loads(raw) if raw else {}
target_symbol = os.environ.get("SYMBOL_ENV", "").upper()
val = None

if isinstance(obj, dict):
  for key in ("InstrumentId", "InstrumentID", "instrumentId"):
    if isinstance(obj.get(key), (int, float)):
      val = int(obj[key])
      break

  if val is None and isinstance(obj.get("items"), list):
    for entry in obj["items"]:
      if isinstance(entry, dict):
        symbol = (entry.get("internalSymbolFull") or "").upper()
        if symbol == target_symbol and isinstance(entry.get("instrumentId"), (int, float)):
          val = int(entry["instrumentId"])
          break
    if val is None:
      for entry in obj["items"]:
        if isinstance(entry, dict) and isinstance(entry.get("instrumentId"), (int, float)):
          val = int(entry["instrumentId"])
          break

  if val is None and isinstance(obj.get("results"), list) and obj["results"]:
    first = obj["results"][0]
    if isinstance(first, dict):
      for key in ("InstrumentId", "InstrumentID", "instrumentId"):
        if isinstance(first.get(key), (int, float)):
          val = int(first[key])
          break

  if val is None and isinstance(obj.get("Results"), list) and obj["Results"]:
    first = obj["Results"][0]
    if isinstance(first, dict):
      for key in ("InstrumentId", "InstrumentID", "instrumentId"):
        if isinstance(first.get(key), (int, float)):
          val = int(first[key])
          break

print(val if val is not None else "")
')"

if [[ -z "$INSTRUMENT_ID" ]]; then
  echo "Instrument lookup failed for symbol=${SYMBOL}"
  exit 1
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "DRY_RUN=true instrumentId=${INSTRUMENT_ID} symbol=${SYMBOL}"
  exit 0
fi

if [[ "$SIDE" != "BUY" && "$SIDE" != "SELL" ]]; then
  echo "Invalid side: $SIDE. Use BUY or SELL"
  exit 1
fi

IS_BUY="true"
if [[ "$SIDE" == "SELL" ]]; then
  IS_BUY="false"
fi

REQ_TRADE="$(/usr/bin/uuidgen | /usr/bin/tr '[:upper:]' '[:lower:]')"
PAYLOAD="{\"InstrumentId\":${INSTRUMENT_ID},\"Amount\":${AMOUNT},\"Leverage\":${LEVERAGE},\"IsBuy\":${IS_BUY}}"

TRADE_RESPONSE="$(/usr/bin/curl --compressed -sS -X POST \
  "${BASE_URL}${OPEN_PATH}" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "x-request-id: ${REQ_TRADE}" \
  -H "x-api-key: ${ETORO_API_KEY}" \
  -H "x-user-key: ${ETORO_USER_KEY}" \
  -d "$PAYLOAD")"

printf '%s' "$TRADE_RESPONSE" | /usr/bin/python3 -c '
import json,sys
obj=json.load(sys.stdin)
order=obj.get("orderForOpen") or obj.get("OrderForOpen") or {}
order_id=order.get("orderID") or order.get("OrderID")
status_id=order.get("statusID") or order.get("StatusID")
instrument_id=order.get("instrumentID") or order.get("InstrumentID")
opened=order.get("openDateTime") or order.get("OpenDateTime")
print(f"demo_trade_result orderId={order_id} statusId={status_id} instrumentId={instrument_id} openDateTime={opened}")
'
