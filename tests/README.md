# tests（阶段四 测试体系）

| 目录 | 类型 | 运行方式 | 验收 |
| --- | --- | --- | --- |
| `integration/` | 集成（Testcontainers 双库） | `.github/workflows/test.yml` | Flyway 全量迁移 + API 可用 |
| `contract/` | 契约（OpenAPI / JSON Schema） | `contract-tests.sh` | 请求/响应与契约一致 |
| `concurrency/` | 并发 | `concurrency-tests.sh` | 单账户 1000 并发仅 1 笔成功 |
| `consistency/` | 一致性 | `consistency-tests.sh` | I-1~I-6 全 0 |
| `property-based/` | 属性测试 | JUnit `platform-tests` | 随机序列下不变量恒成立 |
| `security/` | 安全 | `security-tests.sh` | 签名/防重放/越权 |
| `disaster-recovery/` | 容灾 | `chaos-test/` 联动 | RTO/RPO、恢复后账务一致 |
| `end-to-end/` | 端到端 | `e2e.sh` | 开户→转账→冲正→对账 |

`tests/platform-tests/` 是 Maven 模块，纯逻辑单测已纳入 `mvn test`（当前 18 个用例）。
需要真实数据库/K8s 的用例由 CI 工作流与 chaos-test 驱动，本地 `mvn test` 不依赖外部服务。