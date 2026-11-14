# README
一个工业级金融交易与账务平台
├── transaction-service/   # 交易服务
├── account-service/       # 账户服务
├── ledger-service/        # 复式记账
├── clearing-service/      # 清算服务
├── reconciliation/        # 对账服务
├── risk-engine/           # 风控引擎
├── common/                # 公共组件
├── benchmark/             # 压测
├── chaos-test/            # 故障注入
└── docs/
    ├── architecture.md
    ├── database-design.md
    ├── consistency.md
    ├── disaster-recovery.md
    └── performance-report.md  扩展下，对表一线公司技术指标

# **Financial Core Platform：对标一线金融科技公司的工业级金融核心系统**

建议将 `financial-core-platform` 从简单的金融微服务项目，升级为一个**面向银行、支付机构、证券及金融科技公司的高并发、高一致性、高可用金融交易核心平台**。

技术上可以参考蚂蚁集团、腾讯金融科技、Stripe、Adyen、Visa 等公司的公开架构与工程实践，但不应直接把这些公司的整体交易量当作单个系统的性能要求。

项目应该重点证明五种能力：

1. **金融业务能力**：交易、账户、复式记账、清算、对账、资金结算。
2. **分布式系统能力**：幂等、事务一致性、故障恢复、消息可靠性。
3. **高性能能力**：高并发交易处理、数据库分片、缓存、批处理、性能调优。
4. **高可用能力**：多可用区部署、自动故障转移、灾难恢复、混沌工程。
5. **工程交付能力**：架构设计、详细设计、测试报告、SLO、可观测性、安全审计。

## **一、建议采用的技术指标**

下面是一套适用于自研 Java 金融核心平台的**分阶段工程验收目标**，不是对上述企业内部生产指标的推断。

| 指标            | 第一阶段 MVP | 第二阶段生产级目标 | 第三阶段高性能挑战    |
| ------------- | -------- | --------- | ------------ |
| 交易 API 吞吐量    | 500 TPS  | 5,000 TPS | 20,000+ TPS  |
| 账务入账吞吐量       | 300 TPS  | 3,000 TPS | 10,000+ TPS  |
| 核心交易 P99 延迟   | < 200 ms | < 100 ms  | < 50 ms      |
| 查询 API P99 延迟 | < 100 ms | < 50 ms   | < 20 ms      |
| 核心服务月可用性目标    | 99.9%    | 99.95%    | 99.99%       |
| 同城故障恢复 RTO    | < 30 min | < 5 min   | < 60 s       |
| 灾难恢复 RPO      | ≤ 5 min  | ≤ 30 s    | 关键账务零已确认数据丢失 |
| 已确认账务记录丢失     | 0        | 0         | 0            |
| 重复请求导致重复扣款    | 0        | 0         | 0            |
| 不平衡会计分录       | 0        | 0         | 0            |

TPS 为每秒成功完成的业务交易数；P99 为 99% 请求的响应时间上界。上述性能目标必须在固定硬件、真实业务混合、指定数据规模和稳定负载下验证。RPO=0 需要与同步复制、故障确认协议和持久化边界配套设计。

需要特别区分：

- 交易请求 TPS 不等于数据库 SQL QPS。
- 交易成功不等于资金已经完成银行间最终结算。
- 系统不丢账不代表所有异步消息都只投递一次。
- 高吞吐与强一致性存在成本权衡，不能同时无限制追求。

## **二、扩展后的完整 GitHub 工程目录**

建议采用 **Java 21 + Spring Boot + PostgreSQL/MySQL + Kafka + Redis + Kubernetes**，核心账务模块坚持数据库事务与会计约束，不依赖 Redis 保证资金正确性。

