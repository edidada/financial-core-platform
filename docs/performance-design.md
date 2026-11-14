# 性能设计（阶段三）

> 对应 `README.txt` 第一节指标表与第三节「高性能能力」。详见 `05-performance/` 与 `benchmark/`。

## 一、性能预算

| 指标 | 一 | 二 | 三 | 本阶段口径 |
| --- | --- | --- | --- | --- |
| 交易 API 吞吐 | 500 TPS | 5,000 TPS | 20,000+ TPS | 每秒**成功完成的业务交易数** |
| 账务入账吞吐 | 300 TPS | 3,000 TPS | 10,000+ TPS | 不含查询 |
| 核心交易 P99 | < 200 ms | < 100 ms | < 50 ms | 稳定负载下 |
| 查询 API P99 | < 100 ms | < 50 ms | < 20 ms | |

必须区分：交易请求 TPS ≠ 数据库 SQL QPS；交易成功 ≠ 银行间最终结算；
不丢账 ≠ 消息恰好一次；高吞吐与强一致性存在成本权衡。

## 二、容量模型

每笔转账的写入放大：1 张 `journal` + 2 条 `ledger_entry` + 1 条 `idempotency_record`
+ 1 条 `outbox_event` + 1 条 `transaction`（含 3 次状态 CAS）+ 2 次 `account_balance` 更新。
按 5,000 TPS 估算约 4~5 万次写 SQL/s，单库单主需先验证磁盘与锁竞争上限。

## 三、瓶颈层次与优化顺序

1. **正确性基线**：先单库事务（`ADR-0003`），不引入分片/多活，确保可衡量每次升级收益。
2. **连接与事务**：Hikari 连接池、`READ_COMMITTED`、控制事务粒度、事务内禁止远程调用。
3. **锁竞争**：按 `account_id` 排序加锁防死锁；热点账户用 `engines/hotspot-accounting-engine` 净额折叠。
4. **索引与查询计划**：`database/indexes/`、`database/sql-benchmark/`，EXPLAIN 记录。
5. **分区与归档**：`database/partitioning/`，冷热分离。
6. **批处理**：`engines/batch-engine` 虚拟线程分块；`engines/reconciliation-engine` 哈希连接 O(n+m)。
7. **分库分表**：`infrastructure/database-sharding`；跨分片交易需重新评估一致性协议。

## 四、可观测与可复现

- `deploy/monitoring/`：Prometheus SLO 告警、Grafana 面板。
- `infrastructure/observability`：TraceId 全链路透传、业务指标计数。
- 每次测试记录：CPU 型号与核数、内存、磁盘、网络、JVM 参数、数据库版本、分片数、
  数据量、并发数、负载模型、错误率、P50/P95/P99/P99.9、GC、数据库等待事件。
- 结果写入 `performance-report.md`；优化前后对比 + 火焰图 + SQL 执行计划。

## 五、反模式

- 用 `double` 处理金额（ADR-0002 禁止）。
- 用 Redis 承担余额正确性（架构第一原则禁止）。
- 事务内发 Kafka / 调远程接口（拉长事务、放大锁持有时间）。
- 未测硬件就宣称达到 5,000 TPS；把一线公司整体交易量当作单系统要求。
