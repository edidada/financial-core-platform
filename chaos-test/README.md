# chaos-test（阶段四 混沌工程）

对应 `README.txt` 6.2 矩阵。每个目录含 `experiment.yaml`（故障定义）、`verify.sh`（恢复校验）、`README.md`（验收标准）。

| 目录 | 故障 | 验证重点 | 验收 |
| --- | --- | --- | --- |
| `network-failure/` | 网络分区 | 故障隔离 | 禁止双主写入、防脑裂 |
| `database-failover/` | 数据库主节点故障 | 数据正确性 | 故障转移后无已确认交易丢失 |
| `kafka-failure/` | Kafka Broker 故障 | 消息可靠性 | 可恢复、可重放、不重复入账 |
| `redis-failure/` | Redis 故障 | 非权威缓存 | 缓存降级不影响资金正确性 |
| `pod-kill/` | 服务处理中崩溃 | 事务一致性 | 事务原子性与重试幂等正确 |
| `region-failover/` | 整个可用区不可用 | 高可用 | 自动切换、RTO/RPO 达标 |
| `recovery-validation/` | 综合恢复 | 账务恢复 | 在途交易重放与核对 |

**原则**：灾备方案必须同时验证「基础设施恢复」与「业务账务恢复」。
每次演练后必须跑 `/v1/invariants`，`healthy=true` 才算通过。

统一入口：`./chaos-test/run-chaos.sh <scenario>`。