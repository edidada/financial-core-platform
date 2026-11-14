# Financial Core Platform — 设计文档

> **阶段一：金融账务核心（Fund Correctness First）**
> 定位：Java 21 高并发金融交易与复式记账平台。第一优先级是「资金正确性」，第二是「分布式一致性」，高性能与容灾为进阶能力。

本目录存放 **架构评审级别** 的设计文档。阶段一的目标不是拆出十几个微服务，而是先用 **模块化单体 + 单库事务** 建立可证明的资金正确性基线，再逐阶段引入分布式组件（见 `README.txt` 第九节实施路线）。

## 一、文档清单（阶段一）

| 文档 | 主题 | 覆盖内容 |
| --- | --- | --- |
| [requirements.md](requirements.md) | 需求与验收 | 业务域、范围、功能/非功能需求、资金不变量、DoD |
| [architecture.md](architecture.md) | 总体架构 | C4、模块边界、依赖规则、分层、技术栈、部署 |
| [accounting-design.md](accounting-design.md) | 复式记账 | 科目体系、借贷规则、分录模型、入账、冲正、引擎接口 |
| [transaction-design.md](transaction-design.md) | 交易状态机 | 状态迁移、幂等、UNKNOWN/RECOVERING、超时恢复 |
| [database-design.md](database-design.md) | 数据模型 | ER、DDL、索引、约束、事务边界、隔离级别、Flyway |
| [consistency.md](consistency.md) | 一致性 | 不变量清单、并发扣款、原子提交、故障恢复、校验 SQL |
| [test-strategy.md](test-strategy.md) | 测试策略 | 并发/幂等/属性测试、不变量检查、验收矩阵 |
| [adr/](adr/) | 架构决策 | 关键取舍的 ADR 记录 |

## 二、阅读顺序

1. `requirements.md` —— 先明确「证明什么」。
2. `architecture.md` —— 再明确「边界在哪」。
3. `accounting-design.md` → `transaction-design.md` —— 核心领域模型。
4. `database-design.md` → `consistency.md` —— 落地与不变量。
5. `test-strategy.md` —— 如何用实验证明。
6. `adr/` —— 为什么这样取舍。

## 三、阶段路线图

| 阶段 | 主题 | 核心目标 | 文档状态 |
| --- | --- | --- | --- |
| 一 | 金融账务核心 | 账户、交易、复式记账、幂等、冲正、并发扣款；模块化单体 | **本目录** |
| 二 | 分布式金融交易 | Kafka、Outbox、状态机恢复、清算、对账 | 待建 `04-consistency/` 等 |
| 三 | 高性能与大数据 | 分区、批量、JMH/Gatling、5,000 TPS、性能报告 | 待建 `05-performance/` |
| 四 | 高可用与生产工程 | K8s 多 AZ、监控、混沌、恢复演练、CI/CD、安全审计 | 待建 `06~09/` |
| 五 | 专家级专题 | 高并发账务引擎 / 分布式事务 / 亿级对账 / 多活容灾 | 待建 `11-incident-reviews/` |

阶段二及以后按 `README.txt` 第二节的 `docs/00~11` 编号目录扩展；阶段一先采用扁平命名，避免过早引入空目录。

## 四、贯穿全阶段的第一原则

- **权威数据源唯一**：账户余额与会计分录以关系数据库为唯一权威，Redis 只做非权威缓存，绝不参与资金正确性判定。
- **金额不用浮点**：以 `BIGINT` 存储最小货币单位（分），或明确精度的 `DECIMAL`，禁止 `double`。
- **账务不可变**：已生效分录不删除、不直接修改，冲正通过反向分录实现。
- **原子提交**：余额变更、会计分录、幂等记录、Outbox 事件处于同一本地事务边界。
- **正确先于性能**：先单库事务建立正确性基线，再引入分片与多活，每次升级都要能衡量收益与代价。

## 五、文档规范

- 每个关键模块交付 HLD、LLD、ADR、Test Plan、Benchmark Report 五类产物。
- 所有性能结论必须记录硬件、数据量、并发模型、P50/P95/P99/P99.9、GC 与数据库等待事件。
- ADR 采用「背景 / 决策 / 备选方案 / 后果」四段式，编号递增、只追加不修改。
