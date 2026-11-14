# 03 数据库设计

- `../database-design.md`：ER、可移植 DDL、索引、事务边界、隔离级别、双库方言差异。
- 实际 DDL：`app/financial-core-app/src/main/resources/db/migration/{mysql,postgresql}/V1,V2`。
- `../../database/`：分区、索引、种子数据、SQL 基准脚本。

要点：金额 `BIGINT` 分；主键/时间戳应用层写入；隔离级别显式 `READ_COMMITTED`；
账务表只追加；`opening_balance` 作为 I-2 校验基线。