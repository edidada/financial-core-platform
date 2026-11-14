# ADR-0006：以本机 MySQL 8 为默认、保留 PostgreSQL 的多数据库支持

- 状态：Accepted
- 日期：2026-11-14
- 相关：`database-design.md` 第十节、`architecture.md` 第十一节、ADR-0003

## 背景

阶段一原设计以 PostgreSQL 作为唯一权威账务库。实际本地环境是 **MySQL 8**（监听 3306），用户要求把默认库改成本机 MySQL 8，但**不能丢掉 PostgreSQL 支持**——即同一套代码要能跑在两种数据库上。

需要回答：如何让业务代码不感知数据库差异，同时把差异限制在尽可能小的范围内。若处理不当，会出现两套 SQL、两套迁移、两套并发语义，最终「保留 PG 支持」沦为一句空话。

## 决策

**默认 `mysql`（本机 MySQL 8），可选 `postgresql`；业务代码与事务语义完全共用，方言差异只落在 DDL 与配置。**

具体约束：

1. **切换靠 Spring profile**：`spring.profiles.active=mysql`（默认）/ `postgresql`。连接地址、账号、口令全部走环境变量，不入库。
2. **Flyway 按厂商拆分迁移**：`db/migration/mysql/V1__init.sql` 与 `db/migration/postgresql/V1__init.sql`，由 profile 指定 `spring.flyway.locations`（亦可用 Flyway `{vendor}` 占位符）。
3. **持久层用 JdbcTemplate + 可移植 SQL**：不用 ORM 的方言特性、不用数据库特有 upsert。并发控制只用两库都支持的「条件 `UPDATE` + `SELECT ... FOR UPDATE`」。
4. **主键与时间戳由应用层写入**：雪花 ID、`java.sql.Timestamp`；禁用 `AUTO_INCREMENT` / `BIGSERIAL`、`now()` / `ON UPDATE CURRENT_TIMESTAMP` 等方言默认值。
5. **事务隔离显式统一为 `READ_COMMITTED`**：MySQL 默认 `REPEATABLE READ`、PG 默认 `READ COMMITTED`，必须覆盖，否则条件更新与锁行为在两库不一致（ADR-0003 的单事务边界语义也依赖这一点）。
6. **重复键统一由 Spring `DuplicateKeyException` 处理**，屏蔽 SQLState 差异。
7. **CI 双库验证**：Testcontainers 同时跑 MySQL 8 与 PostgreSQL，同一套集成测试与不变量校验（TC-9）。

## 备选方案

| 方案 | 优点 | 否决原因 |
| --- | --- | --- |
| 只改成本机 MySQL 8，删掉 PG | 最简单 | 违背「保留 PG 支持」要求 |
| 用 JPA/Hibernate 屏蔽方言 | 少写 SQL | 条件更新、行锁排序、INSERT-only 账务表等核心不变量被 ORM 隐式行为掩盖，不利于证明资金正确性 |
| 一套 DDL 用最保守类型硬跑两库 | 只维护一份脚本 | MySQL 的 `TIMESTAMP` 时区/2038 问题、`ENGINE=InnoDB`、JSON 类型差异无法两全 |
| 每个库一套业务 SQL | 可极致调优 | 代码分叉，正确性难以同步验证，阶段一不可接受 |
| 引入 ShardingSphere / 多数据源抽象层 | 功能强 | 阶段一单库单事务，属于过度设计（阶段三专题） |

## 后果

**正面**
- 本地可直接用已运行的 MySQL 8 开发验收；切换 profile 即可在 PostgreSQL 上复现同一套结论。
- 资金正确性的关键手段（条件更新、行锁、单事务边界）在两库语义一致，不变量 I-1~I-6 可用同一套 SQL 校验。
- 阶段三做分片时，多库经验可直接复用。

**负面 / 代价**
- 必须维护两份迁移脚本；新增表/字段需同步更新两份并双库验证。
- 放弃部分数据库特有优化（如 PG 的 `ON CONFLICT`、MySQL 的 `ON UPDATE CURRENT_TIMESTAMP`）。
- MySQL 需 >= 8.0.16 才能保证 CHECK 约束真正生效；低于该版本只能靠应用层兜底。
- 显式隔离级别意味着放弃 MySQL 默认 RR 的某些一致性保证，必须在测试中用并发用例覆盖（TC-1）。
- 本机 MySQL 为 8.4 LTS，而 Flyway 9.x（Spring Boot 3.2.x 默认）报 `Unsupported Database: MySQL 8.4`；因此采用 Spring Boot 3.3.x（管理 Flyway 10.10.0），并显式引入 `flyway-mysql` 与 `flyway-database-postgresql`。后续升级框架须同步核对 Flyway 对各数据库版本的支持范围。

## 验证

`test-strategy.md` TC-9：TC-1~TC-8 在 `mysql` 与 `postgresql` 两个 profile 下结果完全一致；`consistency.md` 的 I-1~I-6 校验 SQL 在两库均返回 0 行异常。
