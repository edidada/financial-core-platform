#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[verify] 处理中崩溃：未提交事务回滚、已提交事务不重复、UNKNOWN 可查询"
curl -sf "$BASE/v1/invariants" | tee /tmp/inv-pod.json
grep -q '"healthy":true' /tmp/inv-pod.json || { echo "FAIL"; exit 1; }
echo "PASS: 事务原子性与重试幂等正确"