```
financial-core-platform/
│
├── README.md
├── pom.xml
├── Makefile
├── docker-compose.yml
│
├── gateway/                     # API 网关
│   ├── api-gateway/
│   └── authentication/
│
├── services/                    # 核心金融业务
│   ├── transaction-service/     # 交易生命周期
│   ├── account-service/         # 账户与余额
│   ├── ledger-service/          # 复式记账总账
│   ├── payment-service/         # 支付指令
│   ├── clearing-service/        # 清分清算
│   ├── settlement-service/      # 资金结算
│   ├── reconciliation-service/  # 多方对账
│   ├── refund-service/          # 退款、冲正
│   ├── fee-service/             # 手续费计算
│   ├── limit-service/           # 交易限额
│   ├── risk-engine/             # 实时风控
│   ├── liquidity-service/       # 流动性管理
│   └── reporting-service/       # 财务及监管报表
│
├── engines/                     # 高性能核心引擎
│   ├── accounting-engine/       # 会计分录引擎
│   ├── posting-engine/          # 账务入账引擎
│   ├── rule-engine/             # 规则执行引擎
│   ├── batch-engine/            # 批处理引擎
│   ├── reconciliation-engine/   # 差异匹配引擎
│   └── routing-engine/          # 交易路由引擎
│
├── infrastructure/              # 基础技术组件
│   ├── id-generator/            # 全局唯一 ID
│   ├── idempotency/             # 幂等控制
│   ├── distributed-lock/        # 分布式锁
│   ├── transaction-outbox/      # 事务消息
│   ├── message-consumer/        # 消息去重、重试
│   ├── database-sharding/       # 数据库分片
│   ├── rate-limiter/            # 限流
│   ├── circuit-breaker/         # 熔断降级
│   ├── audit-log/               # 审计日志
│   └── observability/           # 监控追踪
│
├── common/
│   ├── common-api/
│   ├── common-domain/
│   ├── common-exception/
│   ├── common-security/
│   ├── common-test/
│   └── common-utils/
│
├── contracts/                   # 服务契约
│   ├── openapi/
│   ├── protobuf/
│   ├── avro/
│   └── event-schemas/
│
├── database/
│   ├── migrations/              # Flyway 数据库迁移
│   ├── schemas/
│   ├── indexes/
│   ├── partitioning/
│   ├── seed-data/
│   └── sql-benchmark/
│
├── benchmark/
│   ├── gatling/
│   ├── k6/
│   ├── jmeter/
│   ├── datasets/
│   ├── scenarios/
│   └── reports/
│
├── chaos-test/
│   ├── network-failure/
│   ├── database-failover/
│   ├── kafka-failure/
│   ├── redis-failure/
│   ├── pod-kill/
│   ├── region-failover/
│   └── recovery-validation/
│
├── tests/
│   ├── integration/
│   ├── contract/
│   ├── concurrency/
│   ├── consistency/
│   ├── property-based/
│   ├── security/
│   ├── disaster-recovery/
│   └── end-to-end/
│
├── deploy/
│   ├── docker/
│   ├── kubernetes/
│   ├── helm/
│   ├── terraform/
│   ├── ansible/
│   └── monitoring/
│
├── .github/
│   └── workflows/
│       ├── build.yml
│       ├── test.yml
│       ├── security-scan.yml
│       ├── benchmark.yml
│       └── release.yml
│
└── docs/
    ├── 00-requirements/
    ├── 01-architecture/
    ├── 02-business-design/
    ├── 03-database-design/
    ├── 04-consistency/
    ├── 05-performance/
    ├── 06-high-availability/
    ├── 07-security/
    ├── 08-testing/
    ├── 09-operations/
    ├── 10-capacity-planning/
    ├── 11-incident-reviews/
    └── adr/                      # 架构决策记录
```

这里最重要的变化，是将**业务服务、核心计算引擎、基础设施组件、测试验证和技术文档**分开。

`engines/` 不一定需要独立部署。例如 `posting-engine` 初期可以作为 `ledger-service` 内部的 Java 模块，以避免不必要的跨服务事务。

## **三、金融核心系统总体架构**

**渠道接入层**

Web / App / OpenAPI / 银行及支付渠道

**API Gateway / IAM / 限流 / 防重放**

认证、授权、请求签名、全局 Trace ID

交易中心

Transaction

账户中心

Account

风控中心

Risk

支付中心

Payment

清算结算

Clearing

对账中心

Reconciliation

**核心账务层**

Ledger Service + Posting Engine

复式记账 / 余额约束 / 原子提交 / 冲正

关系数据库

权威账务数据

