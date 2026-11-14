# 复式记账与账务引擎详细设计（LLD）

> 状态：设计中 · 本模块是 **整个项目的技术核心**（README 4.1） · 依赖 ADR-0002 / ADR-0003 / ADR-0004

## 一、职责

Ledger Service + Posting Engine 负责把业务交易翻译成 **平衡的会计分录**，并维护账户余额。它是资金正确性的最终落点，也是唯一能写账务表的模块。

## 二、会计科目与视角

阶段一采用 **平台负债账户视角**（README 4.1）：用户存放在平台的资金，对平台而言是负债。

| 科目 | 方向 | 含义 |
| --- | --- | --- |
| 用户资金负债账户（A/B …） | 贷方增加 | 平台欠用户的钱增加 |
| 平台自有/清算科目 | 借方 | 用于充值、手续费等（阶段一简化） |

A 向 B 转账 100 元（¥100.00 = 10000 分）：

| 账户 | 借方 Debit | 贷方 Credit |
| --- | --- | --- |
| A 的资金负债账户 | 10000 | — |
| B 的资金负债账户 | — | 10000 |
| **合计** | **10000** | **10000** |

> 采用平台负债账户视角：借记 A 的负债表示减少（平台少欠 A），贷记 B 的负债表示增加（平台多欠 B）。

## 三、必须满足的约束

基本式：

```
Σ Debit = Σ Credit
```

但仅平衡还不够，还必须同时满足（README 4.1）：

1. **同币种、同账套、同会计规则下平衡**（不同币种不得混在同一凭证）。
2. **同一业务交易不会重复入账**（`journal.biz_id` 唯一）。
3. **账务记录可追溯，不直接删除已生效分录**。
4. **冲正通过反向分录实现**。
5. **并发交易下余额约束仍成立**（见 `consistency.md`）。
6. **原子提交同时涵盖余额更新、分录和 Outbox 事件记录**。

## 四、领域模型

```
Journal（凭证）
 ├─ journal_id / biz_id(唯一) / type / currency / ledger_set
 ├─ posted_at / status
 └─ entries: List<Entry>  (>= 2 条)
      Entry
       ├─ entry_id / journal_id / account_id
       ├─ direction: DEBIT | CREDIT
       ├─ amount_minor: BIGINT  (> 0)
       ├─ balance_before / balance_after  (审计用)
       └─ created_at  (不可变)
```

设计要点：
- 金额一律 `BIGINT` 最小货币单位（分），**禁止 double**（ADR-0002）。
- Entry **只追加**：无 UPDATE/DELETE 接口，仅 INSERT。
- `balance_before/after` 冗余记录便于事后重算与审计，不影响权威余额。

## 五、入账流程（posting）

`PostingEngine.post(JournalCmd)` 在调用方事务内执行，不自行开启事务：

```
1 校验：金额 > 0、币种一致、账套一致、账户存在且状态正常
2 校验平衡：Σdebit == Σcredit，否则抛 UnbalancedJournalException
3 幂等：biz_id 唯一约束冲突 → 返回已存在凭证（不重复入账）
4 锁余额：对涉及的账户按 account_id 排序加行锁（防死锁）
5 生成分录：逐账户计算 balance_before/after
6 写 journal + entries（INSERT only）
7 更新 account_balance（同一事务）
8 写 outbox_event（同一事务）
返回 journal_id
```

**按 account_id 排序加锁** 是避免并发转账死锁的必要措施（A→B 与 B→A 同时发生时，固定加锁顺序）。

## 六、余额与分录的一致性

权威余额表 `account_balance.available_balance` 必须可由分录重算验证：

```
available_balance(account) =
    opening_balance + Σ(credit.amount_minor) - Σ(debit.amount_minor)
```

不变量 I-2 的校验 SQL 见 `consistency.md`。余额表是 **读优化冗余**，分录才是事实来源；两者必须同事务写入。

## 七、冲正（Reversal）

阶段一冲正规则（ADR-0004）：

- **不删除、不修改**原凭证与分录。
- 生成一张 **反向凭证**：原借方变贷方、原贷方变借方，金额相同。
- 反向凭证 `reversal_of = 原 journal_id`，原凭证 `reversed_by = 反向 journal_id`。
- 冲正后双方余额净变化为 0，且两张凭证都完整保留，满足 I-6。
- 冲正本身也是一次交易，走同一状态机与幂等控制，状态 `SUCCEEDED → REVERSED`。
- 禁止对已冲正凭证二次冲正；禁止对 FAILED/未入账交易冲正。

```
原凭证 J1:  A 借 10000 / B 贷 10000
反向凭证 J2: A 贷 10000 / B 借 10000   (reversal_of = J1)
净额: 0，J1 与 J2 均可追溯
```

## 八、Posting Engine 接口（阶段一）

```java
public interface PostingEngine {
    /** 在调用方事务内执行；不自行管理事务 */
    JournalId post(PostJournalCommand cmd);
    /** 生成反向凭证 */
    JournalId reverse(JournalId original, String reason);
    /** 只读：按业务号查询凭证，用于超时恢复 */
    Optional<Journal> findByBizId(String bizId);
}
```

`posting-engine` 初期作为 `ledger-service` 内的 Java 模块（不独立部署），见 `architecture.md` 第三节。

## 九、账务不可变与审计

| 机制 | 说明 |
| --- | --- |
| 只追加 | 分录表无 UPDATE/DELETE 授权 |
| 触发器/规则 | 禁止 DELETE、禁止改 amount/direction |
| 审计日志 | 每次 post/reverse 记录操作者、TraceId、请求哈希 |
| 链式校验 | 可选：每条分录存前条 hash，形成不可篡改链（阶段五专题） |

## 十、验收要点

- 每笔凭证借贷平衡（I-1），并发下余额约束成立（I-4）。
- 同一 `biz_id` 重复提交不产生第二张凭证（I-3）。
- 任何时刻 `余额 = 分录累计`（I-2）。
- 冲正后净额为 0 且可追溯（I-6）。
- 入账吞吐 >= 300 TPS（阶段一 MVP）。
