#!/usr/bin/env bash
# 端到端：开户 -> 转账 -> 幂等重放 -> 余额校验 -> 冲正 -> 不变量 -> 对账
set -euo pipefail
BASE="${1:-http://localhost:8080}"
j() { python3 -c "import json,sys;print(json.load(sys.stdin)['data']$1)"; }
echo "[e2e] 开户"
A=$(curl -sf -X POST "$BASE/v1/accounts" -H 'Content-Type: application/json' \
  -d '{"tenantId":1,"accountNo":"E2E-A","currency":"CNY","initialBalanceMinor":10000}' | j "['accountId']")
B=$(curl -sf -X POST "$BASE/v1/accounts" -H 'Content-Type: application/json' \
  -d '{"tenantId":1,"accountNo":"E2E-B","currency":"CNY","initialBalanceMinor":0}' | j "['accountId']")
echo "  accounts: A=$A B=$B"
echo "[e2e] 转账 8000 分"
T=$(curl -sf -X POST "$BASE/v1/transactions" -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: e2e-1' \
  -d "{\"tenantId\":1,\"payerAccountId\":$A,\"payeeAccountId\":$B,\"amountMinor\":8000,\"currency\":\"CNY\"}" | j "['transactionId']")
echo "[e2e] 幂等重放"
T2=$(curl -sf -X POST "$BASE/v1/transactions" -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: e2e-1' \
  -d "{\"tenantId\":1,\"payerAccountId\":$A,\"payeeAccountId\":$B,\"amountMinor\":8000,\"currency\":\"CNY\"}" | j "['transactionId']")
[[ "$T" == "$T2" ]] || { echo "FAIL: idempotency broken"; exit 1; }
echo "[e2e] 余额校验"
curl -sf "$BASE/v1/accounts/$A" | grep -q '"availableBalanceMinor":2000' || { echo "FAIL: payer balance"; exit 1; }
curl -sf "$BASE/v1/accounts/$B" | grep -q '"availableBalanceMinor":8000' || { echo "FAIL: payee balance"; exit 1; }
echo "[e2e] 冲正"
curl -sf -X POST "$BASE/v1/transactions/$T/reverse?tenantId=1" \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: e2e-rev-1' -d '{}' >/dev/null
curl -sf "$BASE/v1/accounts/$A" | grep -q '"availableBalanceMinor":10000' || { echo "FAIL: reversal"; exit 1; }
echo "[e2e] 不变量 + 对账"
curl -sf "$BASE/v1/invariants" | grep -q '"healthy":true' || { echo "FAIL: invariants"; exit 1; }
curl -sf -X POST "$BASE/v1/platform/reconciliation" -H 'Content-Type: application/json' \
  -d '{"window":"e2e","left":[],"right":[]}' | grep -q '"clean":true' || { echo "FAIL: recon"; exit 1; }
echo "PASS: end-to-end scenario"