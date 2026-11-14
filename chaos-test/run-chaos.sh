#!/usr/bin/env bash
# 混沌实验统一入口（需要 kubectl / chaos-mesh 与目标集群）
set -euo pipefail
SCENARIO="${1:-}"
BASE="http://localhost:8080"
if [[ -z "$SCENARIO" ]]; then
  echo "usage: $0 <network-failure|database-failover|kafka-failure|redis-failure|pod-kill|region-failover|recovery-validation>"
  exit 1
fi
DIR="$(cd "$(dirname "$0")/$SCENARIO" && pwd)"
echo "[chaos] scenario=$SCENARIO"

# 1) 注入故障
kubectl apply -f "$DIR/experiment.yaml"

# 2) 观测窗口：持续打流量并采集指标
echo "[chaos] observing 120s ..."
for i in $(seq 1 24); do
  curl -sf -X POST "$BASE/v1/transactions" \
    -H 'Content-Type: application/json' -H "Idempotency-Key: chaos-$SCENARIO-$i" \
    -d '{"tenantId":1,"payerAccountId":10001,"payeeAccountId":10002,"amountMinor":100,"currency":"CNY"}' || true
  sleep 5
done

# 3) 删除故障
kubectl delete -f "$DIR/experiment.yaml" --ignore-not-found

# 4) 恢复校验：账务不变量必须全绿
"$DIR/verify.sh" "$BASE"