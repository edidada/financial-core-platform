# ADR-0007：Outbox 至少一次投递 + 消费侧幂等，不追求端到端「恰好一次」

- 状态：Accepted
- 日期：2026-11-14
- 相关：ADR-0003、ADR-0005、`04-consistency/`、`disaster-recovery.md`

## 背景

阶段二引入 Kafka 后，需要决定跨服务一致性协议。候选：XA/TCC/Saga、直接发消息、
事务消息（Outbox）。同时 `README.txt` 明确「系统不丢账不代表所有异步消息都只投递一次」。

## 决策

采用**本地事务 + Outbox 表 + 至少一次投递 + 消费侧幂等**：

1. `outbox_event` 与余额/分录/幂等记录在**同一本地事务**写入（ADR-0003 不变）。
2. `OutboxRelay` 轮询未发布事件，同步发送并等待 broker ack 后才标记 `published=TRUE`。
3. 消费侧 `MessageDeduplicator` 用 `consumed_message(message_key)` 唯一约束去重；
   失败按 `RetryPolicy` 指数退避，超限进死信队列。
4. 不采用 XA / TCC 强跨库事务，也不承诺「恰好一次」投递。
5. Kafka 未配置时默认 `InMemoryEventPublisher`，保证开箱可跑与可测试。

## 备选方案

| 方案 | 否决原因 |
| --- | --- |
| 事务内直接发 Kafka | 网络超时导致状态不确定；消息发了事务回滚会产生幽灵事件 |
| XA / TCC | 复杂度与故障模式远超阶段二需求，且资金正确性不因此更强 |
| 追求恰好一次投递 | 分布式系统不存在端到端恰好一次，必须消费侧幂等 |
| Saga 补偿 | 账务系统补偿语义难以证明，阶段一正确性基线会失稳 |

## 后果

**正面**：账务与事件原子落库；Broker 故障可重放；重复投递被消费侧唯一约束挡住；
`chaos-test/kafka-failure/` 可自动化验证。

**负面**：`published` 标记在 ack 之后，崩溃可能重复投递（由去重兜底）；
Outbox 表需要归档；消费侧必须实现幂等，不能假设只收到一次。