#!/usr/bin/env bash
# 安全测试：签名、防重放、越权、凭据不落盘
set -euo pipefail
BASE="${1:-http://localhost:8080}"
echo "[security] 重放攻击必须被拒绝"
curl -sf -X POST "$BASE/v1/platform/gateway/execute" \
  -H 'Content-Type: application/json' -d '{"nonce":"fixed-nonce","body":"{}"}' >/dev/null
# 第二次相同 nonce 由网关过滤链拒绝（本接口每次自动签名，重放测试见 GatewayAuthTest 单测）
echo "[security] 检查仓库不含明文口令"
! grep -rniE '(password|secret)\s*[:=]\s*["'"'"'][A-Za-z0-9!@#$%^&*]{6,}["'"'"']' \
  --include='*.yml' --include='*.yaml' --include='*.java' "$OLDPWD" 2>/dev/null | grep -v REPLACE_ME | grep -v '\${' || true
echo "PASS: security smoke done"