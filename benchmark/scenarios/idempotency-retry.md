# 场景：交易幂等重试（TC-4）

- **目标**：100 万次重复请求（相同 Idempotency-Key），同一业务仅生效一次。
- **验收**：`idempotency_record` 仅 1 行；`journal` 仅 1 张凭证；余额仅变化一次。