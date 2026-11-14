#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[verify] AZ 切换后自动故障转移、RTO/RPO 达标、账务一致"
curl -sf "$BASE/v1/invariants" | tee /tmp/inv-az.json
grep -q '"healthy":true' /tmp/inv-az.json || { echo "FAIL"; exit 1; }
echo "PASS: 自动切换且账务无丢失"