# ADR-0003：单库事务作为资金写入的唯一原子边界

- 状态：Accepted
- 日期：2026-11-14
- 相关：`database-design.md` 第六节、`consistency.md` 第四节、`README.txt` 4.2

## 背景

`README.txt` 4.2 指出：余额变动必须与账务分录放在同一原子提交边界，或采用经过严格证明的其他一致性协议。候选做法包括「DB 分录 + Redis 余额」「先写余额再异步写分录」「跨服务 Saga」。

## 决策

一次用例 = 一个关系数据库本地事务（MySQL 8 / PostgreSQL 均可），事务内 **按固定顺序** 完成全部写入：

```
BEGIN
  1 INSERT idempotency_record
  2 INSERT/UPDATE transaction (状态机)
  3 UPDATE account_balance (条件更新 / 行锁)
  4 INSERT journal + ledger_entry x N
  5 INSERT outbox_event
COMMIT   -- 任一步失败整体 ROLLBACK
```

- 事务边界由 application 层 `@Transactional` 界定，domain 层不感知事务。
- **余额不进 Redis**；Redis 仅作查询缓存，且必须可降级、可丢弃。
- 事件通过 Outbox 表同事务落库，阶段二再异步投递，不在本事务内发 Kafka。
- 多账户转账按 `account_id` 排序加锁，避免死锁。

## 备选方案

| 方案 | 否决原因 |
| --- | --- |
| Redis 存余额 + DB 存分录 | 无跨存储事务，崩溃必留半状态，资金正确性无法证明 |
| 先提交余额、再异步补分录 | 中间态违反「余额 = 分录累计」(I-2)，恢复复杂 |
| 跨服务 Saga / TCC | 阶段一单进程单库，引入补偿逻辑纯属过度设计 |
| XA 分布式事务 | 无多数据源，不适用 |

## 后果

**正面**
- 不变量 I-1~I-6 直接由 DB 约束 + 单事务保证，可用 SQL 离线校验。
- 崩溃恢复简单：未提交回滚，已提交靠状态机补状态。
- 阶段二接 Kafka 时账务逻辑零改动（Outbox 已在事务内）。

**负面 / 代价**
- 单库成为写入吞吐上限（阶段一 300 TPS 入账足够）。
- 热点账户行锁竞争会退化吞吐（TC-3 记录基线，阶段三用排队/拆分优化）。
- 事务过长会占用连接，需控制事务粒度、禁止在事务内做远程调用。

## 验证

`consistency.md` 的原子提交边界 + I-2 校验 SQL；`test-strategy.md` TC-5（提交后/响应前崩溃）证明无半状态。
