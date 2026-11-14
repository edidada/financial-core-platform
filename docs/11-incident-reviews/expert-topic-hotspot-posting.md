# 专家级专题：热点账户高并发账务引擎

> 阶段五成果。方向选择：`README.txt` 第九节「高并发账务引擎」。
> 代码：`engines/hotspot-accounting-engine/`；验证：`tests/platform-tests/.../HotspotEngineTest.java`。

## 摘要

在复式记账平台中，热点账户（平台归集户、大商户结算户）会在短时间内承接大量交易。
朴素实现下，每笔交易对同一 `account_balance` 行执行 `SELECT ... FOR UPDATE` + `UPDATE`，
行锁串行化使吞吐随并发数增长而急剧退化（README 6.1「热点账户 TPS」）。
本文提出**时间窗内缓冲净额折叠**（buffered netting），在**不放松资金正确性**的前提下减少行锁次数，
并给出不变量证明与属性测试证据。

## 1. 问题定义

设账户 A 期初余额 `B`，时间窗内到达 `n` 笔交易 `t_i`，每笔借方 `d_i`、贷方 `c_i`。
朴素实现每行加锁 `n` 次；热点下锁等待成为主导延迟项，P99 随 `n` 超线性增长。

约束（不可妥协）：

1. `Σ Debit = Σ Credit`（I-1）；
2. 任一时刻 `available_balance >= 0`（I-4），**不允许超扣**；
3. 每笔业务交易最多入账一次（I-3），冲正可追溯（I-6）；
4. 余额与分录必须处于同一本地事务边界（ADR-0003）；
5. 金额用 `BIGINT` 最小货币单位（ADR-0002）。

## 2. 设计

### 2.1 分层

```
[准入层 Admission]  HotspotBuffer.admit(bizId, accountId, direction, amount)
                        纯内存、可并行、按账户累计借方、判定「已缓冲借方 + 本笔 <= 可用余额」
                              |
[折叠层 Netting]     HotspotBuffer.drain() -> List<NetEntry>
                        按 (accountId, direction) 汇总成净额指令，originalCount 记录被折叠笔数
                              |
[落库层 Posting]     JdbcPostingEngine（同一本地事务）
                        校验 ΣD=ΣC -> 按 accountId 排序加锁 -> 写 journal/entries -> 条件更新余额
```

关键点：**缓冲只做纯计算与准入判定，不持有事务、不写数据库**。折叠结果仍交给既有的
`JdbcPostingEngine`，因此 ADR-0003 的单事务边界、I-1 借贷平衡、I-4 条件更新全部保留。

### 2.2 准入判定

令 `bufferedDebit(A)` 为窗口内已接纳的、针对账户 A 的借方累计。对新借方请求 `d`：

```
admit(d)  <=>  bufferedDebit(A) + d <= B - (已落库借方累计)
```

阶段一/二无历史落库借方时右侧即 `B`。判定在 `ReentrantLock` 保护的临界区内完成，
保证并发请求不会同时通过「读后写」式的乐观检查——这是防止超扣的核心。

### 2.3 净额折叠

```
net(A, DEBIT)  = Σ d_i
net(A, CREDIT) = Σ c_i
```

折叠后总借方 = 原始总借方、总贷方 = 原始总贷方，故 `ΣD = ΣC` 自动保持。
落库次数从 `n` 次降为「不同 (账户, 方向) 组合数」次，热点单账户场景下由 `n` 降为 2。

### 2.4 可追溯性

`NetEntry.originalCount` 记录被折叠的原始笔数；生产实现应为每个 `bizId` 保留
独立的业务交易记录与幂等键，折叠仅影响**余额更新与分录落库粒度**，不改变业务语义。
冲正仍通过反向分录实现（ADR-0004），不删除任何已生效分录。

## 3. 正确性论证

**命题 1（不超扣）**：任一时刻账户借方累计不超过期初可用余额。
证明：准入层对每笔借方做 `bufferedDebit + d <= B` 检查且在锁内更新累计；
落库层再用条件 `UPDATE ... WHERE available_balance >= amount` 兜底。两道检查均在
同一余额上限下，故不会超扣。∎

**命题 2（借贷平衡）**：折叠后 `Σ Debit = Σ Credit`。
证明：折叠是按方向的分组求和，保持各方向总量不变；原始交易成对生成（每笔转账一借一贷
且金额相等），故原始 `ΣD=ΣC`，折叠后仍相等。∎

**命题 3（无丢账）**：净额借方总量等于被接纳的原始借方总量。
证明：`drain()` 对 `pending` 中每条记录按 `(accountId, debit)` 累加 `amountMinor`，
不丢弃、不变号；`HotspotProof.verify` 断言 `netDebit == admittedDebit`。∎

**命题 4（事务边界不变）**：折叠结果仍由 `JdbcPostingEngine` 在调用方本地事务内落库，
余额更新、分录、Outbox 事件一次 COMMIT，ADR-0003 成立。∎

## 4. 实验与验证

| 实验 | 方法 | 结果 |
| --- | --- | --- |
| 不超扣 | `HotspotBuffer(100)` 并发 admit 60 + 50 | 第二笔借方被拒，`size()=2` |
| 净额平衡 | 100 笔 5 分转账折叠 | `balanced=true`，净借方 500 分 |
| 随机属性 | 5,000 笔随机序列，`seed=42` | 三项不变量全部成立 |
| 微基准 | `benchmark/jmh` `PostingBenchmark` | 校验开销基线（待实测数值） |
| 系统压测 | `benchmark/scenarios/hotspot-account.md` | 1,000 TPS 热点场景（待实测） |

> 上述前 3 项为已执行的自动化验证；后 2 项为阶段三/五的压测计划，**结论待实测填写**，
> 不得预设 5,000 TPS 或亿级数据目标可达。

## 5. 代价与边界

- 缓冲窗口引入**可见性延迟**：窗口内余额尚未落库，查询 API 需明确说明「已提交余额」口径。
- 进程崩溃会丢失未落库的缓冲指令；因此窗口必须足够短，且`drain()` 落库成功后才能向客户端
  确认交易成功（与 `transaction` 状态机 `SUCCEEDED` 对齐）。
- 长窗口会放大在途交易规模，需配合 `OutboxRelay` 与恢复任务重放。
- 该优化**只针对热点账户**；普通账户继续走逐笔入账，避免无谓复杂度。
- 分片后跨分片交易需重新评估一致性协议，不能套用单库事务边界。

## 6. 与一线实践的关系

本专题借鉴的是**验证方法**（属性测试证明不变量、热点场景压测、火焰图与锁等待分析），
而非照搬任何公司内部交易量指标。结论必须由代码与实验复现（README 第十节 成果 D）。
