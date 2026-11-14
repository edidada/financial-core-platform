# 02 业务设计

| 域 | 文档 / 代码 | 关键约束 |
| --- | --- | --- |
| 交易 | `../transaction-design.md`、`services/transaction-service` | 状态机；UNKNOWN ≠ 失败 |
| 账户 | `services/account-service` | 条件 UPDATE，不超扣、不丢失更新 |
| 复式记账 | `../accounting-design.md`、`services/ledger-service` | ΣD=ΣC，冲正走反向分录 |
| 支付指令 | `services/payment-service`、`contracts/event-schemas/payment-instruction.json` | 支付指令与资金结算分离 |
| 清分清算 | `services/clearing-service` | 净额合计必须为 0、可重算 |
| 资金结算 | `services/settlement-service` | 与账务入账解耦 |
| 多方对账 | `services/reconciliation-service`、`engines/reconciliation-engine` | 哈希连接、差异可定位、结果可复现 |
| 退款冲正 | `services/refund-service` | 自身幂等，基于已入账交易 |
| 手续费 / 限额 / 风控 | `services/fee,limit,risk` | 整数运算、条件更新、确定延迟预算与降级 |
| 流动性 / 报表 | `services/liquidity,reporting` | 快照可追溯、日报借贷平衡 |