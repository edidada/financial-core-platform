# Financial Core Platform — 设计文档

> **阶段一~五：完整实现（Fund Correctness First）**
> 定位：Java 21 高并发金融交易与复式记账平台。第一优先级是「资金正确性」，第二是「分布式一致性」，高性能与容灾为进阶能力。

本目录存放 **架构评审级别** 的设计文档。阶段一先用 **模块化单体 + 单库事务** 建立可证明的资金正确性基线，
阶段二~五逐阶段引入分布式组件、性能与大数据、高可用生产工程、专家级专题（见 `README.txt` 第九节实施路线）。
**五个阶段的代码与验证产物均已落地**，下文路线图与模块地图对应当前仓库状态。

## 一、文档清单（阶段一）

| 文档 | 主题 | 覆盖内容 |
| --- | --- | --- |
| [requirements.md](requirements.md) | 需求与验收 | 业务域、范围、功能/非功能需求、资金不变量、DoD |
| [architecture.md](architecture.md) | 总体架构 | C4、模块边界、依赖规则、分层、技术栈、部署 |
| [accounting-design.md](accounting-design.md) | 复式记账 | 科目体系、借贷规则、分录模型、入账、冲正、引擎接口 |
| [transaction-design.md](transaction-design.md) | 交易状态机 | 状态迁移、幂等、UNKNOWN/RECOVERING、超时恢复 |
| [database-design.md](database-design.md) | 数据模型 | ER、DDL、索引、约束、事务边界、隔离级别、Flyway、多数据库（MySQL 8 / PostgreSQL） |
| [consistency.md](consistency.md) | 一致性 | 不变量清单、并发扣款、原子提交、故障恢复、校验 SQL |
| [test-strategy.md](test-strategy.md) | 测试策略 | 并发/幂等/属性测试、不变量检查、验收矩阵 |
| [performance-design.md](performance-design.md) | 性能设计 | 性能预算、容量模型、瓶颈分析、优化顺序 |
| [performance-report.md](performance-report.md) | 性能报告 | 已实测正确性基线 + 压测矩阵模板 |
| [disaster-recovery.md](disaster-recovery.md) | 灾难恢复 | RTO/RPO、故障模型、恢复流程、账务恢复 |
| [security-design.md](security-design.md) | 安全设计 | 认证签名、密钥管理、审计、CI 门禁 |
| [incident-review.md](incident-review.md) | 故障复盘 | 复盘模板与已登记演练 |
| [00-requirements/](00-requirements/) … [11-incident-reviews/](11-incident-reviews/) | 分阶段文档体系 | 按 `README.txt` 第二节编号目录组织 |
| [adr/](adr/) | 架构决策 | ADR-0001~0009 |

## 二、阅读顺序

1. `requirements.md` —— 先明确「证明什么」。
2. `architecture.md` —— 再明确「边界在哪」。
3. `accounting-design.md` → `transaction-design.md` —— 核心领域模型。
4. `database-design.md` → `consistency.md` —— 落地与不变量。
5. `test-strategy.md` —— 如何用实验证明。
6. `adr/` —— 为什么这样取舍。

## 三、阶段路线图

| 阶段 | 主题 | 核心目标 | 实现状态 |
| --- | --- | --- | --- |
| 一 | 金融账务核心 | 账户、交易、复式记账、幂等、冲正、并发扣款；模块化单体 | 已实现，MySQL 8.4 实测通过 |
| 二 | 分布式金融交易 | Kafka、Outbox、状态机恢复、清算、对账、故障重试 | 已实现（Outbox/去重/锁/支付/清算/结算/对账/退款/手续费/限额/风控/流动性/报表） |
| 三 | 高性能与大数据 | 分区、批量、JMH/Gatling/k6/JMeter、5,000 TPS、性能报告 | 引擎与压测体系已实现；**TPS 数值待实测** |
| 四 | 高可用与生产工程 | K8s 多 AZ、监控告警、故障注入、恢复演练、CI/CD、安全审计 | 已实现（deploy/ + 5 工作流 + 7 类混沌 + 8 类测试） |
| 五 | 专家级专题 | 高并发账务引擎 | 已实现（热点净额引擎 + 属性测试 + 技术论文式报告） |