Kafka

可靠事件传递

Redis

非权威缓存

Prometheus / Grafana / OpenTelemetry / Audit

架构上有一个关键原则：**账户余额和会计分录必须具有明确的权威数据源。**

交易、账务、清算、结算不应混为一谈。外部渠道的支付结果可能延迟或不确定，因此不能把跨机构支付简单设计成一次分布式数据库事务。

## **四、最值得深入研究的六个核心模块**

| 模块             | 核心技术难题      | 工业级验收要求           |
| -------------- | ----------- | ----------------- |
| Transaction    | 状态机、幂等、超时恢复 | 不出现非法状态跳转，超时可查询   |
| Account        | 并发扣款、余额约束   | 不超扣、不丢失更新         |
| Ledger         | 复式记账、原子入账   | 每笔分录借贷平衡，账务不可随意修改 |
| Clearing       | 批量净额计算、精度   | 清分结果可重算、可追溯       |
| Reconciliation | 大规模数据匹配     | 不漏单、差异可定位、结果可复现   |
| Risk           | 实时规则、特征计算   | 有确定的延迟预算与降级策略     |

### **4.1 Ledger Service：整个项目的技术核心**

例如 A 向 B 转账 100 元：

| 账户        | 借方 Debit | 贷方 Credit |
| --------- | -------- | --------- |
| A 的资金负债账户 | ¥100     | —         |
| B 的资金负债账户 | —        | ¥100      |
| **合计**    | **¥100** | **¥100**  |

此示例采用平台负债账户视角，借记 A 的负债表示减少，贷记 B 的负债表示增加。

必须满足：

\\[ \sum Debit = \sum Credit \\]

但仅仅借贷平衡还不够，还需要验证：

- 同币种、同账套、同会计规则下平衡。
- 同一业务交易不会重复入账。
- 账务记录可追溯，不直接删除已生效分录。
- 冲正通过反向分录实现。
- 并发交易下余额约束仍成立。
- 原子提交同时涵盖余额更新、会计分录和 Outbox 事件记录。

建议使用 `BIGINT` 存储最小货币单位，或使用明确精度的 `DECIMAL`，不要使用 `double` 处理货币。

### **4.2 Account Service：并发资金安全**

典型挑战：账户只有 100 元，两个请求同时扣款 80 元。

不正确的读后写逻辑可能导致两笔交易都成功。

可以使用数据库条件更新：

```
UPDATE account_balance
SET available_balance = available_balance - 8000,
    version = version + 1
WHERE account_id = 10001
  AND available_balance >= 8000;
```

检查影响行数，只有一笔能够成功扣款。

实际生产设计还需要把这次余额变动和账务分录放在**同一原子提交边界**，或者采用经过严格证明的其他一致性协议。

### **4.3 Transaction Service：交易状态机**

```
CREATED
   |
   v
VALIDATED
   |
   v
PROCESSING
   |
   +------> FAILED
   |
   +------> UNKNOWN
   |           |
   |           v
   |       RECOVERING
   |
   v
SUCCEEDED
   |
   v
REVERSED (有条件冲正)
```

必须区分：

- 请求超时：不知道交易是否成功。
- 业务失败：明确未完成交易。
- 已入账：核心账务已提交。
- 外部结算完成：资金完成约定的结算流程。

这是金融核心系统与普通 CRUD 系统的重要区别。

## **五、对标一线公司的工程能力成熟度**

一线公司的工程实践更值得学习的是**技术体系和验证方法**，而不只是 TPS 数字。

| 参考公司/体系    | 值得研究的方向          | 项目落地内容              |
| ---------- | ---------------- | ------------------- |
| Stripe     | API 幂等、支付状态、异常恢复 | 幂等键、状态查询、Webhook 去重 |
| Adyen      | 支付生命周期、清结算、对账    | 支付指令与资金结算分离         |
| 蚂蚁集团       | 分布式交易、高可用、金融账务   | 事务一致性、容灾、账务引擎       |
| 腾讯金融科技     | 海量交易、弹性扩容、系统稳定性  | 分库分表、限流、容量规划        |
| Google SRE | SLO、错误预算、故障复盘    | 服务等级目标、监控、事故演练      |
| AWS 金融行业架构 | 故障隔离、业务连续性、灾难恢复  | 多 AZ、RTO/RPO、恢复演练   |

