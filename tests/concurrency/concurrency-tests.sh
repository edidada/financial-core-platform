#!/usr/bin/env bash
# TC-1 单账户并发扣款：1000 并发，账户 10000 分，每笔 8000 分，只允许 1 笔成功
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[concurrency] 1000 concurrent debits"
seq 1 1000 | xargs -P 50 -I{} curl -sf -o /dev/null -X POST "$BASE/v1/transactions" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: conc-{}" \
  -d '{"tenantId":1,"payerAccountId":10001,"payeeAccountId":10002,"amountMinor":8000,"currency":"CNY"}' || true
curl -sf "$BASE/v1/accounts/10001" | tee /tmp/acc.json
echo "[concurrency] 断言：余额恰为 2000 分且无超扣"
grep -q '"availableBalanceMinor":2000' /tmp/acc.json || { echo "FAIL: overdraft or lost update"; exit 1; }
curl -sf "$BASE/v1/invariants" | grep -q '"healthy":true' || { echo "FAIL: invariants"; exit 1; }
echo "PASS: no overdraft, no duplicate posting"