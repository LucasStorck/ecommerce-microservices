#!/usr/bin/env bash
# Runtime check of atomic stock reservation (feat/order-flow, feat/price-lookup). Run inside WSL with the
# infra up (docker compose up -d) and discovery-server, api-gateway, inventory-service
# and order-service running. Uses its own SKUs (VERIFY-*), so the seed data is untouched.
# Run: bash scripts/verify-reservation.sh
set -u

GW=${GW:-http://localhost:8080}
FAILS=0

pass() { echo "  PASS: $1"; }
fail() { echo "  FAIL: $1"; FAILS=$((FAILS + 1)); }

# check <description> <expected> <actual>
check() { [ "$2" = "$3" ] && pass "$1 ($3)" || fail "$1 (expected $2, got $3)"; }

status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
qty() { curl -s "$GW/api/inventory/$1" | sed -E 's/.*"quantity":([0-9]+).*/\1/'; }
set_stock() {
  curl -s -o /dev/null -X PUT "$GW/api/inventory/$1" -H 'Content-Type: application/json' -d "{\"quantity\":$2}"
}
create_sku() {
  curl -s -o /dev/null -X POST "$GW/api/inventory" -H 'Content-Type: application/json' \
    -d "{\"skuCode\":\"$1\",\"quantity\":$2}"
  set_stock "$1" "$2" # reset when the SKU already exists from a previous run
  # Orders are priced from the catalog, so the SKU must exist there too (once; reruns reuse it).
  if [ "$(curl -s "$GW/api/products?skuCode=$1")" = "[]" ]; then
    curl -s -o /dev/null -X POST "$GW/api/products" -H 'Content-Type: application/json'       -d "{\"skuCode\":\"$1\",\"name\":\"$1\",\"description\":\"verify\",\"price\":10.00}"
  fi
}
order() { # order <json-items>
  curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/api/orders" \
    -H 'Content-Type: application/json' -d "{\"items\":[$1]}"
}
item() { echo "{\"skuCode\":\"$1\",\"quantity\":$2}"; }

echo "== 0. Services reachable through the gateway"
check "GET /api/inventory/SKU-00001" 200 "$(status "$GW/api/inventory/SKU-00001")"

create_sku VERIFY-A 10
create_sku VERIFY-B 5

echo "== 1. Happy path: order of 3 takes 3 from stock of 10"
check "POST /api/orders" 201 "$(order "$(item VERIFY-A 3)")"
check "quantity after" 7 "$(qty VERIFY-A)"

echo "== 2. Insufficient stock: 409 and stock untouched"
check "POST /api/orders (qty 100 > 7)" 409 "$(order "$(item VERIFY-A 100)")"
check "quantity after" 7 "$(qty VERIFY-A)"

echo "== 3. All-or-nothing: A is enough but B is short, so A must not be decremented"
check "POST /api/orders (A x2 + B x99)" 409 "$(order "$(item VERIFY-A 2),$(item VERIFY-B 99)")"
check "VERIFY-A after (rolled back)" 7 "$(qty VERIFY-A)"
check "VERIFY-B after" 5 "$(qty VERIFY-B)"

echo "== 4. Same SKU twice in one order is summed (4 + 4 = 8 > 7)"
check "POST /api/orders" 409 "$(order "$(item VERIFY-A 4),$(item VERIFY-A 4)")"
check "quantity after" 7 "$(qty VERIFY-A)"

echo "== 5. Direct reserve/release on inventory-service"
check "reserve 2" 204 "$(status -X POST "$GW/api/inventory/reserve" -H 'Content-Type: application/json' \
  -d '{"items":[{"skuCode":"VERIFY-A","quantity":2}]}')"
check "quantity after reserve" 5 "$(qty VERIFY-A)"
check "release 2" 204 "$(status -X POST "$GW/api/inventory/release" -H 'Content-Type: application/json' \
  -d '{"items":[{"skuCode":"VERIFY-A","quantity":2}]}')"
check "quantity after release" 7 "$(qty VERIFY-A)"

echo "== 6. Concurrency: 50 orders of 1 on stock of 20 (exactly 20 must win)"
create_sku VERIFY-C 20
CODES=$(mktemp)
for _ in $(seq 1 50); do
  order "$(item VERIFY-C 1)" >> "$CODES" & # no newline from -w, so codes are concatenated
done
wait
OK=$(grep -o '201' "$CODES" | wc -l)
CONFLICT=$(grep -o '409' "$CODES" | wc -l)
rm -f "$CODES"
check "orders accepted (201)" 20 "$OK"
check "orders rejected (409)" 30 "$CONFLICT"
check "final quantity (never below 0)" 0 "$(qty VERIFY-C)"

echo "== 7. Price comes from the catalog (feat/price-lookup)"
create_sku VERIFY-D 10
BODY=$(curl -s -X POST "$GW/api/orders" -H 'Content-Type: application/json'   -d '{"items":[{"skuCode":"VERIFY-D","price":0.01,"quantity":2}]}')
ORDER_ID=$(echo "$BODY" | sed -E 's/^\{"id":"([^"]+)".*/\1/')
check "client-sent price ignored (total = 2 x 10.00)" 20.0 "$(echo "$BODY" | sed -E 's/.*"total":([0-9.]+).*/\1/')"
check "unknown sku -> 404" 404 "$(order "$(item NO-SUCH-SKU 1)")"

echo "== 8. Status transitions"
check "confirm PENDING" 200 "$(status -X PATCH "$GW/api/orders/$ORDER_ID/confirm")"
check "cancel CONFIRMED -> 409" 409 "$(status -X PATCH "$GW/api/orders/$ORDER_ID/cancel")"
check "confirm unknown order -> 404" 404 "$(status -X PATCH "$GW/api/orders/no-such-id/confirm")"
create_sku VERIFY-E 10
CANCEL_ID=$(curl -s -X POST "$GW/api/orders" -H 'Content-Type: application/json'   -d "{\"items\":[$(item VERIFY-E 4)]}" | sed -E 's/^\{"id":"([^"]+)".*/\1/')
check "stock after order" 6 "$(qty VERIFY-E)"
check "cancel PENDING" 200 "$(status -X PATCH "$GW/api/orders/$CANCEL_ID/cancel")"
check "stock released by cancel" 10 "$(qty VERIFY-E)"
check "second cancel -> 409 (no double release)" 409 "$(status -X PATCH "$GW/api/orders/$CANCEL_ID/cancel")"
check "stock unchanged after second cancel" 10 "$(qty VERIFY-E)"

echo
if [ "$FAILS" -eq 0 ]; then echo "ALL CHECKS PASSED"; else echo "$FAILS CHECK(S) FAILED"; exit 1; fi

cat <<'EOF'

Manual follow-ups (not scripted):
  - Compensation: stop PostgreSQL of order-service (docker stop <order-db container>), place an
    order for a VERIFY-* SKU. Expect a 5xx and the stock restored by the release call
    (check order-service log for the "release" path, and GET /api/inventory/<sku>).
  - Fail closed: stop inventory-service, POST /api/orders -> 503; same with product-service stopped.
  - Kafka: after the happy path, GET /api/notifications shows the orders from step 1.
EOF