Google SRE 公开资料强调用 SLO 和错误预算指导工程决策；AWS 金融行业架构则强调根据业务关键性制定不同的恢复目标，而不是要求所有模块都达到同样的可用性等级。

[image](https://www.google.com/s2/favicons?domain=https://sre.google\&sz=32)

Error Budget Policy for Service Reliability+1



## **六、必须具备的压测与故障验证体系**

仅仅使用 JMeter 测试 HTTP TPS 不足以证明金融系统的可靠性。

### **6.1 性能测试矩阵**

| 测试场景         | 验收目标            | 主要验证        |
| ------------ | --------------- | ----------- |
| 单账户并发扣款      | 1,000 并发请求      | 不超扣、不重复入账   |
| 多账户随机转账      | 5,000 TPS       | 账务正确性与 P99  |
| 热点账户转账       | 1,000 TPS 目标    | 锁竞争、吞吐退化    |
| 交易幂等重试       | 100 万次重复请求      | 同一业务仅生效一次   |
| Kafka 消息重复投递 | 100 万条          | 消费去重、无重复入账  |
| 1 亿条账务记录查询   | P99 < 200 ms 目标 | 索引、分区、查询计划  |
| 1 亿条交易记录对账   | 1 小时内完成目标       | 批处理、并行计算    |
| 持续稳定性测试      | 72 小时           | 内存、连接、延迟稳定性 |
| 突发流量测试       | 基线 3 倍          | 限流、排队、恢复能力  |

这些是建议的实验目标，尤其是热点账户 TPS 和十亿级数据处理，必须结合硬件与数据分布调整，不能预设一定能达到。

每次测试应记录：CPU 型号与核数、内存、磁盘、网络、JVM 参数、数据库版本、分片数、数据量、并发数、负载模型、错误率、P50/P95/P99/P99.9、GC 和数据库等待事件。

### **6.2 混沌工程验证**

**数据库主节点故障**

数据正确性

故障转移后，无已确认交易丢失

**Kafka Broker 故障**

消息可靠性

消息可恢复、可重放、不重复入账

**服务处理过程中崩溃**

事务一致性

事务原子性与重试幂等正确

**网络分区**

故障隔离

禁止双主写入，防止脑裂

**整个可用区不可用**

高可用

验证自动切换和 RTO/RPO

**错误配置或版本发布**

工程交付

验证灰度、回滚和审计记录

灾备方案应同时验证基础设施恢复和业务账务恢复。AWS 与 DTCC 的公开金融韧性资料也强调跨区域复制、恢复自动化，以及对在途交易状态进行重放和核对。

[image](https://www.google.com/s2/favicons?domain=https://pages.awscloud.com\&sz=32)

DTCC+1



## **七、技术选型建议**

| 层次   | 建议技术                     | 说明           |
| ---- | ------------------------ | ------------ |
| 语言   | Java 21                  | LTS，适合金融后台系统 |
| 应用框架 | Spring Boot              | 企业级开发生态      |
| 服务通信 | REST + gRPC              | 外部接口与内部通信分离  |
| 数据库  | PostgreSQL 或 MySQL       | 事务、约束、可靠持久化  |
| 缓存   | Redis                    | 查询缓存、非权威状态   |
| 消息队列 | Kafka                    | 可靠事件流、异步解耦   |
| 数据迁移 | Flyway                   | Schema 版本管理  |
| 流处理  | Flink                    | 实时风控、流水统计    |
| 批处理  | Spark                    | 大规模历史数据对账    |
| 容器平台 | Kubernetes               | 弹性部署、故障恢复    |
| 监控   | Prometheus + Grafana     | 指标、SLO       |
| 链路追踪 | OpenTelemetry            | 分布式 Trace    |
| 测试   | JUnit 5 + Testcontainers | 自动化集成测试      |
| 性能测试 | Gatling + JMH            | 系统压测、微基准     |

初期不建议一次性引入 Flink、Spark、分布式数据库和多活架构。先通过单库事务建立正确性基线，再引入 Kafka、分片和多可用区部署，才能清楚地衡量每次架构升级的收益与代价。

## **八、文档体系要达到架构评审级别**

建议 `docs/` 不仅有概要设计，还必须有详细设计、验收依据和实验数据。

| 文档                      | 必须覆盖的内容              |
| ----------------------- | -------------------- |
| `requirements.md`       | 业务需求、非功能需求、验收条件      |
| `architecture.md`       | C4 架构图、部署拓扑、技术决策     |
| `transaction-design.md` | 交易状态机、异常状态、重试策略      |
| `accounting-design.md`  | 会计科目、借贷规则、冲正流程       |
| `database-design.md`    | ER 图、DDL、索引、事务边界     |
| `consistency.md`        | 幂等、隔离级别、Outbox、故障恢复  |
| `performance-design.md` | 性能预算、容量模型、瓶颈分析       |
| `disaster-recovery.md`  | RTO/RPO、故障模型、恢复流程    |
| `security-design.md`    | 身份认证、加密、密钥管理、审计      |
| `test-strategy.md`      | 单测、集成、并发、属性测试        |
| `performance-report.md` | 实测 TPS、延迟、CPU、GC、SQL |
| `incident-review.md`    | 故障复盘、根因分析、改进措施       |

推荐每个关键模块同时提供：

- HLD（High-Level Design，概要设计）
- LLD（Low-Level Design，详细设计）
- ADR（Architecture Decision Record，架构决策记录）
- Test Plan（测试计划）
- Benchmark Report（性能报告）

## **九、建议的实施路线**

1. **阶段一：金融账务核心（第 1–2 个月）**

   完成账户、交易、复式记账、数据库事务、幂等、冲正及并发扣款测试。先构建单体模块化系统，证明资金正确性。
2. **阶段二：分布式金融交易（第 3–4 个月）**

   拆分必要的服务，加入 Kafka、Outbox、状态机恢复、清算、对账和故障重试。重点验证跨服务一致性。
3. **阶段三：高性能与大数据（第 5–6 个月）**

   引入大规模测试数据、数据库分区、批量计算、JMH/Gatling、执行计划分析，完成 5,000 TPS 挑战及性能报告。
4. **阶段四：高可用与生产工程（第 7–9 个月）**

   Kubernetes 多节点部署、监控告警、故障注入、数据库切换、恢复演练、CI/CD 和安全审计。
5. **阶段五：专家级专题研究（第 10–12 个月）**

   选择一个方向深挖：高并发账务引擎、分布式事务、亿级对账计算、金融数据库性能优化或多活容灾，并形成技术论文式报告。

## **十、最值得作为 GitHub 代表作的技术成果**

我建议把最终项目验收浓缩成四个可重复验证的成果：

**成果 A：资金一致性**

百万次并发与重试测试，证明无重复扣款、无超扣、无不平衡分录，并提供数据库不变量检查工具。

**成果 B：高性能交易处理**

在明确的测试环境下实现 5,000 TPS 目标，公开 P99 延迟、火焰图、GC、SQL 执行计划和优化前后对比。

**成果 C：故障恢复**

模拟数据库故障、消息重复、服务崩溃与网络分区，展示完整的故障检测、恢复和账务核对过程。

**成果 D：架构设计与技术报告**

完整 HLD、LLD、ADR、测试方案、容量规划、性能报告和故障复盘，所有关键结论可由代码与实验验证。

**最终建议：** 将 `financial-core-platform` 的核心定位为 **Java 高并发金融交易与复式记账平台**，把“资金正确性”作为第一优先级，“分布式一致性”作为技术深度，“高性能与容灾”作为进阶能力。

这样得到的不只是一个包含十几个微服务的演示项目，而是一个能够用于金融核心研发、分布式系统、数据库内核与性能优化方向技术面试和架构评审的长期工程作品。

尤其值得投入的是 `ledger-service`、`posting-engine`、`consistency`、`benchmark` 和 `chaos-test`。这五部分最能体现从普通 Java CRUD 开发向金融核心系统技术专家发展的能力差异。
