#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[verify] Kafka 恢复后 Outbox 可重放且消费侧去重生效"
curl -sf -X POST "$BASE/v1/platform/outbox/relay" | tee /tmp/relay.json
curl -sf "$BASE/v1/invariants" | tee /tmp/inv-kafka.json
grep -q '"healthy":true' /tmp/inv-kafka.json || { echo "FAIL"; exit 1; }
echo "PASS: 消息可恢复、可重放、不重复入账"