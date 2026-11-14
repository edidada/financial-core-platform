# 服务契约

| 目录 | 用途 | 消费方 |
| --- | --- | --- |
| `openapi/` | 外部 REST 接口（含幂等键、防重放头） | 渠道接入层、契约测试 |
| `protobuf/` | 内部 gRPC 契约（阶段二服务拆分） | 服务间通信 |
| `avro/` | Kafka 领域事件 Schema | Outbox / Schema Registry |
| `event-schemas/` | JSON Schema（事件与支付指令） | 契约测试、文档生成 |

约定：金额一律 `int64` 最小货币单位；所有事件必须携带 `eventId` 供消费侧去重；
`UNKNOWN` 是合法状态，超时不等于业务失败（见 `docs/transaction-design.md`）。