# Financial Core Platform

工业级金融交易与账务平台：Java 21 高并发交易、复式记账、资金正确性优先。
五个实施阶段的编码与验证产物均已落地（设计说明见 [`README.txt`](README.txt) 与 [`docs/`](docs/)）。

## 快速开始

需求：JDK 21、Maven 3.9+、本机 MySQL 8（默认，含 8.4 LTS）或 PostgreSQL。

```bash
# 全量构建（41 个模块、28 个单测）
mvn clean package

# 建空库（Flyway 只建表不建库）
#   MySQL:      CREATE DATABASE fcp CHARACTER SET utf8mb4;
#   PostgreSQL: CREATE DATABASE fcp;

FCP_DB_USER=... FCP_DB_PASSWORD=... FCP_SERVER_PORT=18080 \
  java -jar app/financial-core-app/target/financial-core-app-*.jar

# 切换 PostgreSQL
FCP_DB_USER=... FCP_DB_PASSWORD=... FCP_SERVER_PORT=18080 \
  java -jar app/financial-core-app/target/financial-core-app-*.jar --spring.profiles.active=postgresql
```

凭据只通过环境变量注入，仓库内不含任何账号口令。详见 [`docs/README.md`](docs/README.md)。

## 五个阶段

| 阶段 | 内容 | 主要位置 |
| --- | --- | --- |
| 一 | 金融账务核心 | `services/account,ledger,transaction`、`engines/accounting,posting` |
| 二 | 分布式金融交易 | `infrastructure/transaction-outbox,message-consumer,distributed-lock`、`services/payment,clearing,settlement,reconciliation,refund,fee,limit,risk,liquidity,reporting`、`gateway/`、`contracts/` |
| 三 | 高性能与大数据 | `engines/batch,reconciliation,hotspot`、`benchmark/`、`database/partitioning` |
| 四 | 高可用与生产工程 | `deploy/`、`.github/workflows/`、`chaos-test/`、`tests/` |
| 五 | 专家级专题 | `docs/11-incident-reviews/expert-topic-hotspot-posting.md` |

## 核心 API

| 路径 | 说明 |
| --- | --- |
| `POST /v1/accounts` | 开户（可指定期初余额） |
| `POST /v1/transactions` | 转账，Header 必带 `Idempotency-Key` |
| `GET /v1/transactions/{id}` / `byKey/{key}` | 交易查询 / 超时恢复 |
| `POST /v1/transactions/{id}/reverse` | 冲正（反向分录） |
| `GET /v1/invariants` | 资金不变量 I-1~I-4 离线校验 |
| `/v1/platform/*` | 阶段二~五能力（详见 `docs/README.md`） |

金额一律为**最小货币单位（分，BIGINT）**，禁止浮点。完整契约见 `contracts/openapi/openapi.yaml`。

## 设计原则

- 账户余额与会计分录以关系数据库为唯一权威数据源，Redis 不参与资金正确性判定。
- 余额变更、会计分录、幂等记录、Outbox 事件处于同一本地事务边界。
- 账务只追加，冲正通过反向分录实现。
- 默认本机 MySQL 8，保留 PostgreSQL；方言差异只落在 DDL 与配置。

## 验证状态

- `mvn clean package`：41 模块 BUILD SUCCESS，28 个单测通过。
- 本机 MySQL 8.4.8：并发扣款 30 选 1、幂等重放、冲正、I-1~I-4 全 0 实测通过。
- 阶段三 5,000 TPS / 亿级数据、阶段四多 AZ 演练为**实验目标与已交付的验证体系**，数值待实测。

## 常用命令

见 [`Makefile`](Makefile)：`make build` / `make test` / `make run` / `make invariants` / `make e2e`。
