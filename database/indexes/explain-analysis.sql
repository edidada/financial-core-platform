-- 阶段三：执行计划分析脚本（MySQL 8.4 / PostgreSQL 通用语法）
-- 目标：1 亿条账务记录下 P99 < 200 ms；必须记录 EXPLAIN 结果与索引命中情况。

-- Q1 单账户流水分页
EXPLAIN SELECT entry_id, direction, amount_minor, balance_after, created_at
        FROM ledger_entry WHERE account_id = ? AND created_at >= ?
        ORDER BY created_at DESC LIMIT 50;

-- Q2 凭证分录聚合（借贷平衡校验）
EXPLAIN SELECT journal_id,
               SUM(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE 0 END) AS d,
               SUM(CASE WHEN direction = 'CREDIT' THEN amount_minor ELSE 0 END) AS c
        FROM ledger_entry GROUP BY journal_id;

-- Q3 对账：按币种/时间窗汇总
EXPLAIN SELECT j.currency, COUNT(*), SUM(e.amount_minor)
        FROM journal j JOIN ledger_entry e ON e.journal_id = j.journal_id
        WHERE j.posted_at >= ? AND j.posted_at < ? GROUP BY j.currency;