`docs/00~11` 编号目录已按 `README.txt` 第二节建立，与扁平文档交叉引用。
**阶段三的 5,000 TPS / 亿级数据仍是实验目标**，必须结合硬件与数据分布验证，不得预设可达。

## 四、贯穿全阶段的第一原则

- **权威数据源唯一**：账户余额与会计分录以关系数据库为唯一权威，Redis 只做非权威缓存，绝不参与资金正确性判定。
- **金额不用浮点**：以 `BIGINT` 存储最小货币单位（分），或明确精度的 `DECIMAL`，禁止 `double`。
- **账务不可变**：已生效分录不删除、不直接修改，冲正通过反向分录实现。
- **原子提交**：余额变更、会计分录、幂等记录、Outbox 事件处于同一本地事务边界。
- **正确先于性能**：先单库事务建立正确性基线，再引入分片与多活，每次升级都要能衡量收益与代价。
- **多数据库支持**：默认本机 MySQL 8，同时保留 PostgreSQL；方言差异收敛在 DDL 与配置，业务 SQL 保持可移植（见 [ADR-0006](adr/0006-multi-database-support.md)）。

## 五、文档规范

- 每个关键模块交付 HLD、LLD、ADR、Test Plan、Benchmark Report 五类产物。
- 所有性能结论必须记录硬件、数据量、并发模型、P50/P95/P99/P99.9、GC 与数据库等待事件。
- ADR 采用「背景 / 决策 / 备选方案 / 后果」四段式，编号递增、只追加不修改。

## 六、构建与运行（阶段一）

需求：JDK 21、Maven 3.9+、本机 MySQL 8（默认）或 PostgreSQL。

先创建空库 `fcp`（Flyway 只建表、不建库）：MySQL 用 `CREATE DATABASE fcp CHARACTER SET utf8mb4;`，PostgreSQL 用 `CREATE DATABASE fcp;`。

```bash
# 全量编译（JDK 21）
mvn -q clean package

# 默认：本机 MySQL 8
FCP_DB_USER=... FCP_DB_PASSWORD=... \
  java -jar app/financial-core-app/target/financial-core-app-*.jar

# 切换到 PostgreSQL
FCP_DB_USER=... FCP_DB_PASSWORD=... \
  java -jar app/financial-core-app/target/financial-core-app-*.jar --spring.profiles.active=postgresql
```

Maven 模块（依赖方向自上而下单向）：

```
financial-core-platform (pom)                 # 41 个 Maven 模块
├── common/            (4) domain / exception / utils / api
├── infrastructure/    (11) idempotency, outbox, id-generator, distributed-lock,
│                          transaction-outbox, message-consumer, rate-limiter,
│                          circuit-breaker, database-sharding, audit-log, observability
├── gateway/           (2) authentication, api-gateway
├── engines/           (7) accounting, posting, rule, batch,
│                          reconciliation, routing, hotspot-accounting
├── services/          (13) account, ledger, transaction, payment, clearing,
│                          settlement, reconciliation, refund, fee, limit,
│                          risk, liquidity, reporting
├── benchmark/jmh      (1) JMH 微基准
├── tests/platform-tests(1) 18 个纯逻辑单测
└── app/financial-core-app     # 启动类、REST、配置、双库 Flyway V1/V2
```

其他交付物：`contracts/`（OpenAPI/Protobuf/Avro/JSON Schema）、`database/`（分区/索引/种子/SQL 基准）、
`benchmark/`（k6/JMeter/Gatling/场景/报告）、`chaos-test/`（7 类故障）、`tests/`（8 类测试）、
`deploy/`（Docker/K8s/Helm/Terraform/Ansible/监控）、`.github/workflows/`（5 条流水线）、`docs/00~11` + `adr/`。

主要 REST 入口：核心 `/v1/accounts`、`/v1/transactions`、`/v1/invariants`；
阶段二~五 `/v1/platform/*`（支付/清算/对账/手续费/限额/风控/报表/流动性/Outbox/去重/锁/分片/网关/热点证明/指标）。

连接凭据一律通过环境变量注入，仓库内不含任何账号口令。服务端口默认 8080，若被占用用 `FCP_SERVER_PORT=18080` 覆盖（不改动配置文件）。
Kafka 未配置时 `OutboxRelay` 使用内存投递器，开箱即可跑；配置 `FCP_KAFKA_BOOTSTRAP` 后切换 Kafka。
