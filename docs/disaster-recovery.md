# 灾难恢复设计（阶段四）

> 对应 `README.txt` 第五节（AWS 金融行业架构）、第六节 6.2 混沌矩阵、`chaos-test/`。

## 一、恢复目标（分阶段）

| 阶段 | 同城故障恢复 RTO | 灾难恢复 RPO | 已确认账务记录丢失 |
| --- | --- | --- | --- |
| 一 | < 30 min | ≤ 5 min | 0 |
| 二 | < 5 min | ≤ 30 s | 0 |
| 三 | < 60 s | 关键账务零已确认数据丢失 | 0 |

`RPO=0` 需要与**同步复制 + 故障确认协议 + 持久化边界**配套设计，不能只靠异步主从。
不要求所有模块达到同样可用性等级；按业务关键性分级制定恢复目标。

## 二、故障模型与处置

| 故障 | 检测 | 处置 | 恢复校验 |
| --- | --- | --- | --- |
| 数据库主节点故障 | `DatabaseUnavailable` 告警 | 主从切换；禁止双主写入 | `chaos-test/database-failover/`；无已确认交易丢失 |
| Kafka Broker 故障 | Outbox 积压告警 | Broker 恢复后 `OutboxRelay` 重投 | `chaos-test/kafka-failure/`；消费侧去重挡重复 |
| 服务处理中崩溃 | 就绪探针失败 | 未提交事务由 DB 回滚；已提交靠状态机补状态 | `chaos-test/pod-kill/`；无半状态 |
| 网络分区 | 脑裂检测 | NetworkPolicy + 租约锁，隔离副本禁写 | `chaos-test/network-failure/`；`I-3=0` |
| 整个 AZ 不可用 | 多 AZ 健康检查 | 自动切换到备用 AZ | `chaos-test/region-failover/`；RTO/RPO 实测 |
| 错误配置/版本发布 | 灰度探针 + 不变量校验 | 立即回滚到上一稳定版本 | `chaos-test/recovery-validation/` + 发布审计 |

## 三、账务恢复（区别于基础设施恢复）

灾备必须同时验证两者。账务恢复流程：

1. 停止写入，确认权威库切换完成。
2. 重放在途交易：`OutboxRelay` 重投未发布事件；`UNKNOWN/RECOVERING` 交易以**分录事实**为准补状态
   （`transaction-design.md` 第五节），不以本地状态为准。
3. 多方对账：`/v1/platform/reconciliation`（内部流水 vs 渠道流水），差异可定位。
4. 不变量校验：`/v1/invariants` 必须 `healthy=true`；`I-1~I-4` 全 0。
5. 记录实测 RTO/RPO、丢失与差异明细，写入 `11-incident-reviews/`。

## 四、验证体系

- `chaos-test/` 7 类故障注入 + 每类 `verify.sh` 恢复校验。
- `.github/workflows/test.yml` 双库集成测试（TC-9）。
- `tests/disaster-recovery/`：容灾用例清单。
- `deploy/ansible/playbook.yml`：30% 灰度、健康检查、失败即停、回滚、发布审计。

> AWS 与 DTCC 的公开金融韧性资料强调跨区域复制、恢复自动化，以及对在途交易状态进行重放和核对。
