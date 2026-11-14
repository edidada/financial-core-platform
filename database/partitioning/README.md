# database/partitioning

阶段三「数据库分区」的评审脚本。`README.txt` 6.1 的「1 亿条账务记录查询 P99 < 200 ms」与
「1 亿条交易记录对账 1 小时内完成」是**实验目标**，必须结合硬件与数据分布调整，不能预设可达。

- MySQL 8.4：`RANGE (TO_DAYS(created_at))`，分区键必须包含在所有唯一键内。
- PostgreSQL：声明式 `PARTITION BY RANGE (created_at)`，可按月自动建分区。
- 冷热分离：热表 `ledger_entry` 保留近期数据，归档表 `ledger_entry_archive` 承接历史。

**前置条件**：分片/分区后跨分片转账需重新评估一致性协议（阶段五专题），不可直接套用 ADR-0003 的单库事务边界。