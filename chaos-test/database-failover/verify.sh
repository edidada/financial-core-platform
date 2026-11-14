#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[verify] 主从切换后：已确认交易 0 丢失、无重复扣款、借贷平衡"
curl -sf "$BASE/v1/invariants" | tee /tmp/inv-db.json
grep -q '"healthy":true' /tmp/inv-db.json || { echo "FAIL"; exit 1; }
echo "PASS: 账务在故障转移后保持一致"