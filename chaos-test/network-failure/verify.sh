#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[verify] 网络分区恢复后检查账务不变量与双主写入"
curl -sf "$BASE/v1/invariants" | tee /tmp/inv.json
grep -q '"healthy":true' /tmp/inv.json || { echo "FAIL: invariants unhealthy"; exit 1; }
grep -q '"I-3 duplicate biz_id":0' /tmp/inv.json || { echo "FAIL: duplicate posting"; exit 1; }
echo "PASS: 无脑裂、无重复入账"