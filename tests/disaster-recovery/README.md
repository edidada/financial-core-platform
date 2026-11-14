# 容灾测试

与 `chaos-test/` 联动，覆盖 README 6.2 全部 7 类故障。验收：

- 数据库主节点故障后无已确认交易丢失；
- Kafka 故障恢复后消息可重放且不重复入账；
- 网络分区禁止双主写入；
- AZ 切换 RTO/RPO 达标；
- 每次演练后 `/v1/invariants` 必须 `healthy=true`。

结果写入 `docs/11-incident-reviews/`，含恢复时间线、根因与改进措施。