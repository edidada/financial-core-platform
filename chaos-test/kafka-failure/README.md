# Kafka Broker 故障

- **验证**：消息可靠性；消息可恢复、可重放、**不重复入账**。
- **机制**：Outbox 表与账务同事务落库（ADR-0003），Broker 恢复后由 `OutboxRelay` 重投；
  消费侧 `MessageDeduplicator` 以 `message_key` 唯一约束去重。
- **不承诺**「恰好一次」投递，只承诺端到端幂等。