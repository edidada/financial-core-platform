#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[verify] Redis 全挂不得影响资金正确性（Redis 只做非权威缓存）"
curl -sf "$BASE/v1/invariants" | tee /tmp/inv-redis.json
grep -q '"healthy":true' /tmp/inv-redis.json || { echo "FAIL"; exit 1; }
echo "PASS: 缓存降级不影响权威账务数据"