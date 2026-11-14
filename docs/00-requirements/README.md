# 00 需求

| 文档 | 内容 |
| --- | --- |
| `../requirements.md` | 阶段一需求、非功能指标、资金不变量 I-1~I-6、DoD |
| `../README.txt` 第一节 | 分阶段工程验收目标（500/5,000/20,000+ TPS） |
| `scope.md`（本目录） | 五阶段范围与验收矩阵 |

## 五阶段范围与验收矩阵

| 阶段 | 主题 | 核心验收 | 实现位置 |
| --- | --- | --- | --- |
| 一 | 金融账务核心 | 无重复扣款/超扣/不平衡分录 | `services/account,ledger,transaction` |
| 二 | 分布式金融交易 | 跨服务一致性、消息可重放、清算/对账可复现 | `infrastructure/transaction-outbox,message-consumer`、`services/payment,clearing,settlement,reconciliation` |
| 三 | 高性能与大数据 | 5,000 TPS、亿级对账、性能报告 | `engines/batch,reconciliation,hotspot`、`benchmark/`、`database/partitioning` |
| 四 | 高可用与生产工程 | 多 AZ、故障注入、CI/CD、安全审计 | `deploy/`、`.github/workflows/`、`chaos-test/`、`tests/` |
| 五 | 专家级专题 | 高并发账务引擎技术论文式报告 | `11-incident-reviews/expert-topic-*.md`（见本目录下一节） |

> 指标是分阶段工程验收目标，不是对一线公司内部生产指标的推断；必须结合硬件、真实业务混合、
> 指定数据规模与稳定负载验证。