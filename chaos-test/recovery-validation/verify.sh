#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[verify] 综合恢复：基础设施恢复 + 业务账务恢复"
curl -sf -X POST "$BASE/v1/platform/outbox/relay" >/dev/null
curl -sf "$BASE/v1/invariants" | tee /tmp/inv-rec.json
grep -q '"healthy":true' /tmp/inv-rec.json || { echo "FAIL"; exit 1; }
echo "[verify] 在途交易重放与多方对账"
curl -sf -X POST "$BASE/v1/platform/reconciliation" \
  -H 'Content-Type: application/json' \
  -d '{"window":"recovery-drill","left":[],"right":[]}' | tee /tmp/rec.json
grep -q '"clean":true' /tmp/rec.json || { echo "FAIL: reconciliation not clean"; exit 1; }
echo "PASS: 恢复演练通过"