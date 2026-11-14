# 一致性与并发资金安全（LLD）

> 状态：设计中 · 对应 README 第四节六大核心模块 · 依赖 ADR-0003 / ADR-0005

## 一、一致性模型

阶段一采用 **单库强一致性（ACID 本地事务）**，不做跨服务分布式事务。

| 维度 | 阶段一方案 |
| --- | --- |
| 资金正确性 | 关系数据库本地事务（MySQL 8 默认 / PostgreSQL）+ 约束 + 行锁 |
| 幂等 | 唯一索引 + 幂等记录 + 结果快照 |
| 事件可靠性 | Outbox 表同事务写入（阶段二投递） |
| 故障恢复 | 以账务分录为权威事实补状态 |
| 缓存 | 非权威，可降级，不参与判定 |
| 隔离级别 | 显式 `READ_COMMITTED`（MySQL 默认 REPEATABLE READ，需覆盖以统一两库并发语义） |

明确不做/不承诺：
- 不依赖 Redis 保证资金正确性。
- 不承诺消息「恰好一次」投递（阶段二只保证消费侧幂等）。
- 不把跨机构支付设计成一次分布式数据库事务（阶段一不接外部渠道）。

## 二、不变量清单与校验 SQL

| 编号 | 不变量 | 强制手段 |
| --- | --- | --- |
| I-1 | 凭证借贷平衡 | 应用校验 + DB 层校验 |
| I-2 | 余额 = 期初余额 + 分录累计 | 离线重算 |
| I-3 | 同一业务交易最多入账一次 | `journal.biz_id` 唯一 |
| I-4 | 余额不为负 | CHECK + 条件更新 |
| I-5 | 已生效分录不可改删 | 只追加 + 权限 |
| I-6 | 冲正净额为 0 且可追溯 | 反向分录关联 |

校验 SQL（离线对账工具，阶段一即可运行）：

```sql
-- I-1 借贷不平衡的凭证
SELECT journal_id
FROM ledger_entry
GROUP BY journal_id
HAVING SUM(CASE WHEN direction='DEBIT' THEN amount_minor ELSE 0 END)
    <> SUM(CASE WHEN direction='CREDIT' THEN amount_minor ELSE 0 END);

-- I-2 余额与「期初余额 + 分录累计」不一致的账户
SELECT b.account_id, b.available_balance, b.opening_balance,
       COALESCE(SUM(CASE WHEN e.direction='CREDIT' THEN e.amount_minor
                         ELSE -e.amount_minor END), 0) AS delta
FROM account_balance b
LEFT JOIN ledger_entry e ON e.account_id = b.account_id
GROUP BY b.account_id, b.available_balance, b.opening_balance
HAVING b.available_balance - b.opening_balance <> COALESCE(SUM(CASE WHEN e.direction='CREDIT'
                    THEN e.amount_minor ELSE -e.amount_minor END), 0);

-- I-3 重复入账嫌疑（同一业务多张凭证）
SELECT biz_id, COUNT(*) FROM journal GROUP BY biz_id HAVING COUNT(*) > 1;

-- I-4 负余额
SELECT * FROM account_balance WHERE available_balance < 0 OR frozen_balance < 0;
```

**验收条件：以上查询在百万级并发/重试测试后全部返回 0 行。**

## 三、并发扣款（核心难题）

典型挑战（README 4.2）：账户只有 100 元，两个请求同时扣 80 元。错误的「读后写」会让两笔都成功。

### 方案对比

| 方案 | 做法 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- | --- |
| 读后写（无锁） | SELECT 后 UPDATE | 简单 | 丢失更新、超扣 | **禁止** |
| 悲观锁 | `SELECT FOR UPDATE` | 直观 | 热点账户锁竞争、吞吐退化 | 转账多账户时使用 |
| 乐观锁 | `version` CAS + 重试 | 无长锁 | 高冲突下重试放大 | 备选 |
| **条件更新（推荐）** | `UPDATE ... WHERE balance >= amount` + 检查影响行数 | 单语句原子、无读后写、数据库保证 | 需处理影响行数为 0 | **阶段一首选** |

```sql
UPDATE account_balance
SET available_balance = available_balance - 8000, version = version + 1
WHERE account_id = 10001 AND available_balance >= 8000;
-- 影响行数 = 1 成功；= 0 余额不足，返回业务失败，绝不超扣
```

要点：**检查与扣减必须是同一条原子语句**，把不变量下沉到数据库，而非依赖应用层先读后判断。

## 四、原子提交边界

```
@Transactional
transfer():
  幂等记录 + 交易状态 + 余额更新 + 分录 + Outbox  -> 一个 COMMIT
```

- 禁止「先提交余额、再写分录」或反之，否则崩溃会留下半状态。
- 禁止 Redis 与 DB 双写（无跨存储事务）。
- 应用崩溃时，未提交事务由 DB 回滚；已提交事务通过状态机恢复补状态（见 `transaction-design.md` 第五节）。

## 五、幂等与重试

| 场景 | 处理 |
| --- | --- |
| 相同幂等键重复提交 | 返回首次结果快照，不执行业务 |
| 同键不同参数 | `409 CONFLICT` |
| 请求超时 | 先按幂等键/交易号查询，**不直接重试业务** |
| Kafka 重复投递（阶段二） | 消费侧以 `biz_id` 去重，不依赖「恰好一次」 |
| 内部重试 | 仅对明确 FAILED 且未入账的交易；UNKNOWN 必须先查 |

幂等三要素：**唯一约束（DB）+ 状态机（应用）+ 结果快照（响应）**，缺一不可。

## 六、故障恢复矩阵

| 故障 | 现象 | 恢复 | 验收 |
| --- | --- | --- | --- |
| 应用处理中崩溃 | 事务未提交 | DB 自动回滚，无残留 | 无半状态 |
| 提交后、响应前崩溃 | 账务已入账、客户端超时 | 按交易号查得已入账 → 迁移 SUCCEEDED | 不重复入账 |
| 请求超时 | 状态 UNKNOWN | 查询/对账任务以分录为准补状态 | 不误判为失败 |
| 数据库主节点故障 | 不可用 | 主备切换（RTO < 30 min） | 已确认交易 0 丢失 |
| 消息重复（阶段二） | 重复消费 | `biz_id` 唯一约束挡住 | 无重复入账 |
| 网络分区 | 双主风险 | 阶段一单主，禁止双写 | 防脑裂 |

阶段四再引入混沌工程系统化验证（README 6.2）。

## 七、正确性基线测试方法

- 并发扣款测试：1000 并发请求打同一账户（余额 100 元，每笔 80 元）→ 只允许 1 笔成功。
- 幂等重试测试：100 万次重复请求 → 同一业务仅生效一次。
- 属性测试（jqwik）：随机生成交易序列，断言 I-1~I-6 恒成立。
- 双库验证：同一套不变量测试分别在 MySQL 8 与 PostgreSQL 上执行（Testcontainers），结果必须一致。
- 详见 `test-strategy.md`。

## 八、阶段二演进

- Outbox 表已在阶段一同事务写入，阶段二接 Kafka 投递，账务逻辑零改动。
- 引入跨服务调用后，用「本地事务 + Outbox + 消费幂等」替代分布式事务，不采用 XA/TCC 强跨库事务。
- 分片后，跨分片转账需重新评估一致性协议（阶段三专题）。
