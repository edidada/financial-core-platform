#!/usr/bin/env bash
# 契约测试：用 OpenAPI / JSON Schema 校验请求与响应
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[contract] 校验 /v1/invariants 响应结构"
curl -sf "$BASE/v1/invariants" | python3 -c '
import json,sys
d=json.load(sys.stdin)
assert d["code"]==0, d
assert isinstance(d["data"]["healthy"], bool), d
assert "I-1 unbalanced journals" in d["data"]["violations"], d
print("PASS: invariants response matches contract")'
echo "[contract] 校验转账请求必带 Idempotency-Key"
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/v1/transactions" \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":1,"payerAccountId":1,"payeeAccountId":2,"amountMinor":100,"currency":"CNY"}')
[[ "$code" == "400" || "$code" == "4"* ]] || { echo "FAIL: expected 4xx without key, got $code"; exit 1; }
echo "PASS: idempotency header enforced (HTTP $code)"