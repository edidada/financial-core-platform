# 01 架构

- `../architecture.md`：C4 L1/L2、模块边界与依赖规则、技术栈、部署拓扑、多数据库支持。
- `module-map.md`：41 个 Maven 模块与五阶段对应关系。
- `../adr/`：全部架构决策记录（ADR-0001~0009）。

## 模块地图（节选）

| 层 | 模块 | 阶段 |
| --- | --- | --- |
| common | common-domain / exception / utils / api | 一 |
| infrastructure | idempotency / outbox | 一 |
| infrastructure | id-generator / distributed-lock / transaction-outbox / message-consumer / rate-limiter / circuit-breaker / database-sharding / audit-log / observability | 二~四 |
| gateway | authentication / api-gateway | 二、四 |
| engines | accounting-engine / posting-engine | 一 |
| engines | rule / batch / reconciliation / routing | 二、三 |
| engines | hotspot-accounting-engine | 五 |
| services | account / ledger / transaction | 一 |
| services | payment / clearing / settlement / reconciliation / refund / fee / limit / risk / liquidity / reporting | 二 |
| app | financial-core-app（单进程装配） | 一~五 |
| benchmark / tests | jmh / platform-tests | 三、四 |

依赖方向单向向下，用 Maven module 依赖约束（CI 可用 ArchUnit 补强）。