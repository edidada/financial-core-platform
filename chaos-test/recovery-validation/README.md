# 恢复验证（综合）

灾备方案必须同时验证**基础设施恢复**与**业务账务恢复**（README 6.2 / AWS 与 DTCC 公开金融韧性资料）：

1. 在途交易状态重放：`OutboxRelay` 重投未发布事件。
2. 账务核对：`/v1/invariants` 全绿 + `/v1/platform/reconciliation` `clean=true`。
3. 记录 RTO/RPO 实测值、差异明细与改进措施，写入 `docs/11-incident-reviews/